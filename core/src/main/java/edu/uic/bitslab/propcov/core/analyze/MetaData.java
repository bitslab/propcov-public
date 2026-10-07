package edu.uic.bitslab.propcov.core.analyze;

import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.util.timer.RunTimer;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;

import java.util.*;

import static edu.uic.bitslab.propcov.core.analyze.CoverageData.CoverageDataType;

/**
 * A class to encapsulate metadata and tracking information during runtime.
 * This includes details such as elapsed time, errors, and test properties.
 * The class provides mechanisms to log, manage, and evaluate runtime properties.
 */
public class MetaData {
    private static final org.slf4j.Logger LOGGER = Util.getLogger(MetaData.class);

    /**
     * A label or identifier used to describe or differentiate specific metadata or runtime information.
     */
    public String label;
    /**
     * A map used to track and store common timing metrics.
     * The keys represent specific timing categories,
     * and the values represent the associated duration in seconds.
     */
    public final Map<String, Double> timesCommon = new HashMap<>();
    /**
     * A mapping of property identifiers to their corresponding {@code PropertyTest} instances,
     * used to track and manage runtime data specific to property evaluations.
     * <p>
     * Each entry in this map corresponds to a distinct property and contains
     * details such as elapsed time, trial counts, and error statuses for evaluating that property.
     * This allows for granular tracking and assessment of individual properties during execution.
     */
    public final Map<String, PropertyTest> propertyTest = new HashMap<>();
    /**
     * A list of error messages encountered during runtime.
     * This variable is used to track and store error descriptions
     * for further evaluation or debugging purposes.
     * <p>
     * It is updated when errors occur and can be used in conjunction
     * with other class variables such as {@code hasError} to determine
     * the runtime state or issues.
     */
    public final List<String> errors = new ArrayList<>();
    /**
     * Indicates whether any error has occurred during runtime.
     * This variable is set to true when an error is added to the error list,
     * or when any property test encounters an error.
     */
    public boolean hasError = false;
    /**
     * Tracks the total elapsed time, in seconds, for the execution of an operation or process.
     * This variable is updated to reflect the cumulative runtime from the associated `RunTimer`.
     * It is used to monitor and log the overall performance duration within the runtime metadata.
     */
    public double totalElapsedTime = 0.00;

    /**
     * A map that stores coverage details based on the type of coverage data.
     * The key represents the specific type of coverage being analyzed, as defined in the {@link CoverageDataType} enumeration.
     * The value represents detailed information about the corresponding coverage data.
     * This map is populated via the "coverage apply" function during runtime analysis.
     */
    // contains coverage information from the coverage apply function
    public final Map<CoverageDataType, CoverageDetail> coverage = new HashMap<>();

    private final RunTimer elapsedTimer;

    /**
     * Constructs a MetaData object and initializes the elapsedTimer field.
     * <p>
     * The elapsedTimer is an instance of RunTimer configured to measure
     * the total runtime of operations associated with this instance. The timer
     * is immediately started upon creation of the MetaData object.
     *
     * @throws TimerException if the RunTimer instance cannot be started,
     *                        such as when the timer is already running.
     */
    public MetaData() throws TimerException {
        elapsedTimer = new RunTimer("TOTAL", LOGGER);
    }

    /**
     * Adds an error message to the internal error tracking and flags that an error has occurred.
     *
     * @param errorMsg the error message to be added to the error list
     */
    public void addError(String errorMsg) {
        errors.add(errorMsg);
        hasError = true;
    }

    /**
     * PropertyTest is a static inner class designed to track and manage property-based testing
     * metrics and error information. It provides fields for collecting time metrics, trial counts,
     * and error states during the execution of property tests.
     */
    public static class PropertyTest {
        /**
         * A map that tracks the execution times of specific properties in seconds.
         * The keys represent the names of the properties, and the values indicate
         * the respective duration each property took to execute. This field is used
         * to collect and store performance metrics for property-based tests.
         */
        public final Map<String, Double> propertyTimes = new HashMap<>();
        /**
         * Represents the elapsed time in seconds for a property-based test.
         * This variable is used to measure and store the duration taken to execute a specific test property.
         */
        public double propertyElapsedTimeInSeconds = 0.00;
        /**
         * A field representing an optional override for the number of trials to be executed
         * during property-based testing. If the value is -1, the override is not applied, and
         * the default trial configuration is used.
         */
        public long overrideTrials = -1;
        /**
         * Represents the number of trials that are expected to run for a property-based test.
         * This value is initialized to -1, indicating that the expected trial count has not been explicitly set.
         * It can be used to compare the planned number of trials with the actual number of trials run.
         */
        public long trialsExpected = -1;
        /**
         * Represents the number of trials completed during the execution of a property-based test.
         * The value is initialized to -1, indicating that no trials have been executed yet.
         */
        public long trialsRan = -1;

        /**
         * Indicates whether an error occurred during the execution of a property test.
         * This flag is set to `true` if an error is detected, otherwise it remains `false`.
         */
        public boolean error = false;
        /**
         * Stores an error message associated with a property test.
         * This message provides details about the error encountered during the execution
         * of the property test, if applicable. It is meant to be used in conjunction with
         * the {@code error} field to deliver context for a failure or issue detected.
         */
        public String errorMsg = "";

        /**
         * Sets the error state and the corresponding error message for a property test.
         *
         * @param error whether an error occurred during the property test
         * @param errorMsg a message describing the error
         */
        public void setError(boolean error, String errorMsg) {
            this.error = error;
            this.errorMsg = errorMsg;
        }
    }

    /**
     * Completes the timing operation for the current process and calculates the total elapsed time.
     * This method stops the internal timer and retrieves the total elapsed time
     * in seconds. Additionally, it updates the error status by checking whether
     * any property in the propertyTest collection indicates an error.
     *
     * @return the total elapsed time in seconds as a double
     * @throws TimerException if an issue occurs while stopping the timer
     */
    public double done() throws TimerException {
        elapsedTimer.stop();
        totalElapsedTime = elapsedTimer.elapsedInSeconds();

        hasError = hasError || propertyTest.values().stream().anyMatch( p -> p.error );

        return totalElapsedTime;
    }
}