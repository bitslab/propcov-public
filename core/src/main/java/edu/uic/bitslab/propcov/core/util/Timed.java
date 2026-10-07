package edu.uic.bitslab.propcov.core.util;

import java.util.concurrent.*;

/**
 * The Timed class provides a utility for executing tasks with a specified timeout.
 * It uses a single-threaded executor to run the provided {@link Callable} and enforces
 * a timeout for task execution. If the task exceeds the timeout, it is canceled
 * and a {@link TimeoutException} is thrown.
 *
 * @param <R> the result type of the task to be executed
 */
public class Timed<R> {
    private static final long UNLIMITED = 0;

    final long timeout;
    final TimeUnit unit;

    /**
     * Constructs an instance of the Timed class with the specified timeout and time unit.
     * This is used to define the maximum duration within which a task must complete.
     *
     * @param timeout the timeout duration for task execution, in the base unit defined by {@code unit}
     * @param unit the {@link TimeUnit} that defines the unit of the {@code timeout} parameter
     */
    public Timed(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    /**
     * Executes the given task within a single-threaded executor, enforcing a timeout if specified.
     * Cancels the task if the execution exceeds the timeout.
     *
     * @param task the task to execute, provided as a {@link Callable} that returns a result of type {@code R}
     * @return the result returned by the execution of the task
     * @throws ExecutionException       if the task threw an exception during execution
     * @throws InterruptedException     if the current thread was interrupted while waiting for the task to complete
     * @throws TimeoutException         if the task execution exceeded the specified timeout
     */
    public R exec(Callable<R> task) throws ExecutionException, InterruptedException, TimeoutException {
        final ExecutorService service = Executors.newSingleThreadExecutor();
        final Future<R> f;

        try {
            f = service.submit(task);
        } catch (RejectedExecutionException e) {
            service.shutdown();
            throw e;
        }

        try {
            return timeout == UNLIMITED ? f.get() : f.get(timeout, unit);
        } catch (TimeoutException e) {
            f.cancel(true);
            throw e;
        } finally {
            service.shutdown();
        }
    }
}