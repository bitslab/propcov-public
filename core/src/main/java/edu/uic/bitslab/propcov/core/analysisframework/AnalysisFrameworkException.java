package edu.uic.bitslab.propcov.core.analysisframework;

/**
 * Represents an exception thrown for errors specific to the Analysis Framework.
 * This exception is typically used to indicate issues that occur during the execution
 * of operations within analysis-related processes, such as graph construction or property retrieval.
 */
public class AnalysisFrameworkException extends Exception {
    /**
     * Constructs a new AnalysisFrameworkException with the specified detail message.
     *
     * @param message The detail message explaining the reason for the exception.
     */
    @SuppressWarnings("unused")
    public AnalysisFrameworkException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a specific message and a cause.
     *
     * @param message The detail message explaining the reason for the exception.
     * @param cause The root cause (a throwable object) of this exception.
     */
    public AnalysisFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
