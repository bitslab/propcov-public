package edu.uic.bitslab.propcov.core.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.uic.bitslab.propcov.core.SharedProps.getShared;

/**
 * Represents a collection of configurable properties for a Java package, facilitating property
 * management, lookup, and resolution. Provides mechanisms for combining properties, sorting,
 * and resolving runtime values.
 */
public class JavaProperties implements Comparable<JavaProperties> {
    /**
     * Represents the name of the Java package associated with the configurable properties
     * defined in this instance.
     */
    public String packageName;
    /**
     * Provides a description of the Java package configuration or its properties.
     */
    public String description;
    /**
     * Specifies the order of application for the properties in a collection.
     */
    public byte applyOrder;
    /**
     * Represents a collection of Java Properties that override configured variables.
     */
    public List<Prop> props;
    private final Map<String, Prop> lookupMap = new HashMap<>();
    private String sortOrder = "";

    /**
     * Retrieves a sorted list of Prop objects from the internal lookup map values.
     *
     * @return a sorted list of Prop objects
     */
    public List<Prop> getProps() {
        return lookupMap.values().stream().sorted().collect(Collectors.toList());
    }

    /**
     * Builds or updates the internal data structures based on the available properties.
     * This method checks if the `props` collection is null, empty, or if `lookupMap` is already populated.
     * If any of these conditions are met, the method returns without making changes.
     * For each property in `props`, the method performs the following actions:
     * - If the `propertyKey` of a property is null or empty and the `name` contains a period, it sets `propertyKey`
     *   to the substring of `name` after the last period.
     * - If the `defaultValue` of a property equals the string literal "null", it sets `defaultValue` to null.
     * - Adds the property to the `lookupMap` using its `name` as the key and the property itself as the value.
     */
    public void build() {
        sortOrder = String.format("%02X%s", applyOrder, packageName);

        if (props == null || props.isEmpty()) return;

        props.forEach( p -> {
            if ((p.propertyKey == null || p.propertyKey.isEmpty()) && p.name.contains(".")) {
                p.propertyKey = p.name.substring(p.name.lastIndexOf('.') + 1);
            }

            if (p.defaultValue.equals("null")) p.defaultValue = null;

            lookupMap.put(p.name, p);
        });
    }

    /**
     * Combines the contents of this object with another
     *  object by merging their lookup maps.
     *
     * @param other the object whose lookup map is to be merged
     *              with this instance's lookup map
     * @return the modified instance of this object after combining the lookup maps
     */
    public JavaProperties combine(JavaProperties other) {
        this.lookupMap.putAll(other.lookupMap);
        return this;
    }

    /**
     * Resolves the value of a property by looking it up in the internal property map
     * and optionally using a given default value.
     *
     * @param name the name of the property to resolve. Must exist in the internal lookup map.
     * @param givenValue an optional value to use as a fallback if the system property is not set.
     *                   If null, the predefined default value of the property is used.
     * @return the resolved property value. Returns either the system property value if present,
     *         the givenValue if provided, or the property's predefined default value.
     * @throws RuntimeException if the property with the specified name cannot be found in the internal lookup map.
     */
    public String resolveString(String name, String givenValue) {
        Prop prop = lookupMap.get(name);
        if (prop == null) throw new RuntimeException("Unknown property: " + name);

        String defaultValue = Stream.of(givenValue, getShared(name), prop.defaultValue)
                .filter(Objects::nonNull).findFirst().orElse(null);

        return System.getProperty(name, defaultValue);
    }

    @Override
    public int compareTo(JavaProperties other) {
        return sortOrder.compareTo(other.sortOrder);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JavaProperties && obj.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return lookupMap.hashCode();
    }

    /**
     * This class represents a Java property with metadata for name, key, default value, and description.
     * It implements the {@link Comparable} interface to allow comparison based on the property name.
     */
    public static class Prop implements Comparable<Prop> {
        /**
         * Represents the name of the Java property.
         */
        public String name;
        /**
         * Represents the key associated with a specific property to map to the Extension properties..
         */
        public String propertyKey;
        /**
         * Specifies the default value for the property. This value is used if no other value is provided for the property.
         */
        public String defaultValue;
        /**
         * Represents a textual description of the property, providing additional context or
         * information about its purpose or usage.
         */
        public String description;

        @Override
        public String toString() {
            return name + " : " +  defaultValue + " : " + description;
        }

        @Override
        public int compareTo(Prop o) {
            return this.name.compareTo(o.name);
        }
    }

    @Override
    public String toString() {
        return packageName + ": " + description + "\n" + (props == null ? "" : props.stream().map(Prop::toString).collect(Collectors.joining("\n")));
    }
}
