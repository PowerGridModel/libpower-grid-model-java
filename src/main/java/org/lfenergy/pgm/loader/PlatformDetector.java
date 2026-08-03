// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

package org.lfenergy.pgm.loader;

class PlatformDetector {

    private final SystemPropertyProvider systemPropertyProvider;

    PlatformDetector() {

        this(new SystemPropertyProvider() {

            @Override
            public String getOsArch() {

                return System.getProperty("os.arch", "");
            }

            @Override
            public String getOsName() {

                return System.getProperty("os.name", "");
            }
        });
    }

    PlatformDetector(SystemPropertyProvider systemPropertyProvider) {

        this.systemPropertyProvider = systemPropertyProvider;
    }

    Platform detectPlatform() {

        final OperatingSystem operatingSystem = detectOperatingSystem(systemPropertyProvider.getOsName());
        final Architecture architecture = detectArchitecture(systemPropertyProvider.getOsArch());

        return new Platform(operatingSystem, architecture);
    }

    private Architecture detectArchitecture(final String osArch) {

        final String normalized = osArch == null ? "" : osArch.toLowerCase();

        return switch (normalized) {
            case "aarch64", "arm64" -> Architecture.ARM64;
            case "amd64", "x86_64" -> Architecture.X86_64;
            default -> Architecture.UNKNOWN;
        };
    }

    private OperatingSystem detectOperatingSystem(final String osName) {

        final String normalized = osName == null ? "" : osName.toLowerCase();

        if (normalized.contains("mac")) {
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

    /**
     * Interface that allows for mocking in unit tests.
     */
    interface SystemPropertyProvider {

        String getOsArch();

        String getOsName();
    }
}
