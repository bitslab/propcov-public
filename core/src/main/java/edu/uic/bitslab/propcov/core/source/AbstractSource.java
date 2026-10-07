package edu.uic.bitslab.propcov.core.source;

import edu.uic.bitslab.propcov.core.AbstractExtension;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Abstract base class for defining sources used in a project.
 * This class provides a framework for initializing and managing project source configurations
 * via YAML configurations.
 */
abstract public class AbstractSource extends AbstractExtension {
    /**
     * Represents the path to a local directory associated with a specific source configuration.
     * This directory is typically used to store or retrieve files and resources pertaining to
     * the source's functionality within a project. The location of the directory is initialized
     * based on certain properties resolved from an associated {@link YAMLConfig.Extension}.
     */
    public final Path localDirectory;

    /**
     * Retrieves or downloads a source resource managed by the implementing class.
     * This method must be implemented by subclasses to define the behavior of how
     * the source resource is obtained, such as downloading, fetching, or initializing
     * the content from a specific location.
     *
     * @throws SourceException if an error occurs while retrieving the source resource.
     */
    abstract public void get() throws SourceException;

    /**
     * Constructs an AbstractSource instance based on the provided parameters.
     * Initializes the local directory path for the source by resolving it from
     * the extension's properties, using the "LocalDirectory" property key.
     * If the "LocalDirectory" property is not already defined, it is set to a default
     * value derived from the project name.
     *
     * @param extension the extension configuration containing properties and environment variables
     *                  specific to the source. It is used to retrieve or define the "LocalDirectory" property.
     * @param projectName the name of the project. This name is used to construct the default path
     *                    for the "LocalDirectory" property if it is not already set in the extension.
     * @param propertyPrefix a prefix string used to configure the superclass with property settings
     *                       specific to this source instance.
     */
    protected AbstractSource(YAMLConfig.Extension extension, String projectName, String propertyPrefix) {
        super(extension, propertyPrefix);
        extension.properties.putIfAbsent("Directory", "../propcov-sut/"+projectName);
        this.localDirectory = Path.of(extension.properties.get("Directory"));
    }

    /**
     * Removes the local directory associated with this source by recursively deleting all its contents.
     * This operation is performed using the utility method {@code Util.recursiveRemove}.
     *
     * @throws IOException if an I/O error occurs while removing the directory or its contents.
     */
    public void localRemove() throws IOException {
        Util.recursiveRemove(localDirectory);
    }
}
