package edu.uic.bitslab.propcov.core.config;

/**
 * An exception that is thrown to indicate a configuration error.
 * This exception is typically used to signal issues that arise during
 * the configuration process, such as invalid configuration parameters
 * or missing configuration files.
 */
public class ConfigException extends Exception {
    /**
     * Constructs a new ConfigException with the specified detail message.
     * This constructor allows for providing a specific error message
     * to identify the nature of the configuration issue.
     *
     * @param message the detail message describing the configuration error
     */
    public ConfigException(String message) {
        super(message);
    }
}
