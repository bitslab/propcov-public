package edu.uic.bitslab.propcov.extensions.buildsystem;

import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.buildsystem.AbstractBuildSystem;
import edu.uic.bitslab.propcov.core.buildsystem.BuildSystemException;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcess;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcess.Result;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcessException;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static edu.uic.bitslab.propcov.core.config.Config.TimeoutType.*;

/**
 * Represents the implementation of the Maven build system used for project building,
 * cleaning, and property-based testing. This class extends the functionality of
 * the abstract build system by providing Maven-specific implementation details
 * for these operations.
 */
public class Gradle extends AbstractBuildSystem {
    private static final Logger LOGGER = Util.getLogger(Gradle.class);

    /**
     * Represents Maven-specific command-line options for customizing
     * the behavior of Maven operations. This variable typically holds
     * additional arguments or flags to be passed to Maven during
     * build system executions, such as custom goals, profiles, or
     * other configuration settings.
     */
    public final String gradleOptions;

    /**
     * Constructs an instance of the Maven build system configuration.
     *
     * @param extension the extension configuration containing build-specific properties and environment variables
     * @param timeouts a map defining timeout values associated with different timeout types
     * @param projectName the name of the main project
     * @param subProjectName the name of the subproject within the main project
     */
    public Gradle(YAMLConfig.Extension extension, Map<Config.TimeoutType, Long> timeouts, String projectName, String subProjectName) {
        super(extension, timeouts, projectName, subProjectName, "PropCov.BuildSystem.Gradle.");
        String foundGradleOptions = extension.properties.getOrDefault("Options", "");
        gradleOptions = foundGradleOptions == null ? "" : foundGradleOptions;
    }

    @Override
    public void clean() throws TimerException, IOException, ExternalProcessException, InterruptedException {
        LOGGER.info("-------Cleaning target---------");
        (new ExternalProcess(timeouts.getOrDefault(cleanSUT, Long.MAX_VALUE / 1000000000)))
                .command("./gradlew clean")
                .workingDirectory(localDirectory)
                .environment(env)
                .run();
    }

    @Override
    public void build() throws TimerException, IOException, ExternalProcessException, InterruptedException, BuildSystemException {
        LOGGER.info("-------building target---------");

        Result result = (new ExternalProcess(timeouts.getOrDefault(buildSUT, Long.MAX_VALUE / 1000000000)))
                .command("./gradlew assemble testClasses")
                .workingDirectory(localDirectory)
                .environment(env)
                .run();

        if (result.exitCode != 0) {
            LOGGER.error("BUILD FAILED");
            throw new BuildSystemException("Build Failed");
        }
    }

    /**
     * @param propertyTest PropertyTest that is being reviewed/ran
     * @return Path including subProject if needed
     */
    @Override
    public Path getFullTargetPath(PropertyTest propertyTest) {
        return propertyTest == null || propertyTest.subProject == null
                ? Path.of(extension.getProperty("TargetPath", this.localDirectory.resolve("build").toString()))
                : Path.of(extension.getProperty("TargetPath", this.localDirectory.resolve(propertyTest.subProject).resolve("target").toString()));
    }

    @Override
    public Result testProperty(Config config, PropertyTest propertyTest) throws IOException, TimerException, ExternalProcessException, InterruptedException {
        LOGGER.info("----------PROPERTY------------");
        String entryPoint = propertyTest.entryPoint;
        String propertyName = entryPoint.substring(0, entryPoint.lastIndexOf(".")) + "." + entryPoint.substring(entryPoint.lastIndexOf(".") + 1, entryPoint.indexOf("("));
        LOGGER.info(propertyName);

        // reset coverage
        config.coverage.reset(config.buildSystem.getFullTargetPath(propertyTest));

        Result result = (new ExternalProcess(timeouts.getOrDefault(testPropertySUT, Long.MAX_VALUE / 1000000000)))
                .command("./gradlew test " + gradleOptions + " --tests " + propertyName)
                .workingDirectory(localDirectory)
                .environment(env)
                .run();

        // no error, so return result
        return result;
    }
}
