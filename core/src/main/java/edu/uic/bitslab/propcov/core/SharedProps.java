package edu.uic.bitslab.propcov.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Singleton for storing properties that can be shared with more than one extension.
 */
public class SharedProps {
    private static final SharedProps instance = new SharedProps();
    private final Map<String, String> sharedProps = new HashMap<>();
    private Map<String, Set<String>> configuredAliases;

    private SharedProps() {
        configuredAliases = Map.of(
                "PropCov.LocalDirectory", Set.of("PropCov.Source.Local.Directory", "PropCov.BuildSystem.Maven.LocalDirectory"),
                "PropCov.TargetPath", Set.of("PropCov.BuildSystem.Maven.TargetPath")
        );

        configuredAliases.forEach((actual, aliases) -> {
            String actualProperty = System.getProperty(actual);

            if (actualProperty != null) {
                sharedProps.put(actual, actualProperty);
                aliases.forEach(alias -> sharedProps.put(alias, actualProperty));
            }
        });
    }

    /**
     * Get shared property
     *
     * @param name property name to search for
     * @return String for found value, otherwise null
     */
    public static String getShared(String name) {
        if (name == null) return null;
        return instance.sharedProps.get(name);
    }

    /**
     * Add / Update Shared Prop Item
     * @param name Key
     * @param value Value
     */
    public static void setShared(String name, String value) {
        if (name == null || value == null) return;
        String propName = name.startsWith("PropCov.") ? name : "PropCov." + name;
        if (instance.configuredAliases.get(propName) == null) return;
        instance.sharedProps.put(propName, value);
    }

    /**
     * Tickle SharedProps to ensure it gets setup early in the process.
     */
    public static void init() {
        if (instance != null) return;
        throw new IllegalStateException("Shared properties did not get initialized.");
    }
}
