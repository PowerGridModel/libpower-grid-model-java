// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

#include <clang-c/Index.h>
#include <llvm/ADT/StringSwitch.h>
#include <llvm/Support/FormatVariadic.h>
#include <llvm/Support/JSON.h>
#include <llvm/Support/raw_ostream.h>

#include <algorithm>
#include <cstdint>
#include <limits>
#include <map>
#include <memory>
#include <optional>
#include <string>
#include <vector>

namespace {
using llvm::json::Array;
using llvm::json::Object;
using llvm::json::Value;

std::string text(CXString value) {
    auto const* data = clang_getCString(value);
    std::string result = data ? data : "";
    clang_disposeString(value);
    return result;
}

std::string normalized(std::string path) {
    std::replace(path.begin(), path.end(), '\\', '/');
    return path;
}

bool unsignedInteger(CXTypeKind kind) {
    switch (kind) {
    case CXType_Char_U: case CXType_UChar: case CXType_UShort: case CXType_UInt:
    case CXType_ULong: case CXType_ULongLong: case CXType_UInt128: return true;
    default: return false;
    }
}

bool signedInteger(CXTypeKind kind) {
    switch (kind) {
    case CXType_Char_S: case CXType_SChar: case CXType_Short: case CXType_Int:
    case CXType_Long: case CXType_LongLong: case CXType_Int128: return true;
    default: return false;
    }
}

struct Binding {
    std::string location;
    Value shape;
};

class Consumer {
    std::string root;
    std::string target;
    std::map<std::string, Binding> bindings;
    CXCursor declaration{};
    std::string location;
    bool failed = false;

    void reject(const std::string& reason) {
        failed = true;
        llvm::errs() << location << ": error: " << text(clang_getCursorSpelling(declaration))
                     << " [" << target << "]: " << reason << '\n';
    }

    std::optional<Object> unsupported(CXType type, const std::string& reason) {
        reject(text(clang_getTypeSpelling(type)) + ": " + reason);
        return std::nullopt;
    }

    std::optional<Object> prototype(CXType type) {
        if (type.kind != CXType_FunctionProto) {
            return unsupported(type, "expected a prototype");
        }
        if (clang_isFunctionTypeVariadic(type) || clang_getFunctionTypeCallingConv(type) != CXCallingConv_C) {
            reject("variadic or non-default calling convention is unsupported");
            return std::nullopt;
        }
        auto result = describe(clang_getResultType(type));
        if (!result) {
            return std::nullopt;
        }
        Array args;
        for (int i = 0, count = clang_getNumArgTypes(type); i < count; ++i) {
            auto arg = describe(clang_getArgType(type, i));
            if (!arg) {
                return std::nullopt;
            }
            args.emplace_back(std::move(*arg));
        }
        return Object{{"result", std::move(*result)}, {"args", std::move(args)}};
    }

    std::optional<Object> describe(CXType type, bool pointee = false, bool inheritedConst = false) {
        bool isConst = inheritedConst || clang_isConstQualifiedType(type);
        if (clang_isVolatileQualifiedType(type) || clang_isRestrictQualifiedType(type)) {
            return unsupported(type, "volatile and restrict qualifiers are unsupported");
        }
        auto shape = [&](std::string kind) {
            return Object{{"kind", std::move(kind)}, {"const", isConst}};
        };
        auto hasLayout = [&](long long bytes) {
            return clang_Type_getSizeOf(type) == bytes && clang_Type_getAlignOf(type) == bytes;
        };
        switch (type.kind) {
        case CXType_Elaborated:
            return describe(clang_Type_getNamedType(type), pointee, isConst);
        case CXType_Typedef: {
            auto alias = clang_getTypeDeclaration(type);
            auto name = text(clang_getCursorSpelling(alias));
            auto bytes = llvm::StringSwitch<int>(name)
                .Case("int8_t", 1).Case("uint8_t", 1)
                .Case("int16_t", 2).Case("uint16_t", 2)
                .Case("int32_t", 4).Case("uint32_t", 4)
                .Case("int64_t", 8).Case("uint64_t", 8)
                .Case("size_t", 8).Default(0);
            if (bytes) {
                auto kind = clang_getCanonicalType(type).kind;
                bool isUnsigned = name.front() == 'u' || name == "size_t";
                if (!clang_Location_isInSystemHeader(clang_getCursorLocation(alias))
                    || !(isUnsigned ? unsignedInteger(kind) : signedInteger(kind)) || !hasLayout(bytes)) {
                    return unsupported(type, "unexpected size, signedness or alignment of standard type");
                }
                return shape(std::move(name));
            }
            return describe(clang_getTypedefDeclUnderlyingType(alias), pointee, isConst);
        }
        case CXType_Pointer: {
            auto pointeeType = describe(clang_getPointeeType(type), true);
            if (!pointeeType) {
                return std::nullopt;
            }
            if (!hasLayout(8)) {
                return unsupported(type, "expected a 64-bit address with 64-bit alignment");
            }
            auto result = shape("pointer");
            result["pointee"] = std::move(*pointeeType);
            return result;
        }
        case CXType_FunctionProto:
            if (pointee) {
                auto signature = prototype(type);
                if (signature) {
                    (*signature)["kind"] = "function";
                }
                return signature;
            }
            break;
        case CXType_Record: {
            auto record = clang_getTypeDeclaration(type);
            auto name = text(clang_getCursorSpelling(record));
            if (pointee && clang_Cursor_isNull(clang_getCursorDefinition(record))
                && !clang_Cursor_isAnonymous(record) && !name.empty()) {
                auto result = shape("opaque");
                result["name"] = std::move(name);
                return result;
            }
            break;
        }
        case CXType_Void: return shape("void");
        case CXType_Char_S: case CXType_Char_U:
            if (hasLayout(1)) return shape("char");
            return unsupported(type, "unexpected primitive width or alignment");
        case CXType_Int:
            if (hasLayout(4)) return shape("int");
            return unsupported(type, "unexpected primitive width or alignment");
        case CXType_Double:
            if (hasLayout(8)) return shape("double");
            return unsupported(type, "unexpected primitive width or alignment");
        default: break;
        }
        return unsupported(type, "unsupported type in the portable PGM API");
    }

    void add(CXCursor cursor, Object shape) {
        auto name = text(clang_getCursorSpelling(cursor));
        auto existing = bindings.find(name);
        if (existing != bindings.end()) {
            if (existing->second.shape != Value(std::move(shape))) {
                reject("conflicting redeclaration: " + name);
            }
            return;
        }
        bindings.emplace(std::move(name), Binding{location, Value(std::move(shape))});
    }

    bool exported(CXCursor cursor) {
        struct Attributes {
            bool dll = false;
            std::optional<std::string> visibility;
        } attrs;
        clang_visitChildren(cursor, [](CXCursor child, CXCursor, CXClientData data) {
            auto& attrs = *static_cast<Attributes*>(data);
            switch (clang_getCursorKind(child)) {
            case CXCursor_DLLImport: case CXCursor_DLLExport:
                attrs.dll = true;
                break;
            case CXCursor_VisibilityAttr:
                attrs.visibility = text(clang_getCursorSpelling(child));
                break;
            default: break;
            }
            return CXChildVisit_Continue;
        }, &attrs);
        if (attrs.dll) {
            return true;
        }
        if (attrs.visibility) {
            if (*attrs.visibility == "default") return true;
            if (*attrs.visibility == "hidden" || *attrs.visibility == "protected") return false;
            reject("unrecognized explicit PGM visibility annotation");
        } else if (clang_getCursorLinkage(cursor) == CXLinkage_External) {
            reject("external declaration is missing a public or hidden PGM visibility annotation");
        }
        return false;
    }

    void visit(CXCursor cursor) {
        CXFile file = nullptr;
        unsigned line = 0, column = 0;
        clang_getExpansionLocation(clang_getCursorLocation(cursor), &file, &line, &column, nullptr);
        if (!file) return;
        auto path = normalized(text(clang_getFileName(file)));
        llvm::StringRef relative(path);
        if (!relative.consume_front(root)
            || (relative != "power_grid_model_c.h" && !relative.starts_with("power_grid_model_c/"))) {
            return;
        }
        declaration = cursor;
        location = relative.str() + ":" + std::to_string(line) + ":" + std::to_string(column);
        auto type = clang_getCursorType(cursor);
        switch (clang_getCursorKind(cursor)) {
        case CXCursor_FunctionDecl:
            if (exported(cursor)) {
                if (auto signature = prototype(type)) {
                    (*signature)["kind"] = "function";
                    add(cursor, std::move(*signature));
                }
            }
            break;
        case CXCursor_VarDecl:
            if (exported(cursor)) {
                if (clang_getCanonicalType(type).kind != CXType_Pointer || clang_getCursorTLSKind(cursor) != CXTLS_None
                    || clang_Cursor_getStorageClass(cursor) != CX_SC_Extern
                    || !clang_Cursor_isNull(clang_Cursor_getVarDeclInitializer(cursor))) {
                    reject("expected a non-thread-local extern pointer declaration, got " + text(clang_getTypeSpelling(type)));
                } else if (auto pointer = describe(type)) {
                    add(cursor, Object{{"kind", "global"}, {"type", std::move(*pointer)}});
                }
            }
            break;
        case CXCursor_TypedefDecl: case CXCursor_TypeAliasDecl: {
            auto underlying = clang_getTypedefDeclUnderlyingType(cursor);
            if (auto description = describe(underlying, clang_getCanonicalType(underlying).kind == CXType_Record)) {
                add(cursor, Object{{"kind", "typedef"}, {"type", std::move(*description)}});
            }
            break;
        }
        case CXCursor_EnumDecl:
            clang_visitChildren(cursor, [](CXCursor child, CXCursor parent, CXClientData data) {
                if (clang_getCursorKind(child) == CXCursor_EnumConstantDecl) {
                    auto& self = *static_cast<Consumer*>(data);
                    auto value = clang_getEnumConstantDeclValue(child);
                    auto kind = clang_getCanonicalType(clang_getEnumDeclIntegerType(parent)).kind;
                    if (value < std::numeric_limits<std::int32_t>::min() || value > std::numeric_limits<std::int32_t>::max()
                        || (unsignedInteger(kind) && clang_getEnumConstantDeclUnsignedValue(child) > std::numeric_limits<std::int32_t>::max())) {
                        self.declaration = child;
                        self.reject("enum constant does not fit the Java int carrier");
                    } else {
                        self.add(child, Object{{"kind", "constant"}, {"value", value}});
                    }
                }
                return CXChildVisit_Continue;
            }, this);
            break;
        case CXCursor_StructDecl: case CXCursor_UnionDecl: break;
        default: reject("unsupported public declaration form");
        }
    }

public:
    Consumer(std::string root, std::string target)
        : root(normalized(std::move(root))), target(std::move(target)) {
        if (this->root.back() != '/') this->root += '/';
    }

