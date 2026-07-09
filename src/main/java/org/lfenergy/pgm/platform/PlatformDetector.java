package org.lfenergy.pgm.platform;

public class PlatformDetector {

    public Platform detectPlatform() {
       return detectPlatform(
           System.getProperty("os.name", ""),
           System.getProperty("os.arch", "")
       );
    }

    public Platform detectPlatform(String osName, String osArch) {

        OperatingSystem operatingSystem = detectOperatingSystem(osName);
        Architecture architecture = detectArchitecture(osArch);

        return new Platform(operatingSystem, architecture);
    }

    private Architecture detectArchitecture(final String osArch) {

        String normalized = osArch == null ? "" : osArch.toLowerCase();

        return switch (normalized) {
            case "aarch64", "arm64" -> Architecture.ARM64;
            case "amd64", "x86_64" -> Architecture.X86_64;
            default -> Architecture.UNKNOWN;
        };
    }

    private OperatingSystem detectOperatingSystem(final String osName) {
        String normalized = osName == null ? "" : osName.toLowerCase();

        if (normalized.contains("mac") || normalized.contains("darwin")) {
            return OperatingSystem.MACOS;
        }
        if (normalized.contains("linux")) {
            return OperatingSystem.LINUX;
        }
        if (normalized.contains("win")) {
            return OperatingSystem.WINDOWS;
        }
        return OperatingSystem.UNKNOWN;
    }
}
