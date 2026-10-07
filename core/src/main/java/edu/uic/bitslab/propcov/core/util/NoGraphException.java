package edu.uic.bitslab.propcov.core.util;

/**
 * Represents an exception thrown when a graph cannot be built or retrieved during the
 * analysis process. This exception is typically used in scenarios where graph-based operations,
 * such as constructing call graphs, fail due to missing data, configuration errors, or unsupported
 * scenarios.
 */
public class NoGraphException extends Exception {
    /**
     * Constructs a new NoGraphException with the specified detail message.
     *
     * @param message the detail message, providing more information about the exception or its cause
     */
    public NoGraphException(String message) {
        super(message);
    }
}