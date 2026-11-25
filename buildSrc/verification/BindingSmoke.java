// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Path;
import java.util.Arrays;
import org.lfenergy.pgm.PGM_LogOutputCallback;
import org.lfenergy.pgm.PowerGridModelC;
import org.lfenergy.pgm.loader.PGMLoader;

import static org.lfenergy.pgm.PowerGridModelC.*;

class BindingSmoke {
    static void main(String[] args) throws Exception {
        var loader = new PGMLoader();
        if (args.length == 0) {
            loader.autoload();
        } else if (args.length == 1) {
            System.load(Path.of(args[0]).toAbsolutePath().toString());
        } else {
            throw new IllegalArgumentException("Expected zero arguments or the path to one native PGM library");
        }
        loader.check(true);
        for (var method : PowerGridModelC.class.getMethods()) {
            if (method.getName().endsWith("$address") || method.getName().endsWith("$segment")) {
                check(((MemorySegment) method.invoke(null)).address() != 0, method.getName());
            }
        }
        try (var arena = Arena.ofConfined()) {
            verifyBindings(arena);
        }
        System.out.printf("PGM %s verified on %s/%s (Java %s)%n", loader.getPGMRuntimeVersion(),
            System.getProperty("os.name"), System.getProperty("os.arch"), Runtime.version());
    }

    private static void verifyBindings(Arena arena) {
        var handle = PGM_create_handle();
        var nodes = MemorySegment.NULL;
        var source = MemorySegment.NULL;
        var output = MemorySegment.NULL;
        var model = MemorySegment.NULL;
        var inputDataset = MemorySegment.NULL;
        var outputDataset = MemorySegment.NULL;
        var options = MemorySegment.NULL;
        var logger = MemorySegment.NULL;
        try {
            var component = PGM_def_input_node();
            check(component.address() != 0 && component.address() != PGM_def_input_node$segment().address(),
                "metadata getter must dereference the exported slot");
            check(PGM_def_input_node$segment().isReadOnly(), "const metadata slot must be read-only");
            check("node".equals(PGM_meta_component_name(handle, component).getString(0)), "metadata component name");
            var size = PGM_meta_component_size(handle, component);
            check(PGM_meta_component_size$descriptor().returnLayout().orElseThrow()
                .equals(Linker.nativeLinker().canonicalLayouts().get("size_t")), "native size_t layout");
            check(size >= PGM_meta_attribute_offset(handle, PGM_def_input_node_u_rated()) + C_DOUBLE.byteSize(),
                "node component size");
            nodes = PGM_create_buffer(handle, component, 2);
            source = PGM_create_buffer(handle, PGM_def_input_source(), 1);
            var ids = arena.allocateFrom(PGM_ID, 37, 15);
            PGM_buffer_set_value(handle, PGM_def_input_node_id(), nodes, ids, 0, 2, PGM_ID.byteSize());
            PGM_buffer_set_value(handle, PGM_def_input_node_u_rated(), nodes,
                arena.allocateFrom(C_DOUBLE, 10_000.0), 0, 2, 0);
            PGM_buffer_set_value(handle, PGM_def_input_source_id(), source, arena.allocateFrom(PGM_ID, 4), 0, 1, 0);
            PGM_buffer_set_value(handle, PGM_def_input_source_node(), source, ids, 0, 1, 0);
            PGM_buffer_set_value(handle, PGM_def_input_source_status(), source, arena.allocateFrom(C_CHAR, (byte) 1), 0, 1, 0);
            PGM_buffer_set_value(handle, PGM_def_input_source_u_ref(), source, arena.allocateFrom(C_DOUBLE, 1.0), 0, 1, 0);
            PGM_buffer_set_value(handle, PGM_def_input_source_sk(), source, arena.allocateFrom(C_DOUBLE, 1e6), 0, 1, 0);
            checkPgm(handle);
            var node = arena.allocateFrom("node");
            inputDataset = PGM_create_dataset_const(handle, arena.allocateFrom("input"), 0, 1);
            PGM_dataset_const_add_buffer(handle, inputDataset, node, 2, 2, MemorySegment.NULL, nodes);
            PGM_dataset_const_add_buffer(handle, inputDataset, arena.allocateFrom("source"), 1, 1, MemorySegment.NULL, source);
            model = PGM_create_model(handle, 50.0, inputDataset);
            checkPgm(handle);
            var requested = arena.allocateFrom(PGM_ID, 15, 37, 15);
            var indexes = arena.allocateFrom(PGM_Idx, -1L, -1L, -1L);
            PGM_get_indexer(handle, model, node, 3, requested, indexes);
            checkPgm(handle);
            check(Arrays.equals(indexes.toArray(PGM_Idx), new long[] {1, 0, 1}), "complete 64-bit indexer writes");

            logger = PGM_create_logger(handle, PGM_benchmark_logger());
            PGM_register_logger(handle, logger);
            output = PGM_create_buffer(handle, PGM_def_sym_output_node(), 2);
            outputDataset = PGM_create_dataset_mutable(handle, arena.allocateFrom("sym_output"), 0, 1);
            PGM_dataset_mutable_add_buffer(handle, outputDataset, node, 2, 2, MemorySegment.NULL, output);
            options = PGM_create_options(handle);
            PGM_calculate(handle, model, options, outputDataset, MemorySegment.NULL);
            checkPgm(handle);
            verifyCallback(arena, handle, logger);
        } finally {
            if (logger.address() != 0) {
                PGM_unregister_logger(handle, logger);
                PGM_destroy_logger(logger);
            }
            if (options.address() != 0) PGM_destroy_options(options);
            if (model.address() != 0) PGM_destroy_model(model);
            if (outputDataset.address() != 0) PGM_destroy_dataset_mutable(outputDataset);
            if (inputDataset.address() != 0) PGM_destroy_dataset_const(inputDataset);
            if (output.address() != 0) PGM_destroy_buffer(output);
            if (source.address() != 0) PGM_destroy_buffer(source);
            if (nodes.address() != 0) PGM_destroy_buffer(nodes);
            PGM_destroy_handle(handle);
        }
    }

    private static void verifyCallback(Arena arena, MemorySegment handle, MemorySegment logger) {
        var userData = arena.allocate(C_INT);
        var result = new CallbackResult();
        var stub = PGM_LogOutputCallback.allocate((data, length, context) -> {
            result.calls++;
            result.length = length;
            result.context = context.address();
            if (data.address() != 0 && length > 0 && length <= 1_000_000) {
                result.bytes = data.asSlice(0, length).toArray(ValueLayout.JAVA_BYTE);
            }
        }, arena);
        PGM_logger_get_output(handle, logger, stub, userData);
        checkPgm(handle);
        check(result.calls == 1 && result.context == userData.address(), "logger callback count and context");
        check(result.bytes != null && result.length == result.bytes.length, "logger callback byte length");
        check(new String(result.bytes, java.nio.charset.StandardCharsets.UTF_8).contains("\t"), "benchmark log content");
    }

    private static class CallbackResult {
        int calls;
        long length;
        long context;
        byte[] bytes;
    }

    private static void checkPgm(MemorySegment handle) {
        if (PGM_error_code(handle) != PGM_no_error()) {
            throw new AssertionError(PGM_error_message(handle).getString(0));
        }
    }

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
