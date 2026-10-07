package edu.uic.bitslab.propcov.core;

import edu.uic.bitslab.propcov.core.config.JavaProperties;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Static functions related to seamlessly use Java Properties overrides in context
 * of PropCov configurations.
 */
public class Props {
    protected static final SortedSet<JavaProperties> allProperties = new TreeSet<>();
    private static JavaProperties properties = new JavaProperties();

    /**
     * Retrieves a list of properties from the internal properties store.
     *
     * @return a list of JavaProperties.Prop objects representing the properties.
     */
    public static List<JavaProperties.Prop> getProperties() {
        return properties.getProps();
    }

    /**
     * Retrieves a list of distinct package names from the loaded properties.
     *
     * @return a list of strings representing the loaded package names.
     */
    public static List<String> getLoadedExtensions() {
        return allProperties.stream()
                .map(o -> o.packageName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Ingest given YAML file and add to set of available properties.
     * @param definedPropertyFilename Name of resource containing the property yaml file.
     */
    public static void readProperties(String definedPropertyFilename) {
        try (InputStream inputStream = Props.class.getClassLoader().getResourceAsStream(definedPropertyFilename)) {
            Yaml yaml = new Yaml();
            JavaProperties javaProperties = yaml.loadAs(inputStream, JavaProperties.class);
            javaProperties.build();
            allProperties.add(javaProperties);
            setActive();

        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void setActive() {
        for (JavaProperties javaProperties : allProperties) {
            properties = properties.combine(javaProperties);
        }
    }

    /**
     * Retrieve value from property, default, or given value.
     *
     * @param name Property Name
     * @param givenValue Given Value to use if not overridden by Property.
     * @return String value
     */
    public static String resolveString(String name, String givenValue) {
        return properties.resolveString(name, givenValue);
    }

    public static String resolveString(String name, String givenValue, Boolean missingUseDefault) {
        if (missingUseDefault && !hasProp(name)) {
            return givenValue;
        }

        return resolveString(name, givenValue);
    }

    /**
     * Retrieve value from property or default value.
     *
     * @param name Property Name
     * @return String value
     */
    public static String resolveString(String name) {
        return resolveString(name, null);
    }

    /**
     * Retrieve value from property, default, or given value.
     *
     * @param name Property Name
     * @param givenValue Given Value to use if not overridden by Property.
     * @return Path value from String
     */
    public static Path resolvePath(String name, String givenValue) {
        return Path.of(resolveString(name, givenValue));
    }

    /**
     * Retrieve value from property or default value.
     *
     * @param name Property Name
     * @return Path value from String
     */
    public static Path resolvePath(String name) {
        return resolvePath(name, null);
    }

    /**
     * Retrieve value from property, default, or given value using a separate to build a Collection.
     *
     * @param name Property Name
     * @param givenValue Given Value to use if not overridden by Property.
     * @param separator Value used to split the string into elements.
     * @return List of Trimmed String values
     */
    public static Collection<String> resolveStringList(String name, String givenValue, String separator) {
        String value = resolveString(name, givenValue);
        if (value == null) return null;
        return List.of(Arrays.stream(value.split(separator)).map(String::trim).toArray(String[]::new));
    }

    /**
     * Retrieve value from property, default, or given value.
     *
     * @param name Property Name
     * @param givenValue Given Value to use if not overridden by Property.
     * @return Parsed Boolean value
     */
    public static Boolean resolveBoolean(String name, String givenValue) {
        String value = resolveString(name, givenValue);
        if (value == null) return null;
        return Boolean.parseBoolean(value);
    }

    /**
     * Retrieve value from property or default value.
     *
     * @param name Property Name
     * @return Parsed Boolean value
     */
    public static Boolean resolveBoolean(String name) {
        return resolveBoolean(name, null);
    }

    /**
     * Retrieve value from property, default, or given value.
     *
     * @param name Property Name
     * @param givenValue Given Value to use if not overridden by Property.
     * @return Parsed Long value
     */
    public static Long resolveLong(String name, String givenValue) {
        String value = resolveString(name, givenValue);
        if (value == null) return null;
        return Long.parseLong(resolveString(name, value));
    }

    /**
     * Retrieve value from property or default.
     *
     * @param name Property Name
     * @return Parsed Long value
     */
    public static Long resolveLong(String name) {
        return resolveLong(name, null);
    }

    private static boolean hasProp(String name) {
        return properties.getProps().stream().anyMatch(p -> p.name.equals(name));
    }
}
