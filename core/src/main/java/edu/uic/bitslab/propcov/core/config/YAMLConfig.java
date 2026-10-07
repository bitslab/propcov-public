package edu.uic.bitslab.propcov.core.config;

import edu.uic.bitslab.propcov.core.SharedProps;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a configuration that can be serialized/deserialized to and from YAML format.
 * This class is primarily used for managing project-specific settings and metadata
 * required for various tools, frameworks, or build systems.
 * It supports a structured and annotated approach using the {@code @YamlOutput} annotation
 * to control field order during serialization.
 */
public class YAMLConfig {
    /**
     * The version field represents the YAML configuration version.
     */
    @YamlOutput(order=0)
    public int version;

    /**
     * Represents the name of the project or configuration.
     */
    @YamlOutput(order=1)
    public String name;

    /**
     * Represents the name or identifier of a sub-project within a larger project context.
     */
    @YamlOutput(order=2)
    public String subProject;

    /**
     * Represents the source configuration for an extension in the YAML configuration.
     * The `source` field is annotated with {@link YamlOutput}, indicating that it is part
     * of the YAML serialized output with a defined ordering. This field is of type
     * {@link Extension}, which encapsulates additional properties, environmental variables,
     * and details about the extension class.
     */
    @YamlOutput(order=3)
    public Extension source;

    /**
     * Represents the filename of a patch related to the associated project configuration.
     */
    @YamlOutput(order=7)
    public String patchName;

    /**
     * Represents the name of the main JAR file associated with the project.
     */
    @YamlOutput(order=8)
    public ListOrString mainJar;

    /**
     * Defines the file path for the main JAR file of the project, which includes all
     * dependencies bundled together.
     */
    @YamlOutput(order=9)
    public ListOrString mainJarWithDependencies;

    /**
     * Specifies the path or identifier of the test JAR file associated with the project.
     */
    @YamlOutput(order=10)
    public ListOrString testJar;

    /**
     * Represents the build system extension configuration for a project.
     */
    @YamlOutput(order=11)
    public Extension buildSystem;

    /**
     * Represents the test framework configuration for a project.
     */
    @YamlOutput(order=12)
    public Extension testFramework;

    /**
     * Represents the analysis framework configuration for the project.
     */
    @YamlOutput(order=13)
    public Extension analysisFramework;

    /**
     * A list of package names representing the packages included in the
     * configuration.
     */
    @YamlOutput(order=16)
    public List<String> packageNames;

    /**
     * A map representing timeout configurations where each key corresponds to a specific timeout type
     * and the associated value represents the duration in seconds.
     */
    @YamlOutput(order=17)
    public Map<Config.TimeoutType, Long> timeouts;

    /**
     * Represents coverage configuration for the YAML output.
     */
    @YamlOutput(order=18)
    public Extension coverage;

    /**
     * Represents a collection of property tests to be serialized in YAML format.
     */
    @YamlOutput(order=20)
    public List<PropertyTest> properties;

    /**
     * ListOrString supports converting a string to a list.
     */
    public static class ListOrString extends ArrayList<String> implements List<String> {
        /**
         * Create a new instance with all elements of the given list added
         * @param list Populate with the given list
         */
        public ListOrString(List<String> list) {
            super();
            this.addAll(list);
        }

        /**
         * Create a new instance with one string element
         * @param value Add string value to the list
         */
        public ListOrString(String value) {
            super();
            this.add(value);
        }

        /**
         * Create a new instance with no elements
         * Used by SnakeYaml dynamically (suppress unused warning)
         */
        @SuppressWarnings("unused")
        public ListOrString() {
            super();
        }
    }

    /**
     * Represents an extension configuration containing the extension class name and its associated properties and environment variables.
     * This class is used to define extensions in the overall configuration system.
     */
    public static class Extension {
        /**
         * Specifies the fully qualified class name of the extension.
         * This is used to identify and load the corresponding extension implementation.
         */
        public String extensionClass;
        /**
         * A map representing the properties defined for the extension.
         * Each key-value pair in this map corresponds to a specific property name and its respective value.
         * These properties are used to configure the behavior of the associated extension.
         */
        public final Map<String, String> properties = new HashMap<>();
        /**
         * A map representing environment variables associated with the extension.
         * The keys in the map are the names of the environment variables, and the values
         * are the corresponding values for those variables. This map provides a way to
         * configure environment-specific settings for the extension.
         */
        public final Map<String, String> env = new HashMap<>();

        /**
         * Default constructor for the Extension class.
         * Initializes a new instance of the Extension configuration with no predefined class name,
         * properties, or environment variables.
         */
        public Extension() {
        }

        /**
         * Constructs an instance of the Extension class with the specified class name, properties, and environment variables.
         *
         * @param extensionClass the fully qualified class name of the extension. This is used to identify and load the corresponding extension implementation.
         * @param properties a map of key-value pairs representing configuration properties for the extension. Each entry defines a specific property name and its respective value
         * .
         * @param env a map of key-value pairs representing environment variables associated with the extension. Each entry defines the name of the environment variable and its corresponding
         *  value.
         */
        public Extension(String extensionClass, Map<String, String> properties, Map<String, String> env) {
            this.extensionClass = extensionClass;
            this.properties.putAll(properties);
            this.env.putAll(env);
        }

        /**
         * Since properties uses null to indicate to ignore the property,
         * when the property map does not contain the key or when the
         * found value is null, then the given should be used.
         *
         * @param key Key to search for
         * @param defaultValue default value to use if not found
         * @return string value found or default
         */
        public String getProperty(String key, String defaultValue) {
            String value = properties.get(key);
            String rtn = (value == null ? defaultValue : value);
            SharedProps.setShared(key, rtn);
            return rtn;
        }
    }

    static int getYamlVersion(String configFile) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile))) {
            Yaml yaml = new Yaml();
            HashMap<String, Object> hashMap = yaml.load(reader);
            return (int) hashMap.getOrDefault("version", 0);
        }
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
    public static YAMLConfig loadFromFile(String configFile) throws IOException {
        final int CURRENT_VERSION = 1;

        int yamlVersion = getYamlVersion(configFile);
        if (yamlVersion != CURRENT_VERSION) {
            throw new RuntimeException("Provided configfile " + configFile + " with version " + yamlVersion + " does not match expected version " + CURRENT_VERSION);
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile))) {
            Yaml yaml = new Yaml();
            return yaml.loadAs(reader, YAMLConfig.class);
        }
    }

    /**
     * Loads the YAML configuration for the specified project.
     * This method constructs the path to the YAML file based on the project's name
     * and delegates the file loading and parsing to {@code loadFromFile}.
     *
     * @param projectName the name of the project for which the YAML configuration is to be loaded.
     * @return a {@code YAMLConfig} object containing the parsed configuration for the specified project.
     * @throws IOException if an error occurs while accessing or reading the YAML configuration file.
     */
    public static YAMLConfig loadProjectYamlConfig(String projectName) throws IOException {
        String BaseConfigDir = "artifacts/configs/%s/%s.yaml";
        String configFile = String.format(BaseConfigDir, projectName, projectName);
        return loadFromFile(configFile);
    }
}