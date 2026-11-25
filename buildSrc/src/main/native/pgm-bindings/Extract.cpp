// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

#include <clang/AST/ASTConsumer.h>
#include <clang/AST/ASTContext.h>
#include <clang/AST/Attr.h>
#include <clang/Basic/Diagnostic.h>
#include <clang/Basic/TargetInfo.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Frontend/FrontendActions.h>
#include <clang/Tooling/Tooling.h>
#include <llvm/ADT/StringSwitch.h>
#include <llvm/Support/JSON.h>
#include <llvm/Support/Path.h>
#include <algorithm>
#include <map>
#include <optional>

namespace {
using namespace clang;
using llvm::json::Array;
using llvm::json::Object;
using llvm::json::Value;

struct Binding {
    std::string location;
    Value shape;
};

class Consumer : public ASTConsumer {
    ASTContext& ast;
    SourceManager& sources;
    std::string includeRoot;
    std::map<std::string, Binding> bindings;
    Object& output;
    const NamedDecl* declaration = nullptr;

    void reject(const std::string& reason) {
        auto& diagnostics = ast.getDiagnostics();
        auto id = diagnostics.getCustomDiagID(DiagnosticsEngine::Error, "%0 [%1]: %2");
        diagnostics.Report(declaration->getLocation(), id)
            << declaration->getNameAsString() << ast.getTargetInfo().getTriple().str() << reason;
    }

    std::optional<Object> unsupported(QualType type, llvm::StringRef reason) {
        reject(type.getAsString() + ": " + reason.str());
        return std::nullopt;
    }

    std::optional<Object> prototype(const FunctionProtoType* fn) {
        if (fn->isVariadic() || fn->getCallConv() != CC_C) {
            reject("variadic or non-default calling convention is unsupported");
            return std::nullopt;
        }
        auto result = describe(fn->getReturnType());
        if (!result) {
            return std::nullopt;
        }
        Array args;
        for (auto parameter : fn->getParamTypes()) {
            auto arg = describe(parameter);
            if (!arg) {
                return std::nullopt;
            }
            args.emplace_back(std::move(*arg));
        }
        return Object{{"result", std::move(*result)}, {"args", std::move(args)}};
    }

    std::optional<Object> describe(QualType type, bool pointee = false) {
        bool isConst = type.isConstQualified();
        if (type.isVolatileQualified() || type.isRestrictQualified()) {
            return unsupported(type, "volatile and restrict qualifiers are unsupported");
        }
        auto shape = [&](llvm::StringRef kind) {
            return Object{{"kind", kind}, {"const", isConst}};
        };
        auto const* raw = type.getTypePtr();
        if (auto const* alias = dyn_cast<TypedefType>(raw)) {
            auto name = alias->getDecl()->getName();
            unsigned bits = llvm::StringSwitch<unsigned>(name)
                .Cases({"int8_t", "uint8_t"}, 8).Cases({"int16_t", "uint16_t"}, 16)
                .Cases({"int32_t", "uint32_t"}, 32).Cases({"int64_t", "uint64_t"}, 64)
                .Case("size_t", 64).Default(0);
            if (bits) {
                bool isUnsigned = name.starts_with("u") || name == "size_t";
                if (!sources.isInSystemHeader(alias->getDecl()->getLocation())
                    || !type->isIntegerType() || ast.getTypeSize(type) != bits
                    || type->isUnsignedIntegerType() != isUnsigned || ast.getTypeAlign(type) != bits) {
                    return unsupported(type, "unexpected size, signedness or alignment of standard type");
                }
                return shape(name);
            }
            auto underlying = alias->desugar();
            return describe(isConst ? underlying.withConst() : underlying, pointee);
        }
        if (auto const* pointer = dyn_cast<PointerType>(raw)) {
            auto target = describe(pointer->getPointeeType(), true);
            if (!target) {
                return std::nullopt;
            }
            if (ast.getTypeSize(type) != 64 || ast.getTypeAlign(type) != 64) {
                return unsupported(type, "expected a 64-bit address with 64-bit alignment");
            }
            auto result = shape("pointer");
            result["pointee"] = std::move(*target);
            return result;
        }
        if (auto const* fn = dyn_cast<FunctionProtoType>(raw); fn && pointee) {
            auto signature = prototype(fn);
            if (!signature) {
                return std::nullopt;
            }
            (*signature)["kind"] = "function";
            return signature;
        }
        if (auto const* record = dyn_cast<RecordType>(raw);
            record && pointee && !record->getDecl()->getDefinition() && record->getDecl()->getIdentifier()) {
            auto result = shape("opaque");
            result["name"] = record->getDecl()->getName();
            return result;
        }
        if (auto const* primitive = dyn_cast<BuiltinType>(raw)) {
            llvm::StringRef kind;
            unsigned bits = 0;
            switch (primitive->getKind()) {
            case BuiltinType::Void: return shape("void");
            case BuiltinType::Char_S:
            case BuiltinType::Char_U: kind = "char"; bits = 8; break;
            case BuiltinType::Int: kind = "int"; bits = 32; break;
            case BuiltinType::Double: kind = "double"; bits = 64; break;
            default: return unsupported(type, "use a supported primitive or a fixed-width typedef");
            }
            if (ast.getTypeSize(type) != bits || ast.getTypeAlign(type) != bits) {
                return unsupported(type, "unexpected primitive width or alignment");
            }
            return shape(kind);
        }
        if (auto const* paren = dyn_cast<ParenType>(raw)) {
            auto inner = paren->getInnerType();
            return describe(isConst ? inner.withConst() : inner, pointee);
        }
        return unsupported(type, "unsupported type in the portable PGM API");
    }

