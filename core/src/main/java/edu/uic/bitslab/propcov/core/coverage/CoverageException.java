package edu.uic.bitslab.propcov.core.coverage;

/**
 * Represents an exception specific to the coverage framework.
 */
public class CoverageException extends Exception {
    /**
     * Constructs a new CoverageException with the specified detail message and cause.
     *
     * @param message the detail message, which provides additional information about the exception.
     * @param cause the cause of the exception, which may indicate the underlying issue that led to this exception.
     */
    public CoverageException(String message, Throwable cause) {
        super(message, cause);
    }
}
