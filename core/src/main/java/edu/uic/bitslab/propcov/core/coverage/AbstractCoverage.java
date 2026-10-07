package edu.uic.bitslab.propcov.core.coverage;

import edu.uic.bitslab.propcov.core.AbstractExtension;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.report.LOCTracker;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
abstract public class AbstractCoverage extends AbstractExtension {
    private final Pattern jarName = Pattern.compile("^jar:file:(?<jarFile>[^!]+)!.*");

    protected AbstractCoverage(YAMLConfig.Extension extension, String propertyPrefix) {
        super(extension, propertyPrefix);
    }

    /**
     * Retrieves coverage statistics from another system as a mapping of string keys to their associated coverage details.
     *
     * @return a map where the keys are strings representing coverage-related identifiers
     *         and the values are instances of CoverageDetail containing detailed coverage information.
     */
    abstract public Map<String, CoverageDetail> getOtherStatistics();

    /**
     * Retrieves property coverage statistics as a mapping of string identifiers to their associated coverage details.
     *
     * @return a map where the keys are strings representing property-related identifiers
     *         and the values are instances of CoverageDetail containing detailed coverage information.
     */
    abstract public Map<String, CoverageDetail> getPropCovStatistics();

    /**
     * Retrieves a set of identifiers representing nodes that are missing property coverage information.
     *
     * @return a set of strings where each string represents the identifier of a node that lacks property coverage.
     */
    abstract public Set<String> getMissingNodesPropCov();

    /**
     * Retrieves detailed coverage statistics from another coverage system for a specified identifier.
     *
     * @param name the name or identifier for which coverage details are requested
     * @return an instance of CoverageDetail containing the other coverage statistics for the specified identifier
     */
    abstract public CoverageDetail getOtherStatistics(String name);

    /**
     * Retrieves detailed property coverage statistics for a specified identifier.
     *
     * @param name the name or identifier for which property coverage details are requested
     * @return an instance of CoverageDetail containing the property coverage statistics for the specified identifier
     */
    abstract public CoverageDetail getPropCovStatistics(String name);

    /**
     * Applies specific coverage analysis logic to a given graph structure and configuration.
     *
     * @param g the graph structure represented as a {@code Graph<String, DefaultEdge>} instance that this method analyzes.
     * @param config the configuration object containing parameters and metadata necessary for the analysis.
     * @param property the property test under evaluation
     * @throws CoverageException if an error occurs during the coverage analysis process.
     */
    abstract public void apply(Graph<String, DefaultEdge> g, Config config, PropertyTest property) throws CoverageException;

    /**
     * Resets the state or data associated with the specified target path.
     *
     * @param target the path to be reset, represented as a {@code Path} object
     * @throws IOException if an I/O error occurs during the reset process
     */
    abstract public void reset(Path target) throws IOException;

    /**
     * Retrieves an array of paths representing the result starting from a given path.
     *
     * @param start the initial path from which result paths are derived
     * @return an array of {@code Path} objects representing the resulting paths
     */
    abstract public Path[] getResultPaths(Path start);

    /**
     * Builds and updates line of code (LOC) details for a given class. This method processes
     * the class byte array, analyzes its content, and populates the LOC details map with
     * associated information. It also considers inheritance relationships while updating LOC details.
     *
     * @param clazzBytes the byte array representing the class file to be analyzed
     * @param locs a map where keys are identifiers of classes or methods and values are LOC details
     *             represented as {@code LOCTracker.LOCDetail}
     * @param inherit a map representing inheritance relationships with keys as class names
     *                and values as their parent class names or interfaces
     */
    abstract public void buildLOC(byte[] clazzBytes, Map<String, LOCTracker.LOCDetail> locs, Map<String, String> inherit);

        /**
         * Detect if the given class is a test or library method.
         *
         * @param config Full configuration object defined for this project.
         * @param clazz Given class to review
         * @return true if is a test or library method, otherwise false.
         * @throws IOException Thrown if unable to resolve class
         */
    public boolean IsTestOrLibraryMethod(Config config, Class<?> clazz) throws IOException {
        if (clazz == null) return false;

        URL resourceURL;
        if (clazz.isMemberClass() || clazz.isAnonymousClass()) {
            resourceURL = clazz.getResource(Util.tunnelInnerClass(clazz));
        } else {
            resourceURL = clazz.getResource(clazz.getSimpleName() + ".class");
        }

        if (resourceURL == null){
            throw new IOException("Unable to load resource for class " + clazz.getName());
        }

        // check if we have a jar file in the resource url
        Matcher urlMatches = jarName.matcher(resourceURL.toString());
        if (!urlMatches.matches()) return false;

        // review each to see if the classJarFile is the same as the testJars file name
        Path classJarFile = Path.of(urlMatches.group("jarFile"));
        for (Path mainJar : config.mainJars) {
            try {
                if (Files.isSameFile(classJarFile, mainJar)) {
                    return false;
                }
            } catch (IOException ignored) {
                // files aren't same or missing
            }
        }

        // not main file
        return true;
    }

    /**
     * Determines whether the given method is implied by checking if it is abstract.
     *
     * @param clazz the class to which the method belongs; must not be null
     * @param method the method to be evaluated; must not be null
     * @return true if the method is abstract, false otherwise
     */
    public boolean IsImpliedMethod(Class<?> clazz, Executable method) {
        if (clazz == null || method == null)
            return false;
        return Modifier.isAbstract(method.getModifiers());
    }
}
