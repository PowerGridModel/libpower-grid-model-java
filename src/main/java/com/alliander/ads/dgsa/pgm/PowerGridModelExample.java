// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

package com.alliander.ads.dgsa.pgm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.lfenergy.pgm.PGMLoader;
import org.lfenergy.pgm.PowerGridModelC;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Java equivalent of power_grid_model_c_example/main.c
 * <p>
 * Network topology: source_0 --node_1---- sym_load_2 | |---- sym_load_3
 * <p>
 * NOTE: The generated PowerGridModelC_1 class hardcodes the .so path at generation time. If the library has moved, re-run jextract or update SYMBOL_LOOKUP manually.
 * <p>
 * Run with: java --enable-native-access=ALL-UNNAMED -ea -cp <classpath> org.lfenergy.pgm.example.PowerGridModelExample
 */
public class PowerGridModelExample {

    public static void main(String[] args) {
        PGMLoader.autoload();

        System.out.println("\nThis is a Java FFM example calling the Power Grid Model C API.");
        runExample();
    }

    // -------------------------------------------------------------------------

    private static void runExample() {
        MemorySegment handle = PowerGridModelC.PGM_create_handle();

        MemorySegment nodeInput = MemorySegment.NULL;
        MemorySegment symLoadInput = MemorySegment.NULL;
        MemorySegment inputDataset = MemorySegment.NULL;
        MemorySegment model = MemorySegment.NULL;

        MemorySegment nodeOutput = MemorySegment.NULL;
        MemorySegment singleOutputDataset = MemorySegment.NULL;
        MemorySegment batchOutputDataset = MemorySegment.NULL;

        MemorySegment opt = MemorySegment.NULL;

        MemorySegment sourceUpdate = MemorySegment.NULL;
        MemorySegment loadUpdate = MemorySegment.NULL;
        MemorySegment batchUpdateDataset = MemorySegment.NULL;

        try (Arena arena = Arena.ofConfined()) {
            // ---- create input buffers ----
            nodeInput = PowerGridModelC.PGM_create_buffer(handle, PowerGridModelC.PGM_def_input_node(), 1);
            ensureNoError(handle);

            symLoadInput = PowerGridModelC.PGM_create_buffer(handle, PowerGridModelC.PGM_def_input_sym_load(), 2);
            ensureNoError(handle);

            long sourceSize = PowerGridModelC.PGM_meta_component_size(handle, PowerGridModelC.PGM_def_input_source());
            long sourceAlignment = PowerGridModelC.PGM_meta_component_alignment(handle, PowerGridModelC.PGM_def_input_source());
            MemorySegment sourceInput = arena.allocate(sourceSize, sourceAlignment);

            // ---- assign input attributes ----
            long nodeIdOffset = PowerGridModelC.PGM_meta_attribute_offset(handle, PowerGridModelC.PGM_def_input_node_id());
            long nodeURatedOffset = PowerGridModelC.PGM_meta_attribute_offset(handle, PowerGridModelC.PGM_def_input_node_u_rated());
            nodeInput.set(PowerGridModelC.PGM_ID, nodeIdOffset, 1);
            nodeInput.set(PowerGridModelC.C_DOUBLE, nodeURatedOffset, 10e3);

            PowerGridModelC.PGM_buffer_set_nan(handle, PowerGridModelC.PGM_def_input_source(), sourceInput, 0, 1);

            MemorySegment sourceId = arena.allocate(PowerGridModelC.PGM_ID);
            sourceId.set(PowerGridModelC.PGM_ID, 0, 0);

            MemorySegment node = arena.allocate(PowerGridModelC.PGM_ID);
            node.set(PowerGridModelC.PGM_ID, 0, 1);

            MemorySegment status = arena.allocate(PowerGridModelC.C_CHAR);
            status.set(PowerGridModelC.C_CHAR, 0, (byte) 1);

            MemorySegment uRef = arena.allocate(PowerGridModelC.C_DOUBLE);
            uRef.set(PowerGridModelC.C_DOUBLE, 0, 1.0);

            MemorySegment sk = arena.allocate(PowerGridModelC.C_DOUBLE);
            sk.set(PowerGridModelC.C_DOUBLE, 0, 1e6);

            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_source_id(), sourceInput, sourceId, 0, 1, -1);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_source_node(), sourceInput, node, 0, 1, -1);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_source_status(), sourceInput, status, 0, 1, -1);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_source_u_ref(), sourceInput, uRef, 0, 1, -1);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_source_sk(), sourceInput, sk, 0, 1, -1);
            ensureNoError(handle);

            int[] symLoadId = {2, 3};
            MemorySegment symLoadIdSeg = toNativeIntArray(arena, symLoadId);

            MemorySegment loadType = arena.allocate(PowerGridModelC.C_CHAR);
            loadType.set(PowerGridModelC.C_CHAR, 0, (byte) 0);

            double[] pqSpecified = {50e3, 10e3, 100e3, 20e3};
            MemorySegment pqSpecifiedSeg = toNativeDoubleArray(arena, pqSpecified);

            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_sym_load_id(), symLoadInput, symLoadIdSeg, 0, 2, -1);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_sym_load_node(), symLoadInput, node, 0, 2, 0);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_sym_load_status(), symLoadInput, status, 0, 2, 0);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_sym_load_type(), symLoadInput, loadType, 0, 2, 0);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_sym_load_p_specified(), symLoadInput, pqSpecifiedSeg, 0, 2, 16);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_input_sym_load_q_specified(), symLoadInput, pqSpecifiedSeg.asSlice(Double.BYTES), 0, 2, 16);
            ensureNoError(handle);

            // ---- initialize model ----
            inputDataset = PowerGridModelC.PGM_create_dataset_const(handle, arena.allocateFrom("input"), 0, 1);
            PowerGridModelC.PGM_dataset_const_add_buffer(handle, inputDataset, arena.allocateFrom("node"), 1, 1, MemorySegment.NULL, nodeInput);
            PowerGridModelC.PGM_dataset_const_add_buffer(handle, inputDataset, arena.allocateFrom("source"), 1, 1, MemorySegment.NULL, sourceInput);
            PowerGridModelC.PGM_dataset_const_add_buffer(handle, inputDataset, arena.allocateFrom("sym_load"), 2, 2, MemorySegment.NULL, symLoadInput);
            ensureNoError(handle);

            model = PowerGridModelC.PGM_create_model(handle, 50.0, inputDataset);
            ensureNoError(handle);

            // ---- create output buffers ----
            nodeOutput = PowerGridModelC.PGM_create_buffer(handle, PowerGridModelC.PGM_def_sym_output_node(), 3);
            ensureNoError(handle);

            MemorySegment uPuSeg = arena.allocate(3L * PowerGridModelC.C_DOUBLE.byteSize(), PowerGridModelC.C_DOUBLE.byteAlignment());
            MemorySegment uAngleSeg = arena.allocate(3L * PowerGridModelC.C_DOUBLE.byteSize(), PowerGridModelC.C_DOUBLE.byteAlignment());

            singleOutputDataset = PowerGridModelC.PGM_create_dataset_mutable(handle, arena.allocateFrom("sym_output"), 0, 1);
            PowerGridModelC.PGM_dataset_mutable_add_buffer(handle, singleOutputDataset, arena.allocateFrom("node"), 1, 1, MemorySegment.NULL, nodeOutput);
            ensureNoError(handle);

            batchOutputDataset = PowerGridModelC.PGM_create_dataset_mutable(handle, arena.allocateFrom("sym_output"), 1, 3);
            PowerGridModelC.PGM_dataset_mutable_add_buffer(handle, batchOutputDataset, arena.allocateFrom("node"), 1, 3, MemorySegment.NULL, nodeOutput);
            ensureNoError(handle);

            // ---- one-time calculation ----
            opt = PowerGridModelC.PGM_create_options(handle);
            PowerGridModelC.PGM_calculate(handle, model, opt, singleOutputDataset, MemorySegment.NULL);
            ensureNoError(handle);

            PowerGridModelC.PGM_buffer_get_value(handle, PowerGridModelC.PGM_def_sym_output_node_u_pu(), nodeOutput, uPuSeg, 0, 1, -1);
            PowerGridModelC.PGM_buffer_get_value(handle, PowerGridModelC.PGM_def_sym_output_node_u_angle(), nodeOutput, uAngleSeg, 0, 1, -1);
            System.out.println("\nOne-time Calculation");
            System.out.printf("Node result u_pu: %f, u_angle: %f%n", uPuSeg.getAtIndex(PowerGridModelC.C_DOUBLE, 0), uAngleSeg.getAtIndex(PowerGridModelC.C_DOUBLE, 0));

            // ---- one-time calculation error ----
            PowerGridModelC.PGM_set_max_iter(handle, opt, 1);
            PowerGridModelC.PGM_calculate(handle, model, opt, singleOutputDataset, MemorySegment.NULL);
            if (PowerGridModelC.PGM_error_code(handle) == PowerGridModelC.PGM_no_error()) {
                throw new IllegalStateException("Expected one-time calculation to fail with max_iter=1");
            }
            System.out.println("\nOne-time Calculation Error");
            System.out.printf("Error code: %d, error message: %s%n", PowerGridModelC.PGM_error_code(handle), cString(PowerGridModelC.PGM_error_message(handle)));
            PowerGridModelC.PGM_set_max_iter(handle, opt, 20);
            PowerGridModelC.PGM_clear_error(handle);

            // ---- prepare batch update dataset ----
            sourceUpdate = PowerGridModelC.PGM_create_buffer(handle, PowerGridModelC.PGM_def_update_source(), 3);
            ensureNoError(handle);
            PowerGridModelC.PGM_buffer_set_nan(handle, PowerGridModelC.PGM_def_update_source(), sourceUpdate, 0, 3);

            double[] uRefUpdate = {0.95, 1.05, 1.1};
            MemorySegment uRefUpdateSeg = toNativeDoubleArray(arena, uRefUpdate);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_update_source_id(), sourceUpdate, sourceId, 0, 3, 0);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_update_source_u_ref(), sourceUpdate, uRefUpdateSeg, 0, 3, -1);

            loadUpdate = PowerGridModelC.PGM_create_buffer(handle, PowerGridModelC.PGM_def_update_sym_load(), 4);
            PowerGridModelC.PGM_buffer_set_nan(handle, PowerGridModelC.PGM_def_update_sym_load(), loadUpdate, 0, 4);

            int[] loadUpdateId = {2, 3, 2, 3};
            double[] pUpdate = {100e3, 200e3, 0.0, -200e3};
            long[] indptrLoad = {0, 2, 3, 4};
            MemorySegment loadUpdateIdSeg = toNativeIntArray(arena, loadUpdateId);
            MemorySegment pUpdateSeg = toNativeDoubleArray(arena, pUpdate);
            MemorySegment indptrLoadSeg = toNativeLongArray(arena, indptrLoad);

            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_update_sym_load_id(), loadUpdate, loadUpdateIdSeg, 0, 4, -1);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_update_sym_load_p_specified(), loadUpdate, pUpdateSeg, 0, 4, -1);

            batchUpdateDataset = PowerGridModelC.PGM_create_dataset_const(handle, arena.allocateFrom("update"), 1, 3);
            PowerGridModelC.PGM_dataset_const_add_buffer(handle, batchUpdateDataset, arena.allocateFrom("source"), 1, 3, MemorySegment.NULL, sourceUpdate);
            PowerGridModelC.PGM_dataset_const_add_buffer(handle, batchUpdateDataset, arena.allocateFrom("sym_load"), -1, 4, indptrLoadSeg, loadUpdate);
            ensureNoError(handle);

            // ---- batch calculation ----
            PowerGridModelC.PGM_calculate(handle, model, opt, batchOutputDataset, batchUpdateDataset);
            ensureNoError(handle);

            PowerGridModelC.PGM_buffer_get_value(handle, PowerGridModelC.PGM_def_sym_output_node_u_pu(), nodeOutput, uPuSeg, 0, 3, -1);
            PowerGridModelC.PGM_buffer_get_value(handle, PowerGridModelC.PGM_def_sym_output_node_u_angle(), nodeOutput, uAngleSeg, 0, 3, -1);
            System.out.println("\nBatch Calculation");
            for (int i = 0; i < 3; i++) {
                System.out.printf("Scenario %d, u_pu: %f, u_angle: %f%n", i, uPuSeg.getAtIndex(PowerGridModelC.C_DOUBLE, i), uAngleSeg.getAtIndex(PowerGridModelC.C_DOUBLE, i));
            }

            // ---- batch calculation error ----
            pUpdate[2] = 100e12;
            loadUpdateId[3] = 100;
            pUpdateSeg.setAtIndex(PowerGridModelC.C_DOUBLE, 2, pUpdate[2]);
            loadUpdateIdSeg.setAtIndex(PowerGridModelC.C_INT, 3, loadUpdateId[3]);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_update_sym_load_id(), loadUpdate, loadUpdateIdSeg, 0, 4, -1);
            PowerGridModelC.PGM_buffer_set_value(handle, PowerGridModelC.PGM_def_update_sym_load_p_specified(), loadUpdate, pUpdateSeg, 0, 4, -1);

            PowerGridModelC.PGM_calculate(handle, model, opt, batchOutputDataset, batchUpdateDataset);
            if (PowerGridModelC.PGM_error_code(handle) == PowerGridModelC.PGM_no_error()) {
                throw new IllegalStateException("Expected batch calculation to fail for invalid update data");
            }

            System.out.println("\nBatch Calculation Error");
            System.out.printf("Error code: %d%n", PowerGridModelC.PGM_error_code(handle));

            long nFailedScenarios = PowerGridModelC.PGM_n_failed_scenarios(handle);
            MemorySegment failedScenarios = PowerGridModelC.PGM_failed_scenarios(handle);
            MemorySegment batchErrs = PowerGridModelC.PGM_batch_errors(handle);
            for (int i = 0; i < nFailedScenarios; i++) {
                long failedScenario = failedScenarios.getAtIndex(PowerGridModelC.PGM_Idx, i);
                MemorySegment errorPtr = batchErrs.getAtIndex(PowerGridModelC.C_POINTER, i);
                System.out.printf("Failed scenario %d, error message: %s%n", failedScenario, cString(errorPtr));
            }

            System.out.println("Normal result:");
            System.out.printf("Scenario 0, u_pu: %f, u_angle: %f%n", uPuSeg.getAtIndex(PowerGridModelC.C_DOUBLE, 0), uAngleSeg.getAtIndex(PowerGridModelC.C_DOUBLE, 0));
            PowerGridModelC.PGM_clear_error(handle);
        } finally {
            if (!batchUpdateDataset.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_dataset_const(batchUpdateDataset);
            }
            if (!loadUpdate.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_buffer(loadUpdate);
            }
            if (!sourceUpdate.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_buffer(sourceUpdate);
            }
            if (!opt.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_options(opt);
            }
            if (!nodeOutput.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_buffer(nodeOutput);
            }
            if (!batchOutputDataset.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_dataset_mutable(batchOutputDataset);
            }
            if (!singleOutputDataset.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_dataset_mutable(singleOutputDataset);
            }
            if (!model.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_model(model);
            }
            if (!inputDataset.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_dataset_const(inputDataset);
            }
            if (!symLoadInput.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_buffer(symLoadInput);
            }
            if (!nodeInput.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_buffer(nodeInput);
            }
            if (!handle.equals(MemorySegment.NULL)) {
                PowerGridModelC.PGM_destroy_handle(handle);
            }
        }
    }

    private static void ensureNoError(MemorySegment handle) {
        long errorCode = PowerGridModelC.PGM_error_code(handle);
        if (errorCode != PowerGridModelC.PGM_no_error()) {
            throw new IllegalStateException(
                    "PGM error " + errorCode + ": " + cString(PowerGridModelC.PGM_error_message(handle))
            );
        }
    }

    private static String cString(MemorySegment cStringPtr) {
        if (cStringPtr.equals(MemorySegment.NULL)) {
            return "<null>";
        }
        return cStringPtr.getString(0, UTF_8);
    }

    private static MemorySegment toNativeIntArray(Arena arena, int[] values) {
        MemorySegment segment = arena.allocate((long) values.length * PowerGridModelC.C_INT.byteSize(), PowerGridModelC.C_INT.byteAlignment());
        for (int i = 0; i < values.length; i++) {
            segment.setAtIndex(PowerGridModelC.C_INT, i, values[i]);
        }
        return segment;
    }

    private static MemorySegment toNativeDoubleArray(Arena arena, double[] values) {
        MemorySegment segment = arena.allocate((long) values.length * PowerGridModelC.C_DOUBLE.byteSize(), PowerGridModelC.C_DOUBLE.byteAlignment());
        for (int i = 0; i < values.length; i++) {
            segment.setAtIndex(PowerGridModelC.C_DOUBLE, i, values[i]);
        }
        return segment;
    }

    private static MemorySegment toNativeLongArray(Arena arena, long[] values) {
        MemorySegment segment = arena.allocate((long) values.length * PowerGridModelC.C_LONG_LONG.byteSize(), PowerGridModelC.C_LONG_LONG.byteAlignment());
        for (int i = 0; i < values.length; i++) {
            segment.setAtIndex(PowerGridModelC.C_LONG_LONG, i, values[i]);
        }
        return segment;
    }
 }
