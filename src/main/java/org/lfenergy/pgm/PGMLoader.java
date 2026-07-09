package org.lfenergy.pgm;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lfenergy.pgm.libraryloader.TemporaryFileHandler;
import org.lfenergy.pgm.platform.Platform;
import org.lfenergy.pgm.platform.PlatformDetector;

/**
 * TODO: custom exceptions?
 */
public class PGMLoader {

    private static final AtomicBoolean loaded = new AtomicBoolean(false);

    /**
     * Resolves and loads the Power Grid Model native library exactly once,
     * regardless of how many times this method is called. Subsequent calls are
     * no-ops and return immediately.
     * <p>
     * This is a convenience alternative to the explicit two-step pattern:
     * <pre>{@code
     * String path = NativeLibraryResolver.resolveNativeLibraryPath(PowerGridModelC.class);
     * System.load(path);
     * }</pre>
     *
     * @throws IllegalStateException if the native library cannot be found
     * @throws RuntimeException      if the library resource cannot be extracted
     */
    public static void autoload() {
        if (loaded.compareAndSet(false, true)) {
            PlatformDetector platformDetector = new PlatformDetector();
            ResourceLibraryLoader resourceLibraryLoader = new ResourceLibraryLoader(System::load, TemporaryFileHandler.defaultHandler());

            // Detect platform
            Platform platform = platformDetector.detectPlatform();

            // Determine resource path for library
            String libraryResourcePath = determineLibraryResourcePath(platform);

            // Load library
            resourceLibraryLoader.loadResourceLibrary(libraryResourcePath);
        }
    }

    private static String determineLibraryResourcePath(Platform platform) {
        // todo: exception
        // todo: detect windows arm64 edge case
        String archPart = switch (platform.architecture()) {
            case ARM64 -> "arm64";
            case X86_64 -> "x86_64";
            default -> "unknown";
        };

        String osPart = switch (platform.operatingSystem()) {
            case MACOS -> "macosx.dylib";
            case LINUX -> "linux.so";
            case WINDOWS -> "windows.dll";
            default -> "unknown";
        };

        return String.format("power_grid_model_c_%s_%s", archPart, osPart);
    }

    public static void verifyLoaded() throws IOException {
        String runtimeVersion = PowerGridModelC.PGM_version().getString(0, UTF_8);
        String buildVersion = new String(PGMLoader.class.getResourceAsStream("/version").readAllBytes(), UTF_8);

        if (!runtimeVersion.equals(buildVersion)) {
            throw new IllegalStateException(String.format("jpgm was built for version %s, but version %s is present at runtime", buildVersion, runtimeVersion));
        }
    }
}
