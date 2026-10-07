package edu.uic.bitslab.propcov.core.config;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a configuration model for YAML-based configuration files.
 * This class is designed to store metadata and configuration details
 * required for build systems, projects, and analysis settings.
 */
public class YAMLConfig_v0 {
    /**
     * Represents the SUT project name.
     */
    @YamlOutput(order=1)
    public String name;

    /**
     * Represents the name of a subproject within a larger project.
     */
    @YamlOutput(order=2)
    public String subProject;

    /**
     * Represents the URL associated with the project or configuration. This URL
     * typically points to the location of the project repository, API endpoint,
     * or any resource required for the configuration.
     */
    @YamlOutput(order=3)
    public String URL;

    /**
     * Represents the unique identifier for a specific checkout or version of
     * the project in the YAML configuration. This identifier could refer to a
     * commit hash, tag, or branch name, depending on the version control system
     * being used.
     */
    @YamlOutput(order=4)
    public String checkoutID;

    /**
     * Represents the file name of the patch being applied, configured, or managed
     * within the YAML configuration.
     */
    @YamlOutput(order=5)
    public String patchName;

    /**
     * Represents the name of the main JAR file to be used in the configuration.  The
     * value of this field typically indicates the primary executable JAR of
     * the project being configured.
     */
    @YamlOutput(order=6)
    public String mainJar;

    /**
     * Represents the path or identifier to the main JAR file, including its dependencies,
     * for the context of the configuration. This field is typically used to specify
     * a bundled JAR file that includes all the required dependencies to run the application.
     */
    @YamlOutput(order=7)
    public String mainJarWithDependencies;

    /**
     * Represents the file path or identifier for the test JAR associated with the configuration.
     * This field is annotated with {@link YamlOutput} to specify its ordering when serialized
     * to a YAML format, ensuring that it appears at the specified position.
     */
    @YamlOutput(order=8)
    public String testJar;

    /**
     * Represents the build system configuration used in the YAML configuration.
     * This field is an instance of the {@code BuildSystem} class, which holds
     * details about the specific build system (e.g., Maven or Gradle), its
     * target path, environment variables, and additional build options. The
     * ordering of this field in YAML serialization is defined by a value of 9
     * in the {@code YamlOutput} annotation.
     */
    @YamlOutput(order=9)
    public BuildSystem buildSystem;

    /**
     * Specifies the test framework used in the project or build system as part of the configuration.
     * This field holds the name or identifier of the test framework, such as JUnit, TestNG, or another
     * framework. It is serialized to YAML in a specific order defined by the {@code @YamlOutput(order = 10)}
     * annotation.
     */
    @YamlOutput(order=10)
    public String testFramework;

    /**
     * Specifies the type of logger used by OPAL (Object-oriented Programming
     * Analysis Laboratory) for logging operations. This field determines
     * the logging mechanism or library to be utilized during analysis,
     * debugging, or related tasks.
     */
    @YamlOutput(order=11)
    public String opalLoggerType;

    /**
     * Specifies the type of analysis to be performed in the context of the YAML configuration.
     * This field is serialized to YAML using the order defined by the {@code @YamlOutput} annotation.
     * It helps determine the specific analysis category or process that is applied during execution.
     */
    @YamlOutput(order=12)
    public String analysisType;

    /**
     * Represents a set of flags or options used to customize or control the analysis process
     * performed within the configuration. Each flag is represented as a string.
     */
    @YamlOutput(order=13)
    public Set<String> analysisFlags;

    /**
     * Represents a list of package names associated with the configuration.
     * These package names are typically used for analysis, filtering, or
     * categorization purposes within the context of the configuration.
     */
    @YamlOutput(order=14)
    public List<String> packageNames;

    /**
     * A map of timeout configurations, where keys represent different types of timeouts
     * defined by {@link Config.TimeoutType}, and values specify the timeout duration in seconds.
     * This field is used to configure and serialize timeout parameters as part of the YAML configuration.
     */
    @YamlOutput(order=15)
    public Map<Config.TimeoutType, Long> timeouts;

    /**
     * A list representing property configurations used in the YAML configuration.
     * Each item in the list is of type {@code PropertyTest}, which represents
     * an individual property configuration with specific attributes.
     */
    @YamlOutput(order=16)
    public List<PropertyTest> properties;

    /**
     * Represents a build system configuration used within the YAMLConfig_v0 class.
     * This class holds configuration details such as the type of build system,
     * the target build path, and customizable environment and build options.
     */
    public static class BuildSystem {
        /**
         * Specifies the type of the build system being used.
         * This field holds a string value that identifies the build system,
         * such as Maven, Gradle, or other supported build systems.
         */
        public String type;
        /**
         * Represents the target directory or location for the build system.
         * This path is used by the build system to specify where the build
         * files or artifacts should be generated or located.
         */
        public Path target;
        /**
         * A map representing environment variables specific to the build system.
         * Each key-value pair in this map defines the name and value of an environment
         * variable that can be used during the build process.
         */
        @SuppressWarnings("CanBeFinal")
        public Map<String, String> env = new HashMap<>();
        /**
         * A map that holds additional build options or settings for the build system.
         * The keys represent the option names, and the values represent associated settings.
         * This allows for customization and flexibility when configuring the build system.
         */
        @SuppressWarnings("CanBeFinal")
        public Map<String, String> opt = new HashMap<>();
    }

    /**
     * Loads a YAML configuration file and deserializes it into a {@code YAMLConfig_v0} object.
     *
     * @param configFile The path to the YAML configuration file.
     * @return A {@code YAMLConfig_v0} object containing the configuration data.
     * @throws IOException If an I/O error occurs while reading the configuration file.
     */
    public static YAMLConfig_v0 loadFromFile(String configFile) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile))) {
            Yaml yaml = new Yaml();
            return yaml.loadAs(reader, YAMLConfig_v0.class);
        }
    }
}