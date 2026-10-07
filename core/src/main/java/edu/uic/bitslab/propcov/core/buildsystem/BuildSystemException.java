package edu.uic.bitslab.propcov.core.buildsystem;

/**
 * Represents an exception specific to the build system. This exception is used
 * to signal issues encountered during the operation of the build system.
 * It allows for specifying an error message and optionally a root cause.
 */
public class BuildSystemException extends Exception {
    /**
     * Constructs a new {@code BuildSystemException} with the specified error message.
     * This constructor allows for providing a detailed message describing the reason
     * for the exception to help identify and address issues encountered during the
     * execution of the build system.
     *
     * @param message the error message describing the specific cause of the exception
     */
    public BuildSystemException(String message) {
        super(message);
    }

    /**
     * Constructs a new BuildSystemException with the specified detail message and cause.
     * This constructor allows for specifying the underlying cause of the exception
     * along with an error message to provide more context about the error.
     *
     * @param message the detail message that describes the exception.
     * @param cause the cause of the exception (a {@link Throwable} object which caused
     *              this exception). A null value is permitted, indicating that the cause
     *              is nonexistent or unknown.
     */
    public BuildSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
