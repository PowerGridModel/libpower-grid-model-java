package org.lfenergy.pgm.loader;

public class VersionMismatchException extends PGMLoaderException {

    private final String runtimeVersion;
    private final String buildVersion;

    public VersionMismatchException(String runtimeVersion, String buildVersion) {
        super(String.format("jpgm was built for version %s, but version %s is present at runtime", buildVersion, runtimeVersion));
        this.runtimeVersion = runtimeVersion;
        this.buildVersion = buildVersion;
    }

    public String getRuntimeVersion() {

        return runtimeVersion;
    }

    public String getBuildVersion() {

        return buildVersion;
    }
}