    void add(llvm::StringRef name, Object shape, llvm::StringRef location) {
        auto existing = bindings.find(name.str());
        if (existing != bindings.end()) {
            if (existing->second.shape != Value(std::move(shape))) {
                reject("conflicting redeclaration: " + name.str());
            }
            return;
        }
        bindings.emplace(name.str(), Binding{location.str(), Value(std::move(shape))});
    }

    bool exported(const NamedDecl& decl) {
        if (decl.hasAttr<DLLImportAttr>() || decl.hasAttr<DLLExportAttr>()) {
            return true;
        }
        if (auto const* visibility = decl.getAttr<VisibilityAttr>()) {
            return visibility->getVisibility() == VisibilityAttr::Default;
        }
        if (decl.getFormalLinkage() == Linkage::External) {
            reject("external declaration is missing a public or hidden PGM visibility annotation");
        }
        return false;
    }

    void visit(const NamedDecl& decl, llvm::StringRef location) {
        if (auto const* fn = dyn_cast<FunctionDecl>(&decl); fn && exported(decl)) {
            auto const* proto = fn->getType()->getAs<FunctionProtoType>();
            if (auto signature = proto ? prototype(proto) : unsupported(fn->getType(), "expected a prototype")) {
                (*signature)["kind"] = "function";
                add(decl.getName(), std::move(*signature), location);
            }
        } else if (auto const* variable = dyn_cast<VarDecl>(&decl); variable && exported(decl)) {
            auto type = variable->getType();
            if (!type->isPointerType() || variable->getTLSKind() != VarDecl::TLS_None
                    || variable->getStorageClass() != SC_Extern || variable->hasInit()) {
                reject("expected a non-thread-local extern pointer declaration, got " + type.getAsString());
                return;
            }
            if (auto pointer = describe(type)) {
                add(decl.getName(), Object{{"kind", "global"}, {"type", std::move(*pointer)}}, location);
            }
        } else if (auto const* alias = dyn_cast<TypedefNameDecl>(&decl)) {
            auto type = alias->getUnderlyingType();
            if (auto description = describe(type, type->isRecordType())) {
                add(decl.getName(), Object{{"kind", "typedef"}, {"type", std::move(*description)}}, location);
            }
        } else if (auto const* enumeration = dyn_cast<EnumDecl>(&decl)) {
            for (auto const* constant : enumeration->enumerators()) {
                auto value = constant->getInitVal();
                if (!value.isSignedIntN(32) || (value.isUnsigned() && value.getActiveBits() > 31)) {
                    declaration = constant;
                    reject("enum constant does not fit the Java int carrier");
                } else {
                    add(constant->getName(), Object{{"kind", "constant"}, {"value", value.getExtValue()}}, location);
                }
            }
        } else if (!isa<FunctionDecl, VarDecl, RecordDecl>(&decl)) {
            reject("unsupported public declaration form");
        }
    }

public:
    Consumer(ASTContext& ast, std::string root, Object& output)
        : ast(ast), sources(ast.getSourceManager()), includeRoot(std::move(root)), output(output) {}

    void HandleTranslationUnit(ASTContext& context) override {
        for (auto const* node : context.getTranslationUnitDecl()->decls()) {
            auto const* named = dyn_cast<NamedDecl>(node);
            if (!named || named->isImplicit()) {
                continue;
            }
            auto location = sources.getPresumedLoc(sources.getExpansionLoc(named->getLocation()));
            if (location.isInvalid()) {
                continue;
            }
            llvm::StringRef relative(location.getFilename());
            if (!relative.consume_front(includeRoot) || relative.empty()
                || !llvm::sys::path::is_separator(relative.front())) {
                continue;
            }
            relative = relative.drop_front();
            if (relative != "power_grid_model_c.h" && !relative.starts_with("power_grid_model_c/")
                && !relative.starts_with("power_grid_model_c\\")) {
                continue;
            }
            declaration = named;
            auto path = relative.str();
            std::replace(path.begin(), path.end(), '\\', '/');
            visit(*named, path + ":" + std::to_string(location.getLine()) + ":" + std::to_string(location.getColumn()));
        }
        Array declarations;
        for (auto& [name, binding] : bindings) {
            declarations.emplace_back(Object{{"name", name}, {"location", binding.location},
                {"shape", std::move(binding.shape)}});
        }
        output = Object{{"target", context.getTargetInfo().getTriple().str()},
            {"declarations", std::move(declarations)}};
    }
};

class Action : public ASTFrontendAction {
    std::string root;
    Object& output;

public:
    Action(std::string root, Object& output) : root(std::move(root)), output(output) {}

    std::unique_ptr<ASTConsumer> CreateASTConsumer(CompilerInstance& compiler, llvm::StringRef) override {
        return std::make_unique<Consumer>(compiler.getASTContext(), root, output);
    }
};
}

int main(int argc, char** argv) {
    if (argc < 3) {
        llvm::errs() << "usage: pgm-extract <absolute-header-directory> <absolute-clang-path> [clang flags...]\n";
        return 2;
    }
    std::string root = argv[1];
    std::vector<std::string> flags{"-x", "c", "-std=c23", "-Werror", "-I", root};
    flags.insert(flags.end(), argv + 3, argv + argc);
    Object result;
    bool ok = clang::tooling::runToolOnCodeWithArgs(std::make_unique<Action>(root, result),
        "#include <power_grid_model_c.h>\n#include <power_grid_model_c/dataset_definitions.h>\n",
        flags, "pgm-bindings.c", argv[2]);
    if (!ok || result.empty() || result.getArray("declarations")->empty()) {
        llvm::errs() << "PGM header extraction failed or produced no declarations\n";
        return 1;
    }
    llvm::outs() << llvm::formatv("{0}\n", Value(std::move(result)));
}