    std::optional<Object> extract(CXTranslationUnit tu) {
        clang_visitChildren(clang_getTranslationUnitCursor(tu), [](CXCursor cursor, CXCursor, CXClientData data) {
            static_cast<Consumer*>(data)->visit(cursor);
            return CXChildVisit_Continue;
        }, this);
        if (failed || bindings.empty()) return std::nullopt;
        Array declarations;
        for (auto& [name, binding] : bindings) {
            declarations.emplace_back(Object{{"name", name}, {"location", binding.location},
                {"shape", std::move(binding.shape)}});
        }
        return Object{{"target", target}, {"declarations", std::move(declarations)}};
    }
};
}

int main(int argc, char** argv) {
    if (argc < 3 || !*argv[1]) {
        llvm::errs() << "usage: pgm-extract <absolute-header-directory> <absolute-clang-path> [clang flags...]\n";
        return 2;
    }
    constexpr char source[] = "#include <power_grid_model_c.h>\n#include <power_grid_model_c/dataset_definitions.h>\n";
    CXUnsavedFile input{"pgm-bindings.c", source, sizeof(source) - 1};
    // libclang takes a full argv so Clang can locate its resource headers relative to the installed driver.
    std::vector<char const*> flags{argv[2], "-x", "c", "-std=c2x", "-Werror", "-I", argv[1]};
    flags.insert(flags.end(), argv + 3, argv + argc);
    std::unique_ptr<void, decltype(&clang_disposeIndex)> index(clang_createIndex(0, 0), clang_disposeIndex);
    CXTranslationUnit parsed = nullptr;
    auto status = clang_parseTranslationUnit2FullArgv(index.get(), input.Filename, flags.data(),
        static_cast<int>(flags.size()), &input, 1, 0, &parsed);
    if (status != CXError_Success) {
        llvm::errs() << "PGM header parse failed (libclang error " << status << ")\n";
        return 1;
    }
    std::unique_ptr<CXTranslationUnitImpl, decltype(&clang_disposeTranslationUnit)> tu(parsed, clang_disposeTranslationUnit);
    bool parseFailed = false;
    for (unsigned i = 0, count = clang_getNumDiagnostics(tu.get()); i < count; ++i) {
        auto diagnostic = clang_getDiagnostic(tu.get(), i);
        llvm::errs() << text(clang_formatDiagnostic(diagnostic, clang_defaultDiagnosticDisplayOptions())) << '\n';
        parseFailed |= clang_getDiagnosticSeverity(diagnostic) >= CXDiagnostic_Error;
        clang_disposeDiagnostic(diagnostic);
    }
    if (parseFailed) return 1;
    auto targetInfo = clang_getTranslationUnitTargetInfo(tu.get());
    auto target = text(clang_TargetInfo_getTriple(targetInfo));
    clang_TargetInfo_dispose(targetInfo);
    auto result = Consumer(argv[1], std::move(target)).extract(tu.get());
    if (!result) {
        llvm::errs() << "PGM header extraction failed or produced no declarations\n";
        return 1;
    }
    llvm::outs() << llvm::formatv("{0}\n", Value(std::move(*result)));
}
