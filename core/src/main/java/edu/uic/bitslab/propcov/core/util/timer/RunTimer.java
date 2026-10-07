package edu.uic.bitslab.propcov.core.util.timer;

import org.slf4j.Logger;

/**
 * RunTimer is a utility class for measuring the duration of activities. It allows
 * users to start, stop, and retrieve elapsed time for timed operations with optional
 * logging for monitoring each timed activity.
 */
public class RunTimer {
    private final long UNINITIALIZED = -1;
    private final Logger LOGGER;
    /**
     * A descriptive label for the timer used to identify the activity being measured.
     */
    public final String label;
    private long start = UNINITIALIZED;
    private long elapsedTime = UNINITIALIZED;
    private boolean running = false;

    /**
     * Constructs a new {@code RunTimer} instance with a label and a specific logger, and optionally starts the timer.
     *
     * @param label the label associated with this timer, used to identify the timed operation
     * @param LOGGER the logger used to log timing information for this timer
     * @throws TimerException if the timer cannot be started, such as when it is already running
     */
    public RunTimer(String label, Logger LOGGER) throws TimerException {
        this(label, LOGGER, true);
    }

    /**
     * Constructs a new RunTimer instance for measuring the duration of activities.
     * The timer can be optionally started upon instantiation.
     *
     * @param label A descriptive label for the timer to identify the activity being measured.
     * @param LOGGER The Logger instance used for logging timer actions and information.
     * @param start A boolean flag indicating whether to start the timer immediately.
     * @throws TimerException If an error occurs when attempting to start the timer,
     *                         such as if the timer is already running.
     */
    public RunTimer(String label, Logger LOGGER, boolean start) throws TimerException {
        this.label = label;
        this.LOGGER = LOGGER;

        if (start) start();
    }

    /**
     * Starts the timer for measuring elapsed time. If the timer is already
     * running, this method will throw a {@code TimerException}.
     * <p>
     * Logging functionality is used to record the start of the timer, if
     * logging is enabled. The start time is recorded in nanoseconds, which
     * can later be used to calculate the elapsed time using {@link #elapsed()}
     * or {@link #elapsedInSeconds()}.
     *
     * @throws TimerException if the timer is already running when this method
     *         is called. This ensures that overlapping intervals are not created.
     */
    public void start() throws TimerException {
        if (running) throw new TimerException("Called start while still running (hint: call Stop first).");

        running = true;
        LOGGER.info("Starting: {}", label);
        this.start = System.nanoTime();
    }

    /**
     * Calculates and returns the elapsed time in nanoseconds. If the timer is still running,
     * it computes the elapsed time based on the current time and the start time. If the timer
     * has been stopped, it simply returns the previously recorded elapsed time.
     *
     * @return the elapsed time in nanoseconds as a long value.
     */
    public long elapsed() {
        if (running) {
            elapsedTime = System.nanoTime() - start;
        }

        return elapsedTime;
    }

    /**
     * Calculates and returns the total elapsed time in seconds.
     * The method converts the elapsed time measured in nanoseconds
     * into seconds by dividing it by 1,000,000,000. This value
     * represents the duration since the timer was started, up to the
     * last call to `stop()` or the current time if the timer is still running.
     *
     * @return the elapsed time in seconds as a double
     */
    public double elapsedInSeconds() {
        long elapsed = elapsed();
        return (double) elapsed / 1_000_000_000; // nano to seconds
    }

    /**
     * Stops the timer and calculates the elapsed time since the timer was started.
     * <p>
     * Once the timer is stopped, the elapsed time is recorded in nanoseconds and
     * converted to seconds for logging. This method also updates and logs the elapsed
     * time and the specific label associated with the timer.
     *
     * @throws TimerException if the stop method is called when the timer is not currently running.
     */
    public void stop() throws TimerException {
        if (!running) throw new TimerException("Called stop when not running (Hint call Start first).");

        running = false;
        elapsedTime = System.nanoTime() - start;
        LOGGER.info("Elapsed time (s): {}", elapsedInSeconds());
        LOGGER.info("Ended: {}", label);
    }
}