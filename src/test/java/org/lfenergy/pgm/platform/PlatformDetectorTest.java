package org.lfenergy.pgm.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PlatformDetectorTest {

    private final PlatformDetector platformDetector =  new PlatformDetector();

    @MethodSource("testProvider")
    @ParameterizedTest
    void test(String osName, String osArch, Platform expectedPlatform) {
        final Platform actualPlatform = platformDetector.detectPlatform(osName, osArch);
        assertEquals(expectedPlatform, actualPlatform);

    }

    static Stream<Arguments> testProvider() {
        return Stream.of(
            Arguments.argumentSet(
                "MACOS, ARM64",
                "Mac OS X", "aarch64",
                new Platform(OperatingSystem.MACOS, Architecture.ARM64)
            ),
            Arguments.argumentSet(
                "MACOS, ARM64",
                "Mac OS X", "aarch64",
                new Platform(OperatingSystem.MACOS, Architecture.X86_64)
            )
        );
    }
}
