package edu.uic.bitslab.propcov.extensions.analysisframework;

import edu.uic.bitslab.propcov.core.analysisframework.AbstractAnalysisFramework;
import edu.uic.bitslab.propcov.core.analysisframework.AnalysisFrameworkException;
import edu.uic.bitslab.propcov.core.config.AnalysisConfig;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import edu.uic.bitslab.propcov.core.util.SUTClassLoader;
import edu.uic.bitslab.propcov.core.util.Timed;
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.GetProperties;
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.ParsedCallgraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import scala.Tuple2;
import scala.jdk.CollectionConverters;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.uic.bitslab.propcov.core.config.Config.TimeoutType.buildCallgraph;
import static edu.uic.bitslab.propcov.core.util.EnumHelper.stringToCollection;
import static edu.uic.bitslab.propcov.core.util.EnumHelper.stringToObject;

/**
 * The Opal class is an extension of the AbstractAnalysisFramework and serves as an implementation
 * of the OPAL (Object-oriented Programming And Logic) analysis framework. It provides methods for building
 * a call graph and retrieving properties from input streams while using specific configurations
 * defined in the OpalAnalysisConfig.
 */
public class Opal extends AbstractAnalysisFramework {
    /**
     * Represents the logging levels for the OPAL framework. The OPALLoggerType enum is used to define the
     * severity levels of logging messages generated within the OPAL analysis framework. It provides a set
     * of predefined constants for categorizing log messages.
     * Available logging levels include:
     * - warn: Represents warning messages.
     * - info: Represents informational messages.
     * - error: Represents error messages, typically indicating a failure or issue.
     * - fatal: Represents critical errors that might require halting execution.
     */
    public enum OPALLoggerType {
        /**
         * Represents the warning logging level for the OPAL framework.
         * It is used to categorize log messages that indicate potential issues
         * or situations that might require attention but do not necessarily
         * represent an error or critical failure.
         */
        warn,
        /**
         * Represents the informational logging level for the OPAL framework.
         * The `info` level is used to categorize general operational messages,
         * such as those that provide high-level summaries of the framework's activities
         * or progress during execution. These messages are mainly intended for tracking
         * the framework's flow and providing insights about its operation.
         */
        info,
        /**
         * Represents the logging level for error messages.
         * The error level is typically used to indicate failures or issues
         * that have occurred during the execution of the OPAL analysis framework.
         */
        error,
        /**
         * Represents the 'fatal' logging level in the OPALLoggerType enumeration.
         * This level indicates critical errors that typically require immediate attention and
         * may require halting the execution of the program. It is used to log messages
         * that reflect serious issues within the analysis framework.
         */
        fatal
    }

    /**
     * A configuration class for the OPAL analysis framework that extends the base AnalysisConfig class.
     * This configuration includes an additional property for specifying the logging type used within
     * the OPAL framework.
     */
    public static class OpalAnalysisConfig extends AnalysisConfig {
        /**
         * Specifies the logging type to be used within the OPAL analysis framework.
         * The logging type determines the severity level for logging messages during
         * the framework's operation. This variable is a constant and is initialized
         * through the builder pattern of the containing configuration class.
         */
        public final OPALLoggerType opalLoggerType;

        protected OpalAnalysisConfig(Builder builder) {
            super(builder);
            this.opalLoggerType = builder.opalLoggerType;
        }

        /**
         * Builder class for creating instances of {@link OpalAnalysisConfig}.
         * This class extends {@link AnalysisConfig.Builder} to include additional
         * configuration options specific to the OPAL analysis framework.
         */
        public static class Builder extends AnalysisConfig.Builder {
            private OPALLoggerType opalLoggerType = OPALLoggerType.warn;

            /**
             * Sets the logging type to be used within the OPAL analysis configuration.
             * The logging type determines the severity level for logging messages during
             * the framework's operation. The available levels are defined by {@link OPALLoggerType}.
             *
             * @param loggerType the logging level to set. Must be an instance of {@link OPALLoggerType},
             *                   such as "warn", "info", "error", or "fatal".
             * @return the builder instance with the specified logging level configured.
             */
            public Builder opalLoggerType(OPALLoggerType loggerType) {
                this.opalLoggerType = loggerType;
                return this;
            }

            /**
             * Sets the collection of flags to configure specific behaviors or options
             * for the OPAL analysis framework. Flags can be used to enable or disable
             * certain features or modes of operation.
             *
             * @param analysisFlags a collection of {@link AnalysisFlag} instances specifying the desired
             *              configuration flags. Accepts flags such as EXCEPTION or ITERATOR.
             *              Passing null will leave the flags unchanged.
             * @return the builder instance with the specified flags configured.
             */
            public Builder analysisFlags(Collection<AnalysisFlag> analysisFlags) {
                super.analysisFlags(analysisFlags);
                return this;
            }

            /**
             * Sets the analysis type to be used within the analysis configuration.
             * The analysis type determines the specific approach or algorithm that
             * the framework will use for analysis.
             *
             * @param analysisType the desired type of analysis. Must be an instance
             *                     of {@link AnalysisType}, such as CHA, RTA, CTA, or others
             *                     defined in the {@link AnalysisType} enumeration.
             * @return the builder instance with the specified analysis type configured.
             */
            public Builder analysisType(AnalysisType analysisType) {
                super.analysisType(analysisType);
                return this;
            }

            /**
             * Builds a new instance of {@link OpalAnalysisConfig} using the properties set in the builder.
             * This method finalizes the configuration setup and returns a fully initialized
             * {@link OpalAnalysisConfig} object.
             *
             * @return a new instance of {@link OpalAnalysisConfig} initialized with the builder's state.
             */
            public OpalAnalysisConfig build() {
                return new OpalAnalysisConfig(this);
            }
        }
    }

    private final OpalAnalysisConfig opalAnalysisConfig;

    /**
     * Constructs an instance of the Opal class with the specified YAMLConfig.Extension configuration.
     * This constructor initializes the OPAL analysis configuration and logging level based on the extension's properties.
     *
     * @param extension The extension configuration containing properties and environment variables for initializing the OPAL framework.
     */
    public Opal(YAMLConfig.Extension extension) {
        super(extension, "PropCov.AnalysisFramework.Opal.");

        opalAnalysisConfig = (new OpalAnalysisConfig.Builder())
                .opalLoggerType(stringToObject(extension.properties.getOrDefault("OPALLoggerType", "warn"), OPALLoggerType.class))
                .analysisType(stringToObject(extension.properties.get("AnalysisType"), AnalysisConfig.AnalysisType.class))
                .analysisFlags(stringToCollection(extension.properties.get("AnalysisFlags"), AnalysisConfig.AnalysisFlag.class))
                .build();
    }

    @Override
    public Graph<String, DefaultEdge> BuildCallGraph(Config config, PropertyTest propertyTest) throws AnalysisFrameworkException {
        try {
            scala.collection.immutable.Set<Path> jars = CollectionConverters.SetHasAsScala(
                    Stream.concat(
                            Arrays.stream(config.mainJars),
                            Arrays.stream(config.testJars)
                    ).collect(Collectors.toSet())
            ).asScala().toSet();

            scala.collection.immutable.Set<Path> dependenciesSet = CollectionConverters.SetHasAsScala(
                    Arrays.stream(config.mainJarsWithDependencies).collect(Collectors.toSet())
            ).asScala().toSet();


            Timed<Graph<String, DefaultEdge>> timed = new Timed<>(config.timeouts.get(buildCallgraph), TimeUnit.SECONDS);
            return timed.exec(() ->
                    ParsedCallgraph.build(propertyTest.entryPoint, jars, dependenciesSet, opalAnalysisConfig, SUTClassLoader.get(), this.artifact)
            );
        } catch (TimeoutException e) {
            throw new AnalysisFrameworkException("Analysis timeout occurred.", e);
        } catch (ExecutionException | InterruptedException e) {
            throw new AnalysisFrameworkException("An error occurred while building the call graph.", e);
        }
    }

    public List<Tuple2<String, String>> GetProperties(InputStream inputStream, AbstractTestFramework testFramework) {
        return scala.jdk.CollectionConverters.SeqHasAsJava(
                GetProperties.get(inputStream, testFramework::isTestProperty)
        ).asJava();
    }
}
