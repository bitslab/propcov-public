package edu.uic.bitslab.propcov.core.buildsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.util.timer.RunTimer;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The ExternalProcess class facilitates the execution of external shell commands
 * within a specified working directory using a given environment setup, while
 * providing options for timeout and result processing.
 */
public class ExternalProcess {
    private Logger LOGGER;

    private String command;
    private String methodSource;
    private Path workingDirectory;
    private Map<String, String> environment;
    private final long timeoutInSeconds;
    private final Map<String, String> jsonData = new HashMap<>();

    /**
     * Constructs an instance of the ExternalProcess class with a specified timeout.
     * The timeout determines the maximum duration allowed for the process to execute.
     * If a negative value is provided, an IllegalArgumentException is thrown.
     *
     * @param timeoutInSeconds the timeout in seconds for the external process execution.
     *                         Must be greater than or equal to 0.
     * @throws IllegalArgumentException if the provided timeoutInSeconds is less than 0.
     */
    public ExternalProcess(long timeoutInSeconds) {
        if (timeoutInSeconds < 0) throw new IllegalArgumentException("timeoutInSeconds must be >= 0");
        this.timeoutInSeconds = timeoutInSeconds;
    }

    /**
     * Sets the command to be executed by the external process. This method allows
     * specifying the exact command that the process should run.
     *
     * @param command the command to execute, represented as a string
     * @return the current instance of {@code ExternalProcess} for method chaining
     */
    public ExternalProcess command(String command) {
        this.command = command;
        return this;
    }

    /**
     * Sets the method source for this {@code ExternalProcess} and returns the updated instance.
     *
     * @param methodSource the method source to set for this process
     * @return the updated {@code ExternalProcess} instance with the specified method source
     */
    @SuppressWarnings("unused")
    public ExternalProcess methodSource(String methodSource) {
        this.methodSource = methodSource;
        return this;
    }

    /**
     * Sets the working directory for the external process.
     *
     * @param directory the path to the working directory where the external process will be executed
     * @return the current instance of {@code ExternalProcess} for method chaining
     */
    public ExternalProcess workingDirectory(Path directory) {
        this.workingDirectory = directory;
        return this;
    }

    /**
     * Sets the environment variables for the external process and makes the map unmodifiable.
     *
     * @param environment a map containing the environment variables, where each key
     *                    is the variable name and the corresponding value is the variable value
     * @return the current instance of {@code ExternalProcess} with the environment configured
     */
    public ExternalProcess environment(Map<String, String> environment) {
        this.environment = Collections.unmodifiableMap(environment);
        return this;
    }

    /**
     * Executes an external process based on the specified command and configurations,
     * including environment variables, working directory, and timeout settings.
     * The method monitors the process, logs output and errors, and enforces a timeout
     * if one is set. Upon process completion, it returns a {@code Result} object
     * containing the process's exit code, elapsed time, JSON data, and any additional errors.
     *
     * @return a {@code Result} object containing the external process's exit code,
     *         elapsed execution time in seconds, any parsed JSON data, and additional errors if present.
     * @throws ExternalProcessException if the command is not set or if there are issues resolving the method source.
     * @throws TimerException if an error occurs with the timer mechanism.
     * @throws IOException if an I/O error occurs while starting or interacting with the process.
     * @throws InterruptedException if the current thread is interrupted while waiting for the process to complete.
     */
    public Result run() throws ExternalProcessException, TimerException, IOException, InterruptedException {
        // validate and set defaults
        if (command == null) throw new ExternalProcessException("Command must not be null.");
        if (methodSource == null) methodSource = getMethodName();
        if (workingDirectory == null) workingDirectory = Paths.get("").toAbsolutePath();
        if (environment == null) environment = Collections.emptyMap();

        LOGGER = Util.getLogger(methodSource);

        boolean isWindows = isWindows();
        String[] commands = {
                isWindows ? "cmd.exe" : "bash",
                isWindows ? "/c" : "-c",
                command
        };

        // setup process builder
        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream(true);
        pb.environment().putAll(environment);
        pb.command(commands);
        pb.directory(workingDirectory.toFile());

        // start timer
        RunTimer timer = new RunTimer(String.join(" ", commands), LOGGER);

        LOGGER.info("Working Directory: {}", pb.directory().toString());
        Process process = pb.start();

        try(
            BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader processErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
        ) {
            long waitForTickInMs = 100;
            long timeoutInNanoSeconds = timeoutInSeconds * 1000000000;

            do {
                while (processOutputReader.ready()) LOGGER.info(parseJSONData(processOutputReader.readLine()));
                while (processErrorReader.ready()) LOGGER.error(parseJSONData(processErrorReader.readLine()));
                System.out.flush();
                System.err.flush();

                if (timeoutInSeconds != 0 && timer.elapsed() > timeoutInNanoSeconds) {
                    // exceeded, so exit
                    LOGGER.error("Process ran longer than provided timeout of {}", timeoutInSeconds);
                    process.destroyForcibly();
                    LOGGER.error("Forcibly stopped process");
                }
            } while (!process.waitFor(waitForTickInMs, TimeUnit.MILLISECONDS));

            while (processOutputReader.ready()) LOGGER.info(parseJSONData(processOutputReader.readLine()));
            while (processErrorReader.ready()) LOGGER.error(parseJSONData(processErrorReader.readLine()));
            System.out.flush();
            System.err.flush();
        }

        // end timer
        timer.stop();

        // report error value (if we have it)
        if (process.exitValue() != 0) LOGGER.error("Process exited with error: {}", process.exitValue());

        return new Result(process.exitValue(), timer.elapsedInSeconds(), jsonData, "");
    }

    private String parseJSONData(String line) {
        if (line.startsWith("JSONDATA::")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> lineJsonData = mapper.readValue(
                    line.substring(10),
                    TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class)
                );

                lineJsonData.forEach( (k, v) -> {
                    if (jsonData.putIfAbsent(k, v) != null) {
                        LOGGER.error("Duplicate key {}", k);
                    }
               });
            } catch (JsonProcessingException e) {
                LOGGER.error("Exception processing json data.", e);
            }
        }
        return line;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    private static String getMethodName() throws ExternalProcessException {
        StackTraceElement[] stackTraceElements = (new Throwable()).getStackTrace();

        for (StackTraceElement stackTraceElement : stackTraceElements) {
            String stackClassName = stackTraceElement.getClassName();
            if (!stackClassName.equals(ExternalProcess.class.getName())) {
                String commonClassName = Util.getCommonClassName(stackClassName, Util.mainPackageName);
                return stackClassName.substring(commonClassName.length() + 1)
                        + "."
                        + stackTraceElement.getMethodName();
            }
        }

        // this should never happen
        throw new ExternalProcessException("Unable to determine calling method");
    }

    /**
     * Represents the result of an operation, such as the execution of an external process.
     * Provides information about the operation's exit code, execution duration, associated data,
     * and any errors encountered during execution.
     */
    public static class Result {
        /**
         * Represents the exit code resulting from the execution of an operation, such as an external process.
         * The exit code is a numeric value that typically indicates the success or failure of the operation.
         * A value of zero usually denotes successful execution, while nonzero values indicate errors
         * or specific conditions defined by the operation.
         */
        public final int exitCode;
        /**
         * Represents the duration of an operation in seconds.
         * This value typically measures the elapsed time taken to complete an execution
         * or process, providing a quantitative metric for performance or timing analysis.
         */
        public final double elapsedTimeInSeconds;
        /**
         * Represents a collection of JSON data associated with the result of an operation.
         * This variable is a mapping of string keys to string values, which can be used
         * to store and retrieve specific pieces of information related to the operation.
         * Typical use cases include storing metadata, configuration details, or other
         * contextual data that may be generated during the operation's execution.
         */
        public final Map<String, String> jsonData;
        /**
         * Represents error messages or additional error information associated with an operation.
         * This variable is used to capture details about any issues encountered during the execution
         * of a specific process or task. If no errors are present, it may be null or empty.
         */
        public final String errors;

        /**
         * Constructs a Result instance with the specified details of an operation.
         *
         * @param exitCode the exit code resulting from the operation, typically used to indicate success or failure
         * @param elapsedTimeInSeconds the duration of the operation in seconds, used to measure execution time
         * @param jsonData a map containing JSON data related to the operation, used for metadata or contextual information
         * @param errors the error messages or details encountered during the operation, or null/empty if no errors occurred
         */
        public Result(int exitCode, double elapsedTimeInSeconds, Map<String, String> jsonData, String errors) {
            this.exitCode = exitCode;
            this.elapsedTimeInSeconds = elapsedTimeInSeconds;
            this.jsonData = jsonData;
            this.errors = errors;
        }

        /**
         * Constructs a Result instance by copying details from another Result
         * and overriding the errors field.
         *
         * @param result the original Result instance whose details are to be copied, such as exitCode, elapsedTimeInSeconds, and jsonData
         * @param errors the error messages or details to override in the new instance, replacing the errors field of the original Result
         */
        public Result(Result result, String errors) {
            this(result.exitCode, result.elapsedTimeInSeconds, result.jsonData, errors);
        }
    }
}
