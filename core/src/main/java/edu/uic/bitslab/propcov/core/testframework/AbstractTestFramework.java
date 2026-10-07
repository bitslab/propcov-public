package edu.uic.bitslab.propcov.core.testframework;

import edu.uic.bitslab.propcov.core.AbstractExtension;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import org.opalj.br.Method;

/**
 * The AbstractTestFramework class serves as the base class for test framework integrations. It is an
 * abstraction designed to be extended by specific test framework implementations that determine the
 * criteria for identifying test properties and applying framework-specific configurations. This class
 * itself is an extension of the AbstractExtension class, inheriting its capabilities for managing
 * YAML-based extension configurations.
 */
abstract public class AbstractTestFramework extends AbstractExtension {
    /**
     * Constructs an instance of AbstractTestFramework, associating it with a specific YAML configuration
     * extension and property prefix. This constructor manages the initialization of the framework's
     * environment and properties based on the provided parameters.
     *
     * @param extension the YAMLConfig.Extension instance providing the configuration details and
     *                  environment properties for the test framework. This includes resolved
     *                  properties that define behaviors and settings specific to the framework.
     * @param propertyPrefix the prefix used to filter and process properties that are relevant
     *                       to configuring this specific test framework. Properties matching this
     *                       prefix will be applied to the extension.
     */
    public AbstractTestFramework(YAMLConfig.Extension extension, String propertyPrefix) {
        super(extension, propertyPrefix);
    }

    /**
     * Determines whether the specified method is considered a test property in the context of
     * a specific test framework. This method is abstract and must be implemented by subclasses
     * to define the specific criteria for test properties in their respective frameworks.
     *
     * @param method the {@link Method} instance to evaluate as a possible test property.
     *               This instance represents a method within a class, and its annotations
     *               or other characteristics can be used to determine if it qualifies as
     *               a test property.
     * @return a {@link Boolean} indicating whether the provided method matches the criteria
     *         for being a test property. Returns {@code true} if the method is a test
     *         property; otherwise, {@code false}.
     */
    abstract public Boolean isTestProperty(Method method);
}
