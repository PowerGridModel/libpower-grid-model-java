// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

package org.lfenergy.pgm.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.lfenergy.pgm.PowerGridModelC;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResourceLibraryLoaderTest {

    @Mock
    private ResourceLibraryLoader.Dependencies dependencies;

    @InjectMocks
    private ResourceLibraryLoader resourceLibraryLoader;

    @Captor
    private ArgumentCaptor<String> systemLoadCaptor;

    /**
     * Checks if the resource file exists.
     */
    @ParameterizedTest
    @MethodSource("testProvider")
    void resourceFileExists(String resourceLibraryPath) {

        assertNotNull(PowerGridModelC.class.getResource(resourceLibraryPath));
    }

    /**
     * Tests loading the native PGM library from the resources when the resource is not a file on the filesystem, but e.g. part of the jar (which is
     * a zip-file). In those cases, it should copy the resource file to a temporary file, and load it from there.
     */
    @ParameterizedTest
    @MethodSource("testProvider")
    void testWhenResourceIsFile(String resourceLibraryPath) throws Exception {

        final URL resourceURL = PowerGridModelC.class.getResource(resourceLibraryPath);

        when(dependencies.openResource(resourceLibraryPath))
            .thenReturn(resourceURL);

        when(dependencies.resourceIsFile(resourceURL))
            .thenReturn(true);

        resourceLibraryLoader.loadResourceLibrary(resourceLibraryPath);

        verify(dependencies, times(1)).openResource(resourceLibraryPath);
        verify(dependencies, times(1)).resourceIsFile(resourceURL);
        verify(dependencies, never()).copyIntoTemporaryFile(any(), any(), any());
        verify(dependencies, times(1)).loadNativeLibrary(systemLoadCaptor.capture());
        assertEquals(resourceURL.getFile(), systemLoadCaptor.getValue());
    }

    /**
     * Tests loading the native PGM library from the resources when the resource is a file on the filesystem, which is usually the case during
     * development (when deployed, the resource file is an entry in the zip-file). When the resource is a file on the filesystem, it should load the
     * file directly, and not first copy it to a temporary file.
     */
    @ParameterizedTest
    @MethodSource("testProvider")
    void testWhenResourceIsNotFile(String resourceLibraryPath) throws Exception {

        final String expectedPathString = "/foo/bar";
        final Path expectedPath = Path.of(expectedPathString);
        final URL resourceURL = PowerGridModelC.class.getResource(resourceLibraryPath);

        when(dependencies.copyIntoTemporaryFile(any(), any(), any()))
            .thenReturn(expectedPath);

        when(dependencies.openResource(resourceLibraryPath))
            .thenReturn(resourceURL);

        when(dependencies.resourceIsFile(resourceURL))
            .thenReturn(false);

        resourceLibraryLoader.loadResourceLibrary(resourceLibraryPath);

        verify(dependencies, times(1)).openResource(resourceLibraryPath);
        verify(dependencies, times(1)).resourceIsFile(resourceURL);
        verify(dependencies, times(1)).copyIntoTemporaryFile(any(), any(), any());
        verify(dependencies, times(1)).loadNativeLibrary(systemLoadCaptor.capture());
        assertEquals(expectedPathString, systemLoadCaptor.getValue());
    }

    static Stream<Arguments> testProvider() {

        return Stream.of(
            Arguments.argumentSet("LINUX, ARM64", "/power_grid_model_c_arm64_linux.so"),
            Arguments.argumentSet("LINUX, X86_64", "/power_grid_model_c_x86_64_linux.so"),
            Arguments.argumentSet("MACOS, ARM64", "/power_grid_model_c_arm64_macosx.dylib"),
            Arguments.argumentSet("MACOS, X86_64", "/power_grid_model_c_x86_64_macosx.dylib"),
            Arguments.argumentSet("WINDOWS, X86_64", "/power_grid_model_c_x86_64_windows.dll")
        );
    }
}
