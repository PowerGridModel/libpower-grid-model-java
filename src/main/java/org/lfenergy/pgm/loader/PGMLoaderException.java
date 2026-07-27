package org.lfenergy.pgm.loader;

import java.io.Serial;

public class PGMLoaderException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -4755108178968242882L;

    PGMLoaderException(String message) {
        super(message);
    }

    PGMLoaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
