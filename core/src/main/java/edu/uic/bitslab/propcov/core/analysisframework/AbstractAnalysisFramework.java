package edu.uic.bitslab.propcov.core.analysisframework;

import edu.uic.bitslab.propcov.core.AbstractExtension;
import edu.uic.bitslab.propcov.core.config.AnalysisConfig;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import scala.Tuple2;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract class extended by Analysis Framework Extensions
 */
abstract public class AbstractAnalysisFramework extends AbstractExtension {
    /**
     * Sentential value used to indicate a type parameter was unable to be resolved
     * by the underlying Analysis Framework.
     */
    public static final String PARAM_NOT_FOUND = "ParamNotFound";

    /**
     * Defines the Analysis Type as the proper enum.
     */
    public final AnalysisConfig.AnalysisType analysisType;

    /**
     * Defines the set of Analysis Flags active.
     */
    public final Set<AnalysisConfig.AnalysisFlag> analysisFlags = new HashSet<>();

    /**
     * @param extension Defines extension name, properties, and environment variables.
     * @param propertyPrefix Defines prefix used for this extensions Java Property Name(s)
     */
    public AbstractAnalysisFramework(YAMLConfig.Extension extension, String propertyPrefix) {
        super(extension, propertyPrefix);

        analysisType = AnalysisConfig.AnalysisType.valueOf(extension.properties.get("AnalysisType"));

        String strAnalysisFlags = extension.properties.get("AnalysisFlags");
        if (strAnalysisFlags != null && !strAnalysisFlags.isEmpty()) {
            analysisFlags.addAll(
                Arrays.stream(strAnalysisFlags.split(","))
                    .map(AnalysisConfig.AnalysisFlag::valueOf)
                    .collect(Collectors.toSet())
            );
        }
    }

    /**
     * Interface to the Analysis Framework to Build the Call Graph.
     *
     * @param config Full configuration object defined for this project.
     * @param propertyTest Name and Entrypoint for the starting test method.
     * @return Graph representing the discovered call graph.
     * @throws AnalysisFrameworkException Occurs when an error happens during construction of the graph.
     */
    abstract public Graph<String, DefaultEdge> BuildCallGraph(Config config, PropertyTest propertyTest) throws AnalysisFrameworkException;

    /**
     * Get a list of properties from the given input stream and test framework object.
     * @param inputStream Input stream from SUT Jar
     * @param testFramework Test framework used
     * @return List of properties as simple method names, JVM method names tuples
     */
    abstract public List<Tuple2<String, String>> GetProperties(InputStream inputStream, AbstractTestFramework testFramework);

}
