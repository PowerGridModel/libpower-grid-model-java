// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

package org.lfenergy.pgm.loader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.lfenergy.pgm.loader.Architecture.*;
import static org.lfenergy.pgm.loader.OperatingSystem.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.lfenergy.pgm.loader.PGMLoader.PGMInvoker;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PGMLoaderTest {

    @Mock
    private PlatformDetector platformDetector;

    @Mock
    private ResourceLibraryLoader resourceLibraryLoader;

    @Mock
    private PGMInvoker pgmInvoker;

    @InjectMocks
    private PGMLoader pgmLoader;

    @Captor
    private ArgumentCaptor<String> resourceLibraryLoaderCaptor;

    private static String pgmBuildVersion;
    private static MemorySegment pgmBuildVersionPointer;

    @BeforeAll
    static void beforeAll() {

        final InputStream ris = PGMLoader.class.getResourceAsStream("/version");

        try (InputStream is = new BufferedInputStream(ris)) {
            pgmBuildVersion = new String(is.readAllBytes(), UTF_8);
            pgmBuildVersionPointer = createNullTerminatedString(pgmBuildVersion);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected: failed to read version resource", e);
        }
    }

    @MethodSource("testAutoloadProvider")
    @ParameterizedTest
    void testAutoload(Platform platform,
        String expectedResourceFile) {

        // Stub the static method
        when(pgmInvoker.PGM_version())
            .thenReturn(pgmBuildVersionPointer);

        when(platformDetector.detectPlatform()).thenReturn(platform);

        pgmLoader.autoload();

        // Second call to test the caching (it should only call the PlatformDetector and ResourceLibraryLoader once)
        pgmLoader.autoload();

        verify(platformDetector, times(1)).detectPlatform();
        verify(resourceLibraryLoader, times(1)).loadResourceLibrary(resourceLibraryLoaderCaptor.capture());

        assertEquals(expectedResourceFile, resourceLibraryLoaderCaptor.getValue());
    }

    static Stream<Arguments> testAutoloadProvider() {

        return Stream.of(
            argumentSet(
                "MACOS, ARM64",
                new Platform(MACOS, ARM64),
                "power_grid_model_c_arm64_macosx.dylib"
            ), argumentSet(
                "MACOS, X86_64",
                new Platform(MACOS, X86_64),
                "power_grid_model_c_x86_64_macosx.dylib"
            ), argumentSet(
                "LINUX, ARM64",
                new Platform(LINUX, ARM64),
                "power_grid_model_c_arm64_linux.so"
            ), argumentSet(
                "LINUX, X86_64",
                new Platform(LINUX, X86_64),
                "power_grid_model_c_x86_64_linux.so"
            ), argumentSet(
                "WINDOWS, X86_64",
                new Platform(WINDOWS, X86_64),
                "power_grid_model_c_x86_64_windows.dll"
            )
        );
    }

    @Test
    void testCheckThrowsVersionMismatchExceptionWhenCheckingVersionAndVersionsNotEqual() {

        when(pgmInvoker.PGM_version())
            .thenReturn(createNullTerminatedString("This is not a valid PGM version"));

        // Should throw
        assertThrows(VersionMismatchException.class, () -> pgmLoader.check(true));
    }

    @Test
    void testCheckDoesNotThrowWhenCheckingVersionAndVersionsEqual() {

        when(pgmInvoker.PGM_version())
            .thenReturn(pgmBuildVersionPointer);

        // Should not throw
        assertDoesNotThrow(() -> pgmLoader.check(true));
    }

    @Test
    void testCheckDoesNotThrowWhenNotCheckingVersionAndVersionsNotEqual() {

        when(pgmInvoker.PGM_version())
            .thenReturn(createNullTerminatedString("This is not a valid PGM version"));

        // Should not throw
        assertDoesNotThrow(() -> pgmLoader.check(false));
    }

    private static MemorySegment createNullTerminatedString(String string) {

        ByteBuffer buffer = UTF_8.encode(string);
        byte[] bytes = new byte[buffer.remaining() + 1];
        buffer.get(bytes, 0, buffer.remaining());
        bytes[bytes.length - 1] = (byte) 0;
        return MemorySegment.ofArray(bytes);
    }
}
