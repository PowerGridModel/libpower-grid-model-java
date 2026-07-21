package org.lfenergy.pgm.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lfenergy.pgm.loader.Architecture.*;
import static org.lfenergy.pgm.loader.OperatingSystem.*;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlatformDetectorTest {

    @Mock
    private PlatformDetector.SystemPropertyProvider systemPropertyProvider;

    @InjectMocks
    private PlatformDetector platformDetector;

    @MethodSource("testProvider")
    @ParameterizedTest
    void test(String osName, String osArch, Platform expectedPlatform) {

        when(systemPropertyProvider.getOsName()).thenReturn(osName);
        when(systemPropertyProvider.getOsArch()).thenReturn(osArch);

        final Platform actualPlatform = platformDetector.detectPlatform();

        assertEquals(expectedPlatform, actualPlatform);
    }

    static Stream<Arguments> testProvider() {

        return Stream.of(
            //
            // CPU: Apple M4 Pro (ARMv9.2-A)
            // OS: Mac OS X Tahoe 26.5.2
            // Expected platform: MACOS, ARM64
            Arguments.argumentSet(
                "MACOS, ARM64",
                "Mac OS X", "aarch64",
                new Platform(MACOS, ARM64)
            ),
            //
            // CPU: AMD Ryzen 7 6800U (x86-64)
            // OS: Arch Linux
            // Expected platform: LINUX, X86_64
            Arguments.argumentSet(
                "LINUX, X86_64",
                "Linux", "amd64",
                new Platform(LINUX, X86_64)
            ),
            //
            // CPU: Rockchip RK3399 (ARMv8-A)
            // OS: Manjaro ARM
            // Expected platform: LINUX, ARM64
            Arguments.argumentSet(
                "LINUX, ARM64",
                "Linux", "aarch64",
                new Platform(LINUX, ARM64)
            ),
            //
            // CPU: Intel Core i7 ...
            // OS: Windows 11
            // Expected platform: Windows, X86_64
            Arguments.argumentSet(
                "WINDOWS, X86_64",
                "Windows 11", "amd64",
                new Platform(WINDOWS, X86_64)
            )
        );
    }
}
