// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

package org.lfenergy.pgm.loader;

import java.io.Serial;

public class VersionMismatchException extends PGMLoaderException {

    @Serial
    private static final long serialVersionUID = 4716888612549282053L;

    private final String runtimeVersion;
    private final String buildVersion;

    public VersionMismatchException(String runtimeVersion,
        String buildVersion) {

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
