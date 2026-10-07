package edu.uic.bitslab.propcov.core;

import edu.uic.bitslab.propcov.core.analysisframework.AbstractAnalysisFramework;
import edu.uic.bitslab.propcov.core.buildsystem.AbstractBuildSystem;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.source.AbstractSource;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for Setup implemented by extensions.
 */
abstract public class AbstractSetup {
    /**
     * A list containing all instances of classes that extend AbstractSetup.
     * This list allows for managing and accessing all discovered extension setups
     * during the runtime lifecycle. Instances are populated during initialization
     * when the `load` method scans for subclasses of AbstractSetup and invokes their
     * initialization logic.
     */
    public static final List<AbstractSetup> bridges = new ArrayList<>();

    protected void init() {
    }

    /**
     * Get new Source object for the provided extension.
     *
     * @param extension Extension details for this source implementation
     * @param projectName Name of the SUT project
     * @return Instance of a Source object
     * @throws SourceException When a given source is unable to be constructed
     */
    abstract public AbstractSource getSource(YAMLConfig.Extension extension, String projectName) throws SourceException;

    /**
     * Retrieves an instance of the test framework implementation associated with the specified extension.
     *
     * @param extension The extension configuration containing details to identify and configure the test framework.
     * @return An instance of {@link AbstractTestFramework} specific to the provided extension.
     */
    abstract public AbstractTestFramework getTestFramework(YAMLConfig.Extension extension);

    /**
     * Retrieves an instance of {@code AbstractCoverage} corresponding to the given extension details.
     * This method is intended to provide coverage-specific implementation for a given extension configuration.
     *
     * @param extension the extension details provided as an instance of {@code YAMLConfig.Extension}
     *                  containing metadata and configuration for the extension.
     * @return an instance of {@code AbstractCoverage} associated with the provided extension details.
     */
    abstract public AbstractCoverage getCoverage(YAMLConfig.Extension extension);

    /**
     * Retrieves and returns an instance of an abstract analysis framework based on the provided configuration extension.
     *
     * @param extension The configuration extension containing details, properties, and environment variables
     *                  required to construct and retrieve the corresponding analysis framework.
     * @return An instance of AbstractAnalysisFramework corresponding to the given extension.
     */
    abstract public AbstractAnalysisFramework getAnalysisFramework(YAMLConfig.Extension extension);

    /**
     * Retrieves an instance of the AbstractBuildSystem associated with a given extension,
     * including additional context like timeout configurations, project name,
     * and subproject name.
     *
     * @param extension Extension details used to determine the build system implementation.
     * @param timeouts A map of timeout configurations categorized by TimeoutType.
     * @param projectName The name of the main project.
     * @param subProjectName The name of the sub-project, if applicable.
     * @return An implementation of AbstractBuildSystem specific to the provided configuration.
     */
    abstract public AbstractBuildSystem getBuildSystem(YAMLConfig.Extension extension, Map<Config.TimeoutType, Long> timeouts, String projectName, String subProjectName);

    /**
     * Loads and executes init on all classes that extend AbstractSetup. This
     * provides discovery to load resources from each package.
     */
    public static void load() {
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().scan()) {
            scanResult
                .getSubclasses(AbstractSetup.class.getName())
                .loadClasses(AbstractSetup.class)
                .forEach( clazz -> {
                    try {
                        clazz.getConstructor().newInstance().init();
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                             InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
    }
}