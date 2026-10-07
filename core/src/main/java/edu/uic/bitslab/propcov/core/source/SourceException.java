package edu.uic.bitslab.propcov.core.source;

/**
 * This exception is thrown to indicate that an error occurred while interacting
 * with or retrieving a source resource within the context of a project source configuration.
 */
public class SourceException  extends Exception {
    /**
     * Constructs a new {@code SourceException} with the specified detail message.
     * This exception is typically used to indicate an error related to the source
     * configuration or retrieval process in a project context.
     *
     * @param message The detail message, which provides additional information
     *                about the exception and its cause.
     */
    public SourceException(String message) {
        super(message);
    }

    /**
     * Constructs a new SourceException with a specified detailed message and cause.
     *
     * @param message the detail message that provides additional information about the error
     * @param cause the cause of the exception (a throwable object that resulted in this exception being thrown)
     */
    public SourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
