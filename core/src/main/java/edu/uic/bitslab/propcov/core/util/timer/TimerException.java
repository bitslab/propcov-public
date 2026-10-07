package edu.uic.bitslab.propcov.core.util.timer;

/**
 * Represents an exception thrown when operations on a timer
 * are performed incorrectly, such as starting a timer that is already
 * running or stopping a timer that is not active.
 */
public class TimerException extends Exception {
    /**
     * Constructs a new TimerException with the specified detail message.
     *
     * @param message the detail message that provides additional context
     *                about the exception. For example, it can describe
     *                an invalid operation on a timer, such as starting
     *                a timer that is already running or stopping one
     *                that is not running.
     */
    public TimerException(String message) {
        super(message);
    }
}
