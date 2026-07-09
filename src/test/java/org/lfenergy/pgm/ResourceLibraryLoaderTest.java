package org.lfenergy.pgm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.lfenergy.pgm.libraryloader.TemporaryFileHandler;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResourceLibraryLoaderTest {

    @Mock
    Consumer<String> systemLoader;

    @Mock
    TemporaryFileHandler temporaryFileHandler;

    @InjectMocks
    ResourceLibraryLoader resourceLibraryLoader;

    @Captor
    ArgumentCaptor<String> systemLoadCaptor;

    @ParameterizedTest
    @MethodSource("testProvider")
    void test(String resourceLibraryPath) throws Exception {
        resourceLibraryLoader.loadResourceLibrary(resourceLibraryPath);
        when(temporaryFileHandler.copyIntoTemporaryFile(any(), any(), any())).thenReturn(Path.of(resourceLibraryPath));

//        verify(temporaryFileHandler, times(1)).copyIntoTemporaryFile(any(), any(), any());
//        verify(systemLoader, times(1)).accept(systemLoadCaptor.capture());

        final byte[] expectedHash = createFileHash(PowerGridModelC.class.getResourceAsStream(resourceLibraryPath));
        final byte[] actualHash = createFileHash(new FileInputStream(systemLoadCaptor.getValue()));

        assertEquals(expectedHash, actualHash);
    }

    private byte[] createFileHash(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        byte[] buffer = new byte[8192];
        int count;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
            while ((count = bis.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
        }

        return digest.digest();
    }

    static Stream<Arguments> testProvider() {
        return Stream.of(
            Arguments.argumentSet("ARM64 Linux", "/power_grid_model_c_arm64_linux.so")
        );
    }

}
