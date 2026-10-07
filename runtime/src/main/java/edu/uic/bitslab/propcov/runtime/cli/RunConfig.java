package edu.uic.bitslab.propcov.runtime.cli;

import edu.uic.bitslab.propcov.core.analysisframework.AnalysisFrameworkException;
import edu.uic.bitslab.propcov.core.buildsystem.BuildSystemException;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcessException;
import edu.uic.bitslab.propcov.core.cli.AbstractCLI;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.Run;
import edu.uic.bitslab.propcov.core.config.ConfigException;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.coverage.CoverageException;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;

import java.io.IOException;


/**
 * This class provides a command-line interface for configuring and executing a project using predefined YAML configurations.
 * It extends the AbstractCLI class and provides the functionality to validate input arguments and process configurations.
 */
public class RunConfig extends AbstractCLI {
    private final static int ARG_INDEX_PROJECT_NAME = 1;

    protected void validate(String[] args) throws IllegalArgumentException {
        if (args.length == 2 && !args[ARG_INDEX_PROJECT_NAME].isEmpty()) return;
        throw new IllegalArgumentException("Argument projectName is missing or invalid.");
    }

    @Override
    public void process(String[] args) throws TimerException, BuildSystemException, IOException, ExternalProcessException, SourceException, InterruptedException, ClassNotFoundException, IllegalAccessException, AnalysisFrameworkException, CoverageException, ConfigException {
        validate(args);

        String projectName = args[ARG_INDEX_PROJECT_NAME];

        Config config = ConfigFromYAML(projectName);
        Run.process(config);
    }

    /**
     * Generates a {@link Config} object from the YAML configuration file for the specified project.
     *
     * @param projectName The name of the project for which the configuration is to be generated.
     *                     It must be a valid project name corresponding to a predefined YAML project configuration.
     * @return A {@link Config} object constructed based on the provided YAML configuration.
     * @throws IOException If an error occurs during reading the YAML configuration file.
     * @throws SourceException If the source configuration within the YAML file is invalid or missing.
     * @throws ConfigException When configuration class could not load object.
     */
    public static Config ConfigFromYAML(String projectName) throws IOException, SourceException, ConfigException {
        YAMLConfig yamlConfig = YAMLConfig.loadProjectYamlConfig(projectName);
        return BuilderFromYAMLConfig(projectName, yamlConfig).build(yamlConfig);
    }

    /**
     * Constructs a {@link Config.Builder} object using the provided project name and YAML configuration.
     *
     * @param projectName The name of the project for which the builder is to be initialized. It must match the project
     *                    name of a predefined YAML configuration.
     * @param yamlConfig  The YAML configuration for the project, which includes settings for build system, coverage,
     *                    analysis framework, and other properties required to configure the builder.
     * @return A {@link Config.Builder} object configured based on the provided YAML configuration.
     * @throws SourceException If the source configuration within the YAML file is invalid or missing.
     * @throws ConfigException When configuration could not load object.
     */
    public static Config.Builder BuilderFromYAMLConfig(String projectName, YAMLConfig yamlConfig) throws SourceException, ConfigException {
        // no buildSystem defined, so define empty BuildSystem with a default type of MAVEN
        if (yamlConfig.buildSystem == null) yamlConfig.buildSystem = new YAMLConfig.Extension();
        if (yamlConfig.buildSystem.extensionClass == null) yamlConfig.buildSystem.extensionClass = "edu.uic.bitslab.propcov.extensions.buildsystem.Maven";

        Config.Builder builder = new Config.Builder(projectName);

        if (yamlConfig.source == null) {
            throw new SourceException("No source defined for project.");
        }

        return builder
            .subProject(yamlConfig.subProject)
            .source(yamlConfig.source)
            .addTimeouts(yamlConfig.timeouts)
            .buildSystem(yamlConfig.buildSystem, builder.getTimeouts())
            .coverage(yamlConfig.coverage)
            .analysisFramework(yamlConfig.analysisFramework)
            .testFramework(yamlConfig.testFramework)
            .addMainJars(yamlConfig.mainJar)
            .addMainJarsWithDependencies(yamlConfig.mainJarWithDependencies)
            .addTestJars(yamlConfig.testJar)
            .patchFile(yamlConfig.patchName)
            .addProperties(yamlConfig.properties)
            .outputPath("output")
            .artifactDirectory("artifacts")
            .addPackages(yamlConfig.packageNames);
    }

}
