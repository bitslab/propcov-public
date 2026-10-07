package edu.uic.bitslab.propcov.core.buildsystem;

import edu.uic.bitslab.propcov.core.AbstractExtension;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * The AbstractBuildSystem class provides a base abstraction for implementing build system-specific
 * functionalities such as cleaning, building, and executing tests. It extends {@link AbstractExtension}
 * and encapsulates configuration details and operations related to a build system.
 * <p>
 * Subclasses of this abstract class are required to implement methods for cleaning, building, and
 * testing properties. Additionally, they may define specific behaviors suitable for the particular
 * build system they represent.
 * <p>
 * This class handles environment configuration, file path management, and timeout settings that are
 * common to various build system implementations.
 */
public abstract class AbstractBuildSystem extends AbstractExtension {
    private static final String PROPCOV_BUILD_ENV_PREFIX = "PropCov.Build.Env.";

    /**
     * Represents the local directory path associated with the build system.
     * This directory serves as the working space for various build-related operations.
     * The exact usage and context of this path depend on the specific implementation
     * of the build system subclasses.
     */
    public final Path localDirectory;
    /**
     * Represents the environment variables used by the build system.
     * This map contains key-value pairs where the key is the name of the environment variable
     * and the value is its corresponding value. The variables in this map are used
     * during the execution of various build system operations.
     */
    public final Map<String, String> env;
    /**
     * Represents the type of build system being used.
     * This variable is intended to store a string that identifies the specific
     * build system implementation (e.g., "Maven", "Gradle", "Ant") or its configuration.
     */
    public final String buildSystemType;
    /**
     * A mapping of timeout configurations to their respective durations. The keys
     * represent different timeout types specified in {@link Config.TimeoutType},
     * while the values define the timeout duration in second for each type.
     * This map allows for fine-grained control over the timing behavior of various
     * operations in the build system.
     */
    public final Map<Config.TimeoutType, Long> timeouts;

    /**
     * Performs a clean operation specific to the build system implementation. The
     * clean operation prepares the build environment by removing any artifacts
     * or intermediate files generated in previous build cycles. This ensures that
     * further builds are performed in a clean state, free of any potentially
     * inconsistent or outdated data.
     *
     * @throws TimerException if the cleanup operation is interrupted due to timing constraints.
     * @throws IOException if an I/O error occurs during the clean operation.
     * @throws ExternalProcessException if an external process invoked during the clean operation fails.
     * @throws InterruptedException if the thread executing the clean operation is interrupted.
     */
    public abstract void clean() throws TimerException, IOException, ExternalProcessException, InterruptedException;
    /**
     * Executes the build process defined by the specific implementation of the build system.
     * This method should be implemented by subclasses to define the precise logic for building
     * the project or artifact associated with the build system.  The build process typically
     * involves compiling code, linking dependencies, and generating outputs such as binaries,
     * libraries, or packaged distributions. As this method interacts with external processes
     * and resources, exceptions may be thrown to indicate issues encountered during the build process.
     *
     * @throws BuildSystemException if an error occurs that is specific to the build system, such as
     *                              configuration issues or missing dependencies.
     * @throws TimerException if the build operation exceeds a predefined time limit or encounters
     *                        timing-related constraints.
     * @throws IOException if an I/O error occurs during the build operation, such as file access issues.
     * @throws ExternalProcessException if a failure occurs while invoking or managing external processes
     *                                   involved in the build.
     * @throws InterruptedException if the thread executing the build is interrupted during its operation.
     */
    public abstract void build() throws BuildSystemException, TimerException, IOException, ExternalProcessException, InterruptedException;
    /**
     * Tests a property using the provided configuration and property test.
     *
     * @param config the configuration used for the property test
     * @param propertyTest the property test to be executed
     * @return the result of the external process execution containing details such as exit code, elapsed time, and additional data
     * @throws IOException if an I/O error occurs during execution
     * @throws TimerException if a timing issue occurs during execution
     * @throws ExternalProcessException if an error occurs during the external process execution
     * @throws InterruptedException if the execution is interrupted
     */
    public abstract ExternalProcess.Result testProperty(Config config, PropertyTest propertyTest) throws IOException, TimerException, ExternalProcessException, InterruptedException;

    protected AbstractBuildSystem(@SuppressWarnings("SameParameterValue") YAMLConfig.Extension extension, Map<Config.TimeoutType, Long> timeouts, String projectName, String subProjectName, String propertyPrefix) {
        super(extension, propertyPrefix);

        overrideBySystemProperty(extension.env, PROPCOV_BUILD_ENV_PREFIX);

        this.localDirectory = Path.of(extension.getProperty("LocalDirectory", "../propcov-sut/" + projectName + (subProjectName == null ? "" : "/"+subProjectName) ));
        this.env = Collections.unmodifiableMap(extension.env);
        this.buildSystemType = extension.extensionClass;
        this.timeouts = timeouts;
    }

    /**
     * @param propertyTest PropertyTest that is being reviewed/ran
     * @return Path including subProject if needed
     */
    public Path getFullTargetPath(PropertyTest propertyTest) {
        return propertyTest == null || propertyTest.subProject == null
            ? Path.of(extension.getProperty("TargetPath", this.localDirectory.resolve("target").toString()))
            : Path.of(extension.getProperty("TargetPath", this.localDirectory.resolve(propertyTest.subProject).resolve("target").toString()));
    }

    /**
     * Overrides entries in the provided map based on system properties that match specified prefixes.
     * For each system property whose key starts with one of the provided prefixes,
     * the method extracts the suffix of the key (after the prefix) and adds a corresponding entry
     * to the map, where the suffix represents the key in the map and the value is the system property value.
     *
     * @param in the map to be updated with the overridden entries based on system properties
     * @param prefixes the array of prefixes to determine which system properties will override the map entries
     */
    public static void overrideBySystemProperty(Map<String, String> in, String ...prefixes) {
        System.getProperties().forEach( (p, v) -> {
            String property = p.toString();

            for (String prefix : prefixes) {
                if (property.startsWith(prefix)) {
                    String envName = property.substring(prefix.length() + 1);
                    String envValue = v.toString();
                    in.put(envName, envValue);
                    break;
                }
            }
        });
    }
}