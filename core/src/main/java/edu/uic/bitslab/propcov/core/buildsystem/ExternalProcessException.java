package edu.uic.bitslab.propcov.core.buildsystem;

/**
 * Represents an exception thrown when an external process
 * executed as part of the build system encounters an error.
 * This exception is generally used for capturing and
 * handling errors related to external processes such as commands,
 * scripts, or other executable components.
 */
public class ExternalProcessException extends Exception {
    /**
     * Constructs a new ExternalProcessException with the specified detail message.
     *
     * @param message the detail message providing additional information about the exception.
     */
    public ExternalProcessException(String message) {
        super(message);
    }
}
