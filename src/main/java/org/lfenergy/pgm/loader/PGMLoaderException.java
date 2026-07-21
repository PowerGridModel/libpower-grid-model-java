package org.lfenergy.pgm.loader;

public class PGMLoaderException extends RuntimeException {
    PGMLoaderException(String message) {
        super(message);
    }

    PGMLoaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
