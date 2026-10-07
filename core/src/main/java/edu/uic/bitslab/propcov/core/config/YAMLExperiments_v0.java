package edu.uic.bitslab.propcov.core.config;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * The YAMLExperiments class is used for managing and organizing different YAML experiment configurations.
 * This class contains a version identifier, a preset configuration, and a map of experiment configurations.
 * It is designed to encapsulate the properties and setup required for managing YAML-driven experiments.
 */
public class YAMLExperiments_v0 {
    /**
     * Represents the version identifier for the {@code YAMLExperiments} class.
     * This variable indicates the current version of the YAML experiment configurations.
     * It can be used to track updates or changes to the experiment setup.
     */
    public int version;
    /**
     * Represents the default YAML experiment configuration used within the scope of YAML experiments.
     * This variable provides a standardized, reusable configuration setup that can be applied to new
     * or existing experiments as a starting point.
     * The configuration includes key properties such as the list of projects, number of trials,
     * number of iterations, and an optional override setup for fine-grained control.
     * It allows for consistent and centralized management of experiment presets.
     */
    public YAMLExperimentConfig_v0 preset;
    /**
     * A map containing YAML experiment configurations, where the key is the name of the experiment
     * as a String and the value is the corresponding {@code YAMLExperimentConfig} object.
     * This map is used to manage and store multiple experiment configurations within the context
     * of the {@code YAMLExperiments} class. Each experiment configuration specifies associated
     * projects, trials, iterations, and optional overriding configurations.
     */
    public Map<String, YAMLExperimentConfig_v0> experiments;

    /**
     * Represents the configuration for a single YAML-based experiment.
     * This class encapsulates key properties required for defining and managing
     * an individual experiment, including the associated projects, number of trials,
     * number of iterations, and an optional override configuration.
     */
    public static class YAMLExperimentConfig_v0 {
        /**
         * Version number for this particular yaml config.
         */
        public int version;

        /**
         * An array of project names or identifiers associated with the experiment.
         * This variable is used to specify the projects that the experiment is related to or applies to.
         * Each element in the array corresponds to a specific project.
         */
        public String[] projects;
        /**
         * Specifies the number of trials configured for the experiment.
         * This variable represents the total count of trial runs to be executed
         * within the context of a YAML-based experiment configuration. It is
         * intended to define the number of independent experimentation rounds.
         */
        public Long trials;
        /**
         * Represents the number of iterations configured for the experiment.
         * This value determines how many times the experiment should be executed
         * or repeated for each trial.
         */
        public Long iterations;
        /**
         * Specifies an optional override configuration for this YAML-based experiment.
         * This configuration can be used to redefine or supplement specific settings
         * within the experiment, allowing for customization or refinement of its behavior.
         * It is represented by the {@link YAMLConfig} class, which contains detailed
         * settings for versioning, project-specific properties, and various extensions.
         */
        public YAMLConfig_v0 overrideConfig;
    }

    /**
     * Loads a YAML configuration file and parses it into a {@code YAMLConfig} object.
     * The method verifies that the YAML file matches the expected version before loading.
     *
     * @param configFile the path to the YAML configuration file to be loaded.
     * @return a {@code YAMLConfig} object parsed from the provided file.
     * @throws IOException if an error occurs while accessing the file.
     * @throws RuntimeException if the YAML file version does not match the expected version.
     */
    public static YAMLExperiments_v0 loadFromFile(String configFile) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile))) {
            Yaml yaml = new Yaml();
            return yaml.loadAs(reader, YAMLExperiments_v0.class);
        }
    }
}