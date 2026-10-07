package edu.uic.bitslab.propcov.core.config;

import edu.uic.bitslab.propcov.core.AbstractSetup;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.analysisframework.AbstractAnalysisFramework;
import edu.uic.bitslab.propcov.core.buildsystem.AbstractBuildSystem;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.source.AbstractSource;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import edu.uic.bitslab.propcov.core.util.SUTClassLoader;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.uic.bitslab.propcov.core.Props.*;

/**
 * Configuration class that holds all settings and parameters for the property coverage analysis system.
 * This class implements Serializable to allow for persistence of configuration data.
 */
public class Config implements Serializable {
    /**
     * Represents the name of the SUT being configured. It is expected to
     * correspond to the name of a project folder or related artifact
     * directory structure defined in the build system.
     */
    public final String project;

    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * Represents the name of a specific subproject within the SUT
     */
    public final String subProject;

    /**
     * An array of {@link Path} objects representing the primary JAR files
     * for the SUT's main modules. These JAR files typically include
     * the compiled code for the main functionality of the SUT.
     */
    public final Path[] mainJars;

    /**
     * Represents an array of {@link Path} objects pointing to the main JAR file
     * along with its dependencies for the SUT. This array is used to define
     * the complete set of classpath entries for the main application runtime.
     */
    public final Path[] mainJarsWithDependencies;

    /**
     * Represents an array of paths to the test JAR files used in the SUT.
     */
    public final Path[] testJars;

    /**
     * Represents the source code retrieval functions for the SUT as an
     * instance of {@link AbstractSource}. The specific implementation of the
     * source (e.g., details of how the source is retrieved) is provided by
     * the subclass of {@link AbstractSource}.
     */
    public final AbstractSource source;

    /**
     * Represents the analysis framework used for various operations such as constructing
     * the call graph and extracting properties during the analysis process. This variable
     * is an instance of AbstractAnalysisFramework and is initialized based on the
     * project's configuration.
     */
    public final AbstractAnalysisFramework analysisFramework;

    /**
     * Represents the file path to the patch to be applied to the SUT before
     * running analysis within PropCov.
     */
    public final String patchFile;

    /**
     * Represents a collection of PropertyTest objects associated with the configuration. Each
     *  PropertyTest object includes the name and the entry point of the test.
     */
    public final Set<PropertyTest> properties;

    /**
     * Specifies the path where generated output files are stored.
     */
    public final String outputPath;

    /**
     * Represents a collection of package names associated with this SUT to
     * restrict the scope of analysis within the defined packages in a
     * project configuration.
     */
    public final Set<String> packages;

    /**
     * Represents the build system configuration for the SUT. This variable holds an instance of
     * a concrete implementation of the abstract class {@code AbstractBuildSystem}, which defines
     * the behavior required to manage and interact with the project's build system.
     */
    public final AbstractBuildSystem buildSystem;

    /**
     * Represents the directory where artifacts generated during the build
     * or testing process are stored.
     */
    public final String artifactDirectory;

    /**
     * Represents the defined workflow of the system as a set of {@link WorkflowItem}.
     * Each {@link WorkflowItem} corresponds to a specific operation or step in the
     * overall system process, such as building, analysis, or patching.
     */
    public final Set<WorkflowItem> workflow;

    /**
     * Represents an array of colors associated with each type of callgraph node.
     */
    public final String[] nodeColor;

    /**
     * Represents the default color assigned to paths in the configuration.
     */
    public final String pathDefaultColor;

    /**
     * Represents an array of colors used to color paths for improvement
     * based on what the system Heuristic reports.
     */
    public final String[] pathImprovementColors;

    /**
     * Represents the coverage analysis configuration for the current project.
     * This variable is an instance of {@link AbstractCoverage}, which serves as the
     * base class for handling various types of coverage statistics related to the SUT.
     */
    public final AbstractCoverage coverage;

    /**
     * A mapping of timeout types to their corresponding timeout durations. Each key in this map
     * represents a specific operation defined by the {@link Config.TimeoutType} enumeration,
     * and the value represents the timeout duration in seconds for that operation.
     */
    public final Map<TimeoutType, Long> timeouts;

    /**
     * A field that holds the parsed and stored YAML configuration for this configuration.
     */
    public final YAMLConfig storedYAMLConfig;

    /**
     * A builder instance that stores pre-configured settings for constructing a {@link Config} object.
     * This variable provides a way to initialize and reuse customizable configuration setups for
     * creating Config instances.
     */
    public final Config.Builder storedBuilder;

    /**
     * Represents a test framework configuration used in the project.
     * This field holds an instance of an abstract test framework implementation
     * that provides functionality for managing and interacting with test frameworks.
     */
    public final AbstractTestFramework testFramework;

    /**
     * Represents different types of timeout operations in the system.
     */
    public enum TimeoutType {
        /** Timeout for patching the System Under Test (SUT) */
        patchSUT,
        /** Timeout for cleaning the System Under Test */
        cleanSUT,
        /** Timeout for building the System Under Test */
        buildSUT,
        /** Timeout for testing properties of the System Under Test */
        testPropertySUT,
        /** Timeout for DOT file generation */
        dotGeneration,
        /** Timeout for building the call graph */
        buildCallgraph
    }

    /**
     * Defines the different workflow steps available in the system.
     */
    public enum WorkflowItem {
        /**
         * Represents the "Remove" workflow step in the system. This step signifies the removal
         * of the retrieved SUT.
         */
        Remove,
        /**
         * Represents the "Download" step for retrieving the SUT.
         */
        Download,
        /**
         * Represent the SUT build step that calls the SUT build system, such as compilation, dependency resolution,
         * or related actions.
         */
        RunBuild,
        /**
         * Represents the build call graph step for running each property test and generating the analysis artifacts.
         */
        BuildCallGraph,
        /**
         * Represents the property coverage analysis step.
         */
        RunPropertyCoverage,
        /**
         * Represents the "ApplyCoverage" step in the workflow process for applying coverage results to the SUT
         */
        ApplyCoverage,
        /**
         * Represents the "Prune" step in the workflow process to handle tasks related to the reduction or
         * refinement of graph nodes.
         */
        Prune,
        /**
         * Represents the action or workflow step where a patch is applied on the SUT.
         */
        Patch,
        /**
         * Represents the "Analysis" workflow step within the system.
         */
        Analysis
    }

    /**
     * Defines shorthand combinations of workflow items for common operations.
     */
    public enum WorkflowShortName {
        /** Git operations including remove, download, and patch */
        git(WorkflowItem.Remove,WorkflowItem.Download,WorkflowItem.Patch),
        /** Fetch operations including remove, download, and patch */
        fetch(WorkflowItem.Remove,WorkflowItem.Download,WorkflowItem.Patch),
        /** Build operation */
        build(WorkflowItem.RunBuild),
        /** Test operations including call graph building, coverage, and analysis */
        test(WorkflowItem.BuildCallGraph,WorkflowItem.RunPropertyCoverage,WorkflowItem.ApplyCoverage,WorkflowItem.Analysis);

        /**
         * Represents an array of workflow items that define individual workflow steps or operations in the system.
         * Each element in the array corresponds to a predefined step enumerated in the {@link WorkflowItem} enum.
         * Used to outline or configure sequences of actions within a workflow or operation.
         */
        public final WorkflowItem[] workflowItems;

        WorkflowShortName(WorkflowItem ...workflowItems) {
            this.workflowItems = workflowItems;
        }
    }

    /**
     * Defines different types of nodes in the analysis graph.
     */
    public enum NodeType {
        /**
         * Represents an undefined or unclassified node type.
         */
        UNKNOWN,
        /**
         * Represents a node type in the analysis graph that signifies
         * an error has occurred or that a problematic state exists.
         */
        ERROR,
        /**
         * Represents a node type in the analysis graph that signifies an entry point (starting point).
         */
        ENTRYPOINT,
        /**
         * Represents a node type in the analysis graph related to test functionality.
         */
        TEST,
        /**
         * Represents a node type in the analysis graph that is inferred or deduced indirectly based
         * on the relationships or context of other nodes.
         */
        IMPLIED,
        /**
         * Represents a node type in the analysis graph that corresponds to a node having no coverage
         */
        COVERAGE0,
        /**
         * Represents a node type in the analysis graph with minimal coverage.
         */
        COVERAGE1,
        /**
         * Represents a node type with some coverage.
         */
        COVERAGE2,
        /**
         * Represents a node type with a medium level of coverage.
         */
        COVERAGE3,
        /**
         * Represents a node type that is fully covered.
         */
        COVERAGE4,
        /**
         * Represents a virtual node in the analysis graph.
         * This node type is used for abstract or computed nodes
         * that do not correspond to concrete entities or locations.
         */
        VIRTUAL
    }

    /**
     * Defines ranking levels for path improvements.
     */
    public enum PathImprovementRank {
        /**
         * Represents the first level of ranking for path improvements.
         * Used to categorize improvements with the highest priority.
         */
        FIRST,
        /**
         * Represents the second level of ranking for path improvements, that are improvements prioritized
         * below the FIRST level but above the THIRD level of ranking.
         */
        SECOND,
        /**
         * Represents the third level of ranking for path improvements.
         * Used to categorize improvements with a lower priority compared
         * to FIRST and SECOND.
         */
        THIRD
    }

    /**
     * Converts the configuration to YAML format.
     * @return String representation of the configuration in YAML format
     */
    public String toYaml() {
        YAMLConfig yamlConfig;

        if (storedYAMLConfig == null) {
            yamlConfig = new YAMLConfig();
            yamlConfig.version = 1;
            yamlConfig.name = project;
            if (subProject != null) yamlConfig.subProject = subProject;
            yamlConfig.source = source.extension;
            yamlConfig.patchName = patchFile;
            yamlConfig.mainJar = new YAMLConfig.ListOrString(Arrays.stream(mainJars).map(Path::toString).collect(Collectors.toList()));
            yamlConfig.mainJarWithDependencies = new YAMLConfig.ListOrString(Arrays.stream(mainJarsWithDependencies).map(Path::toString).collect(Collectors.toList()));
            yamlConfig.testJar = new YAMLConfig.ListOrString(Arrays.stream(testJars).map(Path::toString).collect(Collectors.toList()));
            yamlConfig.buildSystem = buildSystem.extension;
            yamlConfig.packageNames = List.copyOf(packages);
            yamlConfig.properties = List.copyOf(properties);
            yamlConfig.testFramework = testFramework.extension;
            yamlConfig.coverage = coverage.extension;
            yamlConfig.analysisFramework = analysisFramework.extension;
            yamlConfig.timeouts = timeouts;
        } else {
            yamlConfig = storedYAMLConfig;
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(new YamlConfigRepresenter(options), options);
        return yaml.dump(yamlConfig);
    }

    private Config(Builder builder, YAMLConfig storedYAMLConfig)  {
        this.storedYAMLConfig = storedYAMLConfig;
        this.storedBuilder = builder;

        this.project = builder.project;
        this.subProject = builder.subProject;
        this.mainJars = builder.mainJars.toArray(new Path[0]);
        this.mainJarsWithDependencies = builder.mainJarsWithDependencies.toArray(new Path[0]);
        this.testJars = builder.testJars.toArray(new Path[0]);
        this.source = builder.source;
        this.patchFile = builder.patchFile;
        this.properties = builder.properties;
        this.outputPath = builder.outputPath;
        this.packages = builder.packages;
        this.buildSystem = builder.buildSystem;
        this.artifactDirectory = builder.artifactDirectory;
        this.workflow = builder.workflow;
        this.nodeColor = builder.nodeColor;
        this.pathDefaultColor = builder.pathDefaultColor;
        this.pathImprovementColors = builder.pathImprovementColors;
        this.coverage = builder.coverage;
        this.timeouts = builder.timeouts;
        this.testFramework = builder.testFramework;
        this.analysisFramework= builder.analysisFramework;

        SUTClassLoader.addPaths(sutClassPaths());
    }

    /**
     * Gets the classpath entries for the System Under Test (SUT).
     * @return Array of Path objects representing the SUT classpath
     */
    private Path[] sutClassPaths() {
        return Stream.of(mainJars, testJars, mainJarsWithDependencies)
            .flatMap(Arrays::stream)
            .toArray(Path[]::new);
    }

    /**
     * Builder class for creating Config instances using the builder pattern.
     */
    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public static class Builder {
        private final String project;
        private String subProject;
        private final Set<Path> mainJars = new HashSet<>();
        private final Set<Path> mainJarsWithDependencies = new HashSet<>();
        private final Set<Path> testJars = new HashSet<>();
        private AbstractSource source;
        private AbstractBuildSystem buildSystem;
        private String patchFile;
        private final Set<PropertyTest> properties = new HashSet<>();
        private String outputPath = "output";
        private final Set<String> packages = new HashSet<>();
        private String artifactDirectory;
        private final Map<TimeoutType, Long> timeouts = new HashMap<>();
        private AbstractTestFramework testFramework;
        private AbstractAnalysisFramework analysisFramework;

        private String pathDefaultColor = "black";

        private final String[] pathImprovementColors = {
                resolveString("PropCov.PathImprovementColorFirst"),
                resolveString("PropCov.PathImprovementColorSecond"),
                resolveString("PropCov.PathImprovementColorThird")
        };

       private final String[] nodeColor = new String[NodeType.values().length];

        /**
         * A predefined set of workflow items representing the starting state of a workflow process.
         * The set is initialized using a utility method that provides the default or initial configuration
         * of workflow items.
         */
        public final Set<WorkflowItem> workflow = Util.getStartingWorkflow();

        private AbstractCoverage coverage;

        /**
         * Creates a new Builder instance for the specified project.
         * @param project The name of the project to configure
         */
        public Builder(String project) {
            this.project = resolveString("PropCov.Project", project);

            // ensure init all colors to white
            Arrays.fill(nodeColor, "white");

            // Set colors based on properties
            nodeColor[NodeType.ERROR.ordinal()] = resolveString("PropCov.NodeColorError");
            nodeColor[NodeType.ENTRYPOINT.ordinal()] = resolveString("PropCov.NodeColorEntryPoint");
            nodeColor[NodeType.TEST.ordinal()] = resolveString("PropCov.NodeColorTest");
            nodeColor[NodeType.IMPLIED.ordinal()] = resolveString("PropCov.NodeColorImplied");
            nodeColor[NodeType.COVERAGE0.ordinal()] = resolveString("PropCov.NodeColorCoverage0");
            nodeColor[NodeType.COVERAGE1.ordinal()] = resolveString("PropCov.NodeColorCoverage1");
            nodeColor[NodeType.COVERAGE2.ordinal()] = resolveString("PropCov.NodeColorCoverage2");
            nodeColor[NodeType.COVERAGE3.ordinal()] = resolveString("PropCov.NodeColorCoverage3");
            nodeColor[NodeType.COVERAGE4.ordinal()] = resolveString("PropCov.NodeColorCoverage4");
            nodeColor[NodeType.VIRTUAL.ordinal()] = resolveString("PropCov.NodeColorVirtual");

            // Set timeouts to default
            timeouts.putAll(getDefaultTimeout());
        }

        /**
         * Gets the default timeout values for all timeout types.
         * @return Map of timeout types to their default values
         */
        public static Map<Config.TimeoutType, Long> getDefaultTimeout() {
            Map<Config.TimeoutType, Long> defaultTimeouts = new HashMap<>();

            long defaultTimeoutValue = resolveLong("PropCov.TimeoutDefault");
            for (TimeoutType value : TimeoutType.values()) {
                defaultTimeouts.put(value, defaultTimeoutValue);
            }

            return defaultTimeouts;
        }

        /**
         * Attempts to resolve a path relative to the SUT's location.
         * @param path The path to resolve
         * @return The resolved absolute path
         * @throws NullPointerException if required configuration elements are missing
         * @throws IndexOutOfBoundsException if required paths are empty
         */
        public Path guessSUTPath(Path path)  {
            // always use absolute
            if (path.isAbsolute()) return path;

            if (source == null) throw new NullPointerException("source is null");
            if (buildSystem == null) throw new NullPointerException("buildSystem is null");
            if (source.localDirectory.toString().isEmpty()) throw new IndexOutOfBoundsException("source.localDirectory is empty");

            // if starts with ./ then know
            if (path.startsWith("./")) return source.localDirectory.resolve(path);

            return buildSystem.getFullTargetPath(null).resolve(path);
        }

        /**
         * Attempts to infer the path to the System Under Test (SUT) based on the given file name.
         *
         * @param file the name of the file as a string that is used to guess the SUT path
         * @return the inferred path to the SUT as a Path object
         */
        public Path guessSUTPath(String file)  {
            return guessSUTPath(Path.of(file));
        }

        /**
         * Sets the colors corresponding to the different path improvement ranks.
         *
         * @param first  the color associated with the first improvement rank
         * @param second the color associated with the second improvement rank
         * @param third  the color associated with the third improvement rank
         * @return the updated Builder instance
         */
        public Builder pathImprovementColors(String first, String second, String third) {
            this.pathImprovementColors[PathImprovementRank.FIRST.ordinal()] = first;
            this.pathImprovementColors[PathImprovementRank.SECOND.ordinal()] = second;
            this.pathImprovementColors[PathImprovementRank.THIRD.ordinal()] = third;
            return this;
        }

        /**
         * Sets the default color for the path.
         *
         * @param color the default color to be used for the path
         * @return the Builder instance, allowing for method chaining
         */
        public Builder pathDefaultColor(String color) {
            this.pathDefaultColor = color;
            return this;
        }

        /**
         * Sets the color for a specific node type.
         *
         * @param nodeType the type of the node whose color is to be set
         * @param color the color to assign to the specified node type
         * @return this builder instance for method chaining
         */
        public Builder nodeColor(NodeType nodeType, String color) {
            nodeColor[nodeType.ordinal()] = color;
            return this;
        }

        /**
         * Replaces the current workflow with the provided collection of {@code WorkflowItem}s.
         * If the provided workflow is {@code null}, no changes are made.
         *
         * @param workflow the collection of {@code WorkflowItem}s to replace the current workflow
         * @return the current {@code Builder} instance for method chaining
         */
        public Builder workflowReplace(Collection<WorkflowItem> workflow) {
            if (workflow == null) return this;

            this.workflow.clear();
            this.workflow.addAll(workflow);
            return this;
        }

        /**
         * Removes the specified collection of WorkflowItem objects from the current workflow.
         *
         * @param workflow the collection of WorkflowItem objects to be removed from the existing workflow.
         *                 If the input is null, no action is performed.
         * @return the current Builder instance after the specified items have been removed.
         */
        public Builder workflowRemove(Collection<WorkflowItem> workflow) {
            if (workflow == null) return this;

            this.workflow.removeAll(workflow);
            return this;
        }

        /**
         * Sets the subproject value for the builder. The value is resolved
         * using the specified property key and the provided input.
         *
         * @param subProject the name or identifier of the subproject to set
         * @return the builder instance with the updated subproject value
         */
        public Builder subProject(String subProject) {
            this.subProject = resolveString("PropCov.SubProject", subProject);
            return this;
        }

        /**
         * Adds the provided main JAR file path to the main JAR collection after validation and resolution.
         *
         * @param mainJars the file path of the main JAR to be added
         * @return the updated Builder instance
         */
        public Builder addMainJars(List<String> mainJars)  {
            if (mainJars == null) throw new IllegalArgumentException("mainJars cannot be null");
            for (String jar : mainJars) {
                String mainJar = resolveString("PropCov.MainJar", jar);
                validateNotEmpty("mainJar", mainJar);
                this.mainJars.add(guessSUTPath(mainJar));
            }

            return this;
        }

        /**
         * Adds the specified main JAR file path with its dependencies to the builder configuration.
         * This allows the main JAR and its dependencies to be resolved and added for further processing.
         *
         * @param mainJarsWithDependencies the path to the main JAR file with its dependencies.
         *                                 This can be a direct path or a property key that resolves to the path.
         *                                 If the input is null or empty, the method will return without making changes.
         * @return the current instance of the Builder to allow method chaining.
         */
        public Builder addMainJarsWithDependencies(List<String> mainJarsWithDependencies)  {
            if (mainJarsWithDependencies == null) return this;
            for (String jar : mainJarsWithDependencies) {
                String mainJarWithDependencies = resolveString("PropCov.MainJarWithDependencies", jar);
                if (!(mainJarWithDependencies == null || mainJarWithDependencies.isEmpty())) {
                    this.mainJarsWithDependencies.add(guessSUTPath(mainJarWithDependencies));
                }
            }
            return this;
        }

        /**
         * Adds a test jar to the Builder's configuration.
         *
         * @param testJars the path or name of the test jar to be added. This value is resolved,
         *                validated to ensure it is not empty, and then processed to determine
         *                the correct location or identifier for the system under test.
         * @return the Builder instance for method chaining.
         */
        public Builder addTestJars(List<String> testJars)  {
            for (String jar : testJars) {
                String testJar = resolveString("PropCov.TestJar", jar);
                validateNotEmpty("testJar", testJar);

                this.testJars.add(guessSUTPath(testJar));
            }
            return this;
        }

        /**
         * Sets the source for the builder using the given YAMLConfig.Extension instance.
         * Iterates through all registered bridges to find a matching source.
         *
         * @param extension the YAML configuration extension based on which the source is determined
         * @return the updated Builder instance
         * @throws ConfigException if no suitable source class is found for the given extension
         * @throws SourceException Unable to create a source object
         */
        public Builder source(YAMLConfig.Extension extension) throws ConfigException, SourceException {
            for (AbstractSetup bridge : AbstractSetup.bridges) {
                AbstractSource newSource = bridge.getSource(extension, project);
                if (newSource != null) {
                    this.source = newSource;
                    return this;
                }
            }

            throw new ConfigException("No source class for " + extension.extensionClass + " found.");
        }

        /**
         * Sets the path for the patch file.
         *
         * @param patchFile the path to the patch file to be resolved and set
         * @return the current Builder instance for method chaining
         */
        public Builder patchFile(String patchFile) {
            patchFile = resolveString("PropCov.PatchFile", patchFile);
            this.patchFile = patchFile;
            return this;
        }

        /**
         * Configures the test framework based on the provided extension configuration.
         * Searches through available setup bridges to find the matching test framework
         * and sets it for the builder if found.
         *
         * @param extension the extension configuration used to determine the appropriate test framework
         * @return the builder instance with the configured test framework
         * @throws ConfigException if no matching test framework is found for the given extension
         */
        public Builder testFramework(YAMLConfig.Extension extension) throws ConfigException {
            for (AbstractSetup bridge : AbstractSetup.bridges) {
                AbstractTestFramework newTestFramework = bridge.getTestFramework(extension);
                if (newTestFramework != null) {
                    this.testFramework = newTestFramework;
                    return this;
                }
            }

            throw new ConfigException("No test framework class for " + extension.extensionClass + " found.");
        }

        /**
         * Adds a property to the builder after performing checks for matching entry points
         * in the available test jars. The method validates the provided {@code PropertyTest}
         * and ensures that it has a valid or resolved entry point before adding it to
         * the internal collection of properties.
         * The checks include:
         * <ul>
         * <li>Ensuring at least one exact match exists for the provided entry point.</li>
         * <li>Resolving potential candidates if an exact match is not found.</li>
         * <li>Handling scenarios with no matches or multiple candidates.</li>
         * </ul>
         * This method terminates execution with {@code System.exit} when certain conditions
         * are unmet (e.g., no matches or multiple matches are found for the entry point).
         *
         * @param propertyTest An instance of {@code PropertyTest} containing configuration
         *                     for the property, including the name and entry point to validate.
         * @return The {@code Builder} instance to allow method chaining.
         * @throws IndexOutOfBoundsException If the list of test jars is empty and thus no validation
         *                                   can be performed.
         * @throws RuntimeException If an I/O issue occurs when processing test jars or in cases
         *                          where unexpected scenarios arise despite the logic structure.
         */
        public Builder addPropertyWithCheck(PropertyTest propertyTest)  {
            if (testJars.isEmpty()) throw new IndexOutOfBoundsException("testJars must be set before calling this function.");

            String stringPropertyTest = resolveString("PropCov.TestPropertyEndpoint");
            if (stringPropertyTest != null) {
                propertyTest.name = resolveString("PropCov.TestPropertyName", "manualEndpoint");
                propertyTest.entryPoint = stringPropertyTest;
            }

            int foundExactMatch = 0;
            Set<String> possibleCandidates = new HashSet<>();

            for (Path testJar : testJars) {
                try {
                    Set<String> entryPoints = Util.getEntryPoints(testJar.toFile(), testFramework, analysisFramework).stream().map(m -> m.entryPoint).collect(Collectors.toSet());

                    // no entry points, so move on to the next file
                    if (entryPoints.isEmpty()) continue;

                    // do we have an exact match?  If so, track and continue.
                    if (entryPoints.contains(propertyTest.entryPoint)) {
                        foundExactMatch++;
                        continue;
                    }

                    // add possible candidates...
                    possibleCandidates.addAll(
                        entryPoints.stream()
                            .filter( m -> m.contains(propertyTest.entryPoint) )
                            .collect(Collectors.toSet())
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // found one exact match, so we are done
            if (foundExactMatch == 1) {
                this.properties.add(propertyTest);
                return this;
            }

            // no possible candidates found...
            if (possibleCandidates.isEmpty()) {
                throw new RuntimeException("*** NO MATCHES FOUND FOR " + propertyTest.entryPoint + " ***");
            }

            // found one non-exact match
            if (possibleCandidates.size() == 1) {
                this.properties.add(
                    new PropertyTest(
                        propertyTest.name,
                        possibleCandidates.toArray(new String[0])[0]
                    )
                );

                return this;
            }

            // found multiple candidates
            throw new RuntimeException("*** MULTIPLE MATCHES FOUND ***"
                + String.join("\n", possibleCandidates.toArray(new String[0])));

        }

        /**
         * Adds a {@link PropertyTest} instance to the current Builder instance.
         * The provided property is appended to the internal list of properties.
         *
         * @param propertyTest An instance of {@link PropertyTest} to add. Must not be null.
         * @return The current Builder instance, allowing for method chaining.
         */
        public Builder addProperty(PropertyTest propertyTest) {
            this.properties.add(propertyTest);
            return this;
        }

        /**
         * Adds a collection of {@link PropertyTest} instances to the current Builder instance.
         * If both "PropCov.TestPropertyName" and "PropCov.TestPropertyEndpoint" configuration values
         * are available, they will override the provided collection by creating a single {@link PropertyTest}
         * instance with these values.
         * <p>
         * Any added properties are validated to ensure they are not empty and are then processed individually.
         *
         * @param properties A collection of {@link PropertyTest} instances to add. If null or empty,
         *                   an exception will be thrown.
         *
         * @return This Builder instance, allowing for method chaining.
         */
        public Builder addProperties(Collection<PropertyTest> properties) {
            String testPropertyName = resolveString("PropCov.TestPropertyName");
            String testPropertyEndpoint = resolveString("PropCov.TestPropertyEndpoint");
            String testPropertySubProject = resolveString("PropCov.TestPropertySubProject");
            if (testPropertyName != null && testPropertyEndpoint != null) {
                if (testPropertySubProject == null) {
                    properties = List.of(new PropertyTest(testPropertyName, testPropertyEndpoint));
                } else {
                    properties = List.of(new PropertyTest(testPropertyName, testPropertyEndpoint, testPropertySubProject));
                }
            }

            validateNotEmpty("properties", properties);

            properties.forEach(this::addProperty);
            return this;
        }

        /**
         * Sets the output path for the current Builder instance. Resolves the provided
         * path using the property configuration and assigns it to the Builder.
         * Enables method chaining.
         *
         * @param outputPath The output path to set. This value may be resolved from a
         *                   configuration property if applicable. If null or empty,
         *                   a default value may be used.
         * @return This Builder instance with the output path configured, allowing
         *         method chaining.
         */
        public Builder outputPath(String outputPath) {
            outputPath = resolveString("PropCov.OutputPath", outputPath);
            this.outputPath = outputPath;
            return this;
        }

        /**
         * Adds a package name to the current Builder instance. The package name is resolved
         * to ensure proper formatting and then added to the Builder's configuration.
         *
         * @param packageName The name of the package to add. If null or empty, the
         *                    method resolves it the default value.
         * @return This Builder instance, allowing method chaining.
         */
        public Builder addPackage(String packageName) {
            packageName = resolveString("PropCov.PackageName", packageName);
            this.packages.add(packageName);
            return this;
        }

        /**
         * Adds multiple package names to the current Builder instance. The provided collection of package
         * names is resolved and split into individual entries, which are then added to the Builder's
         * configuration. Package names are processed to ensure the appropriate formatting and duplication handling.
         *
         * @param packageNames A collection of package names to add. If null, the method does nothing
         *                     and immediately returns the current Builder instance.
         * @return This Builder instance, allowing method chaining.
         */
        public Builder addPackages(Collection<String> packageNames) {
            Collection<String> resolvedList = Objects.requireNonNull(
                    resolveStringList(
                        "PropCov.PackageNames",
                        packageNames == null ? "" : String.join(",", packageNames),
                        ","
                    )
                );

            resolvedList.forEach(this::addPackage);
            return this;
        }

        /**
         * Sets the artifact directory for this Builder instance. Resolves the given
         * directory path using the property configuration and stores it in the
         * Builder instance. Enables method chaining.
         *
         * @param artifactDirectory The directory path where artifacts will be stored.
         *                          This can be a relative or absolute path. If null
         *                          or empty, a default value is resolved based on the
         *                          property configuration.
         * @return This Builder instance with the artifact directory configured.
         */
        public Builder artifactDirectory(String artifactDirectory) {
            artifactDirectory =  resolveString("PropCov.ArtifactDirectory", artifactDirectory);
            this.artifactDirectory = artifactDirectory;
            return this;
        }

        /**
         * Configures the coverage system for the current Builder instance based on the provided extension.
         * This method attempts to find the appropriate coverage class for the given extension by iterating
         * through available setup bridges. If no matching coverage is found, an exception is thrown.
         *
         * @param extension The extension containing the configuration details required to initialize the
         *                  coverage system. This includes properties and environment variables.
         * @return This Builder instance with the coverage system configured, allowing for method chaining.
         * @throws ConfigException If no matching coverage class is found for the given extension.
         */
        public Builder coverage(YAMLConfig.Extension extension) throws ConfigException {
            for (AbstractSetup bridge : AbstractSetup.bridges) {
                AbstractCoverage newCoverage = bridge.getCoverage(extension);
                if (newCoverage != null) {
                    this.coverage = newCoverage;
                    return this;
                }
            }

            throw new ConfigException("No coverage class for " + extension.extensionClass + " found.");
        }

        /**
         * Configures and initializes the build system for the current Builder instance. This method selects an
         * appropriate build system based on the provided extension. If no matching build system is found,
         * a {@link ConfigException} is thrown.
         *
         * @param extension The extension containing the configuration details required to initialize the build system.
         *                  This must include information such as properties and environmental variables.
         * @param timeouts  A map of timeout types to their respective durations in milliseconds.
         *                  This may be null, in which case an empty map will be used as the default.
         * @return This Builder instance with the configured build system, allowing for method chaining.
         * @throws ConfigException If no matching build system class is found for the given extension.
         */
        public Builder buildSystem(YAMLConfig.Extension extension, Map<TimeoutType, Long> timeouts) throws ConfigException {
            if (timeouts == null) timeouts = new HashMap<>();

            // set to -1 if we don't have this (quickcheck set to emit more details if this is not null)
            extension.env.putIfAbsent("OverrideNumOfTrials", "-1");

            if (source == null) {
                throw new NullPointerException("source is null");
            }

            if (!extension.properties.containsKey("LocalFile")) {
                extension.properties.put("LocalFile",
                        (subProject == null || subProject.isEmpty())
                        ? source.localDirectory.toString()
                        : source.localDirectory.resolve(subProject).toString());
            }

            for (AbstractSetup bridge : AbstractSetup.bridges) {
                AbstractBuildSystem newBuildSystem = bridge.getBuildSystem(extension, timeouts, project, subProject);
                if (newBuildSystem != null) {
                    this.buildSystem = newBuildSystem;
                    return this;
                }
            }

            throw new ConfigException("No build system class for " + extension.extensionClass + " found.");
        }

        /**
         * Adds multiple timeout values for the specified timeout types in the configuration.
         * This method iterates through the provided map and updates the timeout settings for
         * each timeout type. Returns the Builder instance, allowing for method chaining.
         *
         * @param timeouts A map where keys represent the timeout types and values represent
         *                 the corresponding timeout durations in seconds. If the map
         *                 is null, the method does nothing and returns the current Builder instance.
         * @return This Builder instance with the updated timeout configurations.
         */
        public Builder addTimeouts(Map<TimeoutType, Long> timeouts) {
            if (timeouts == null) return this;

            timeouts.forEach(this::addTimeout);
            return this;
        }

        /**
         * Adds a timeout value for a specified timeout type in the configuration.
         * This method updates the timeout mapping and returns the Builder instance,
         * allowing for method chaining.
         *
         * @param timeoutType The type of timeout to set. Represents different timeout
         *                    scenarios such as patching, cleaning, building, etc.
         * @param timeout     The timeout duration in milliseconds for the specified type.
         * @return This Builder instance with the updated timeout configuration.
         */
        public Builder addTimeout(TimeoutType timeoutType, long timeout) {
            timeouts.put(timeoutType, timeout);
            return this;
        }

        /**
         * Retrieves an unmodifiable map of timeout configurations.
         *
         * @return a map where keys represent timeout types and values represent their corresponding timeout durations in milliseconds
         */
        public Map<TimeoutType, Long> getTimeouts() {
            return Collections.unmodifiableMap(timeouts);
        }

        /**
         * Builds and returns a Config object based on the current Builder's state and the provided YAML configuration.
         * The method processes timeouts and validates required configurations before constructing the Config instance.
         *
         * @param storedYamlConfig the YAML configuration containing additional properties for the Config object
         * @return a Config object initialized with the current Builder state and the provided YAML configuration
         * @throws NullPointerException if the analysisFramework is not set before invoking this method
         */
        public Config build(YAMLConfig storedYamlConfig)  {
            // Set timeouts using provided properties
            for (TimeoutType value : TimeoutType.values()) {
                Long timeoutValue = resolveLong("PropCov.Timeout." + value.name());
                if (timeoutValue != null) timeouts.put(value, timeoutValue);
            }

            if (analysisFramework == null) throw new NullPointerException("analysisFramework is not set when building Config object.");

            return new Config(this, storedYamlConfig);
        }

        /**
         * Configures the analysis framework for this builder instance based on the provided extension.
         * Attempts to resolve a compatible analysis framework by iterating through available setup bridges.
         * Throws an exception if no matching framework is found.
         *
         * @param extension The extension containing details required to initialize the analysis framework.
         * @return This Builder instance, allowing for method chaining.
         * @throws ConfigException If no matching analysis framework class is found for the given extension.
         */
        public Builder analysisFramework(YAMLConfig.Extension extension) throws ConfigException {
            for (AbstractSetup bridge : AbstractSetup.bridges) {
                AbstractAnalysisFramework newAnalysisFramework = bridge.getAnalysisFramework(extension);
                if (newAnalysisFramework != null) {
                    this.analysisFramework = newAnalysisFramework;
                    return this;
                }
            }

            throw new ConfigException("No analysis framework class for " + extension.extensionClass + " found.");
        }

        protected static void validateNotEmpty(String name, Object value) throws IllegalArgumentException {
            if (value == null) throw new IllegalArgumentException(name + " must not be null");

            if (value instanceof String) {
                if (((String) value).isEmpty()) throw new IllegalArgumentException(name + " must be at least 1 character");
            }
        }
    }
}