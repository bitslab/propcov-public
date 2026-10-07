package edu.uic.bitslab.propcov.core.analysisframework;

import edu.uic.bitslab.propcov.core.AbstractSetup;
import edu.uic.bitslab.propcov.core.Setup;
import edu.uic.bitslab.propcov.core.buildsystem.AbstractBuildSystem;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.ConfigException;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.source.AbstractSource;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scala.Tuple2;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AbstractAnalysisFrameworkTest {
    Config config;
    Config configWithFlags;
    Config configMissingAnalysisFlags;

    @BeforeEach
    void setUp() throws ConfigException {
        Setup.load();
        AbstractSetup.bridges.add(new SetupTest());
        config = new Config.Builder("Test")
                .analysisFramework(
                        new YAMLConfig.Extension(
                                "Test",
                                Map.of("AnalysisType", "CHA", "AnalysisFlags", ""),
                                Map.of())
                )
                .build(null);

        configMissingAnalysisFlags = new Config.Builder("Test")
                .analysisFramework(
                        new YAMLConfig.Extension(
                                "Test",
                                Map.of("AnalysisType", "CHA"),
                                Map.of())
                )
                .build(null);

        configWithFlags = new Config.Builder("Test")
                .analysisFramework(
                        new YAMLConfig.Extension(
                                "Test",
                                Map.of("AnalysisType", "CHA", "AnalysisFlags", "ITERATOR"),
                                Map.of())
                )
                .build(null);
    }

    @Test
    void testBuildCallGraph_NormalInput_ReturnsGraph() throws Exception {
        PropertyTest propertyTest = new PropertyTest("TestProperty", "test.EntryPoint");
        Graph<String, DefaultEdge> graph = config.analysisFramework.BuildCallGraph(config, propertyTest);
        assertNotNull(graph, "The generated graph should not be null.");
        assertTrue(graph.vertexSet().contains("test.EntryPoint"), "The graph should contain the entry point node.");
    }

    @Test
    void testBuildCallGraph_NormalInput_ReturnsGraph_MissingFlags() throws Exception {
        PropertyTest propertyTest = new PropertyTest("TestProperty", "test.EntryPoint");
        Graph<String, DefaultEdge> graph = configMissingAnalysisFlags.analysisFramework.BuildCallGraph(configMissingAnalysisFlags, propertyTest);
        assertNotNull(graph, "The generated graph should not be null.");
        assertTrue(graph.vertexSet().contains("test.EntryPoint"), "The graph should contain the entry point node.");
    }

    @Test
    void testBuildCallGraph_NormalInput_ReturnsGraph_WithFlags() throws Exception {
        PropertyTest propertyTest = new PropertyTest("TestProperty", "test.EntryPoint");
        Graph<String, DefaultEdge> graph = configWithFlags.analysisFramework.BuildCallGraph(configWithFlags, propertyTest);
        assertNotNull(graph, "The generated graph should not be null.");
        assertTrue(graph.vertexSet().contains("test.EntryPoint"), "The graph should contain the entry point node.");
    }

    @Test
    void testBuildCallGraph_NullPropertyTest_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> config.analysisFramework.BuildCallGraph(config, null),
                "Passing a null PropertyTest should throw an exception.");
    }

    @Test
    void testBuildCallGraph_ValidInputEmptyGraph() throws Exception {
        PropertyTest propertyTest = new PropertyTest("EmptyGraphProperty", "test.EmptyEntryPoint");
        Graph<String, DefaultEdge> graph = config.analysisFramework.BuildCallGraph(config, propertyTest);
        assertNotNull(graph, "The generated graph should not be null.");
        assertTrue(graph.vertexSet().isEmpty(), "The graph should be empty for unsupported entry point.");
    }

    // A simple test implementation of AbstractAnalysisFramework for testing purposes
    private static class TestAnalysisFramework extends AbstractAnalysisFramework {

        public TestAnalysisFramework(YAMLConfig.Extension extension) {
            super(extension, "test");
        }

        @Override
        public Graph<String, DefaultEdge> BuildCallGraph(Config config, PropertyTest propertyTest) {
            if (config == null || propertyTest == null || propertyTest.entryPoint == null) {
                throw new IllegalArgumentException("Config or PropertyTest cannot be null.");
            }

            // Simulating a call graph generation
            Graph<String, DefaultEdge> graph = new org.jgrapht.graph.DefaultDirectedGraph<>(DefaultEdge.class);
            if (propertyTest.entryPoint.equals("test.EntryPoint")) {
                graph.addVertex("test.EntryPoint");
            }
            return graph;
        }

        @Override
        public List<Tuple2<String, String>> GetProperties(InputStream inputStream, AbstractTestFramework testFramework) {
            return List.of();
        }
    }

    static class SetupTest extends AbstractSetup {
        @Override
        public AbstractSource getSource(YAMLConfig.Extension extension, String projectName) throws SourceException {
            return null;
        }

        @Override
        public AbstractTestFramework getTestFramework(YAMLConfig.Extension extension) {
            return null;
        }

        @Override
        public AbstractCoverage getCoverage(YAMLConfig.Extension extension) {
            return null;
        }

        @Override
        public AbstractAnalysisFramework getAnalysisFramework(YAMLConfig.Extension extension) {
            return switch (extension.extensionClass) {
                case "Test" -> new TestAnalysisFramework(extension);
                default -> null;
            };
        }

        @Override
        public AbstractBuildSystem getBuildSystem(YAMLConfig.Extension extension, Map<Config.TimeoutType, Long> timeouts, String projectName, String subProjectName) {
            return null;
        }
    }
}