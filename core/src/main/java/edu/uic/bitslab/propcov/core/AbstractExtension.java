package edu.uic.bitslab.propcov.core;

import edu.uic.bitslab.propcov.core.analyze.Artifact;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;

import static edu.uic.bitslab.propcov.core.Props.resolveString;
import static edu.uic.bitslab.propcov.core.SharedProps.getShared;

/**
 * Abstract class that provides a base framework for handling YAML-based extension configurations.
 * This class is designed to be extended by more specialized extension classes, supplying
 * shared functionalities for managing properties and configurations of an extension.
 * <p>
 * Responsibilities of this class include:
 * - Initializing extension configuration properties based on a specified property prefix.
 * - Resolving and overriding default properties using defined prefix values.
 * - Providing utility methods for common operations, such as preparing objects encapsulating
 *   extension data.
 */
abstract public class AbstractExtension {
    protected Artifact artifact;

    /**
     * Represents an instance of {@link YAMLConfig.Extension} that is associated
     * with the current abstract extension configuration.  This variable provides
     * access to configuration properties and environment settings specific to the
     * extension. It serves as the primary node for managing and interacting with the
     * extension's properties and metadata.
     * Key characteristics include:
     * - Holds the class and configuration details of the extension.
     * - Contains property mappings that may be overridden or resolved dynamically.
     * - Supports environmental settings through a dedicated map.
     */
    public final YAMLConfig.Extension extension;

    /**
     * Constructs an instance of {@code AbstractExtension} and configures its associated
     * {@link YAMLConfig.Extension} instance using the provided property prefix.
     * This constructor filters properties from a central property source that match
     * the specified prefix and recursively resolves any placeholders before populating
     * the extension's property map. It ensures that the extension's properties are
     * initialized with the relevant settings based on the prefix.
     *
     * @param extension the {@link YAMLConfig.Extension} instance to associate with this
     *                  abstract extension. This instance will be updated with resolved
     *                  property values.
     * @param propertyPrefix the prefix used to filter and identify properties relevant
     *                       to this extension. Only properties starting with the given
     *                       prefix will be processed and added to the extension.
     */
    public AbstractExtension(YAMLConfig.Extension extension, String propertyPrefix) {
        Props.getProperties().stream()
            .filter( p -> p.name.startsWith(propertyPrefix) )
            .forEach( (p) ->
                extension.properties.put(
                        p.propertyKey,
                        resolveString(
                                p.name,
                                extension.properties.getOrDefault(p.propertyKey, getShared(p.name))
                        )
                )
            );


        this.extension = extension;
    }

    /**
     * Set the artifact to the provided object, so that it can be
     * accessed within each extension.
     *
     * @param artifact Artifact for current run
     */
    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }
}
