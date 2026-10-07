package edu.uic.bitslab.propcov.core.config;

import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.source.SourceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.uic.bitslab.propcov.core.config.YAMLConfigTest.pathV1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;

/**
 * Test of Config
 */
public class ConfigTest {
    PropertyTest propertyTest1;
    PropertyTest propertyTest2;

    @BeforeEach @AfterEach
    void cleanSlate() {
        System.getProperties().forEach((k, v) -> {
            if (k.toString().toLowerCase().contains("propcov.")) {
                System.clearProperty(k.toString());
            }
        });

        propertyTest1 = new PropertyTest("TestProperty", "test.EntryPoint");
        propertyTest2 = new PropertyTest("TestProperty2", "test.EntryPoint2");
    }

    @BeforeEach
    void setUp() {
        new edu.uic.bitslab.propcov.core.Setup().init();
        new Mocks.Setup().init();
    }

    @Test
    void builderTest() throws ConfigException, SourceException {
        Config.Builder configBuilder = new Config
            .Builder("A")
            .subProject("B")
            .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
            .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of(), Map.of()), Map.of())
            .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA", "AnalysisFlag", ""), Map.of()))
            .testFramework(new YAMLConfig.Extension("TestFrameworkTest", Map.of(), Map.of()))
            .coverage(new YAMLConfig.Extension("CoverageTest", Map.of(), Map.of()))
            .addTestJars(List.of("TEST.JAR"))
            .pathImprovementColors("RED","GREEN", "BLUE")
            .pathDefaultColor("PURPLE")
            .nodeColor(Config.NodeType.COVERAGE4, "YELLOW")
            .patchFile("patch.diff")
            .workflowRemove(null)
            .workflowReplace(null)
            .addProperty(propertyTest1)
            .addProperties(List.of(propertyTest2))
            .outputPath("output")
            .addPackage("test.package")
            .addPackages(List.of("test.package2"))
            .artifactDirectory("artifactDirectory")
            .addTimeout(Config.TimeoutType.buildCallgraph, 1000L)
            .addTimeouts(Map.of(Config.TimeoutType.buildSUT, 120L))
            .addMainJars(List.of("MAIN.JAR"))
            .addMainJarsWithDependencies(List.of("MAIN_WITH_DEPENDENCIES.JAR"));

        Map<Config.TimeoutType, Long> timeouts = configBuilder.getTimeouts();
        assertEquals(1000L, timeouts.get(Config.TimeoutType.buildCallgraph));
        assertEquals(120L, timeouts.get(Config.TimeoutType.buildSUT));

        // Config1
        System.setProperty("PropCov.Timeout.buildCallgraph", "1000");
        Config config1 = configBuilder.build(null);
        assertEquals(1000L, config1.timeouts.get(Config.TimeoutType.buildCallgraph));
        assertEquals("A", config1.project);
        assertEquals("B", config1.subProject);
        assertTrue(config1.testJars[0].toString().endsWith("/TEST.JAR"));
        assertTrue(config1.patchFile.endsWith("patch.diff"));
        assertEquals("RED", config1.pathImprovementColors[0]);
        assertEquals("GREEN", config1.pathImprovementColors[1]);
        assertEquals("BLUE", config1.pathImprovementColors[2]);
        assertEquals("PURPLE", config1.pathDefaultColor);
        assertEquals("YELLOW", config1.nodeColor[Config.NodeType.COVERAGE4.ordinal()]);
        assertEquals(2, config1.properties.size());
        assertTrue(config1.properties.contains(propertyTest1));
        assertTrue(config1.properties.contains(propertyTest2));
        assertEquals("output", config1.outputPath);
        assertEquals(2, config1.packages.size());
        assertTrue(config1.packages.contains("test.package"));
        assertTrue(config1.packages.contains("test.package2"));
        assertEquals("artifactDirectory", config1.artifactDirectory);
        assertEquals(1000L, config1.timeouts.get(Config.TimeoutType.buildCallgraph));
        assertEquals(120L, config1.timeouts.get(Config.TimeoutType.buildSUT));
        assertTrue(config1.mainJars[0].toString().endsWith("/MAIN.JAR"));
        assertTrue(config1.mainJarsWithDependencies[0].toString().endsWith("/MAIN_WITH_DEPENDENCIES.JAR"));

        // Config2
        Set<Config.WorkflowItem> workflowItems1 = Set.copyOf(config1.workflow);
        Config.WorkflowItem workflowItem = workflowItems1.iterator().next();
        Config config2 = configBuilder
                .workflowRemove(List.of(workflowItem))
                .build(null);

        assertEquals(workflowItems1.size() - 1, config2.workflow.size());
        assertFalse(config2.workflow.contains(workflowItem));


        // Config3
        Config config3 = configBuilder
            .workflowReplace(List.of(Config.WorkflowShortName.git.workflowItems))
            .workflowReplace(List.of(Config.WorkflowShortName.fetch.workflowItems))
            .workflowReplace(List.of(Config.WorkflowShortName.build.workflowItems))
            .workflowReplace(List.of(Config.WorkflowShortName.test.workflowItems))
            .build(null);

        assertEquals(Config.WorkflowShortName.test.workflowItems.length, config3.workflow.size());
        assertTrue(config3.workflow.containsAll(List.of(Config.WorkflowShortName.test.workflowItems)));

        // toYaml of config3
        String yaml = config3.toYaml();
        assertTrue(yaml.contains("version: 1"));
    }

    @Test
    void builderTest2() throws ConfigException, SourceException {
        Config.Builder configBuilder = new Config.Builder("A");

        assertThrows(IllegalArgumentException.class, () -> configBuilder.addMainJars(null));
        assertThrows(IllegalArgumentException.class, () -> configBuilder.addMainJars(List.of("")));

        assertThrows(NullPointerException.class, () -> configBuilder.build(null));

        configBuilder
                .addMainJarsWithDependencies(null)
                .addMainJarsWithDependencies(List.of(""))
                .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA", "AnalysisFlags", ""), Map.of()));
        assertThrows(NullPointerException.class, () -> configBuilder.build(null).toYaml());

        configBuilder.source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()));
        assertThrows(NullPointerException.class, () -> configBuilder.build(null).toYaml());

        configBuilder.buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of(), Map.of()), Map.of());
        assertThrows(NullPointerException.class, () -> configBuilder.build(null).toYaml());

        configBuilder.testFramework(new YAMLConfig.Extension("TestFrameworkTest", Map.of(), Map.of()));
        assertThrows(NullPointerException.class, () -> configBuilder.build(null).toYaml());

        configBuilder.coverage(new YAMLConfig.Extension("CoverageTest", Map.of(), Map.of()));
        String yaml = configBuilder.build(null).toYaml();
        assertTrue(yaml.contains("version: 1"));
    }

    @Test
    void notFound() throws Exception{
        assertTrue(assertThrows(ConfigException.class, () -> new Config.Builder("A").testFramework(new YAMLConfig.Extension("Fake", Map.of(), Map.of()))).getMessage().contains("No test framework class for Fake found."));
        assertTrue(assertThrows(ConfigException.class, () -> new Config.Builder("A").source(new YAMLConfig.Extension("Fake", Map.of(), Map.of()))).getMessage().contains("No source class for Fake found."));
        assertTrue(assertThrows(ConfigException.class, () -> new Config.Builder("A").coverage(new YAMLConfig.Extension("Fake", Map.of(), Map.of()))).getMessage().contains("No coverage class for Fake found."));
        assertTrue(assertThrows(NullPointerException.class, () -> new Config.Builder("A").buildSystem(new YAMLConfig.Extension("Fake", Map.of(), Map.of()), Map.of())).getMessage().contains("source is null"));

        assertTrue(
            assertThrows(ConfigException.class, () -> new Config.Builder("A")
                .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
                .buildSystem(new YAMLConfig.Extension("Fake", Map.of(), Map.of()), Map.of())
            ).getMessage().contains("No build system class for Fake found.")
        );

        assertTrue(
                assertThrows(ConfigException.class, () -> new Config.Builder("A")
                        .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
                        .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of(), Map.of()), Map.of())
                        .analysisFramework(new YAMLConfig.Extension("Fake", Map.of(), Map.of()))
                ).getMessage().contains("No analysis framework class for Fake found.")
        );

        new Config.Builder("A")
                .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
                .addTimeouts(null)
                .addPackages(null)
                .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA", "AnalysisFlags", ""), Map.of()))
                .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of("LocalFile", "FakeFile"), Map.of()), null);

    }

    @Test
    void usingStoredYAML() throws IOException, ConfigException, SourceException {
        YAMLConfig source = YAMLConfig.loadFromFile(pathV1);
        Config config = new Config.Builder("A")
                .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
                .addTimeouts(null)
                .addPackages(null)
                .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA", "AnalysisFlags", ""), Map.of()))
                .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of("LocalFile", "FakeFile"), Map.of()), null)
                .build(source);
        assertNotNull(config.storedYAMLConfig);
        String yaml = config.toYaml();
        assertTrue(yaml.contains("version: 1"));
    }

    @Test
    void guessSUTPath() throws ConfigException, SourceException {
        Config.Builder builder = new Config.Builder("A");
        Path absolute = Path.of("/tmp");
        assertEquals(absolute, builder.guessSUTPath(absolute));

        assertTrue(
            assertThrows(NullPointerException.class, () -> {
               Path relative = Path.of("relative");
               builder.guessSUTPath(relative);
            })
            .getMessage().contains("source is null")
        );

        builder.source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()));
        assertTrue(
            assertThrows(NullPointerException.class, () -> {
                Path relative = Path.of("relative");
                builder.guessSUTPath(relative);
            })
            .getMessage().contains("buildSystem is null")
        );

        builder.buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of("LocalFile", "FakeFile"), Map.of()), null);
        builder.source(new YAMLConfig.Extension("SourceTest", Map.of("Directory", ""), Map.of()));
        assertTrue(
                assertThrows(IndexOutOfBoundsException.class, () -> {
                    Path relative = Path.of("relative");
                    builder.guessSUTPath(relative);
                })
                .getMessage().contains("source.localDirectory is empty")
        );

        builder.buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of("TargetPath", "/tmp"), Map.of()), null);
        builder.source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()));
        assertEquals(Path.of("../propcov-sut/A/./relative"), builder.guessSUTPath("./relative"));
        assertEquals(Path.of("/tmp/relative"), builder.guessSUTPath("relative"));

        builder.subProject("");
        assertEquals(Path.of("/tmp/relative"), builder.guessSUTPath("relative"));

        builder.buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of("TargetPath", "/tmp/B"), Map.of()), null);
        assertEquals(Path.of("/tmp/B/relative"), builder.guessSUTPath("relative"));
    }

    @Test
    void testAddPropertyWithCheck_ValidProperty() throws ConfigException, SourceException {
        Config.Builder builder = new Config
            .Builder("MockProject")
            .testFramework(new YAMLConfig.Extension("TestFrameworkTest", Map.of(), Map.of()))
            .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA"), Map.of()));

        assertTrue(
            assertThrows(IndexOutOfBoundsException.class, () -> builder.addPropertyWithCheck(propertyTest1) )
                    .getMessage().contains("testJars must be set before calling this function.")
        );

        builder
            .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
            .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of(), Map.of()), Map.of())
            .addTestJars(List.of("test-jar.jar"));

        // single
        try (MockedStatic<Util> utilMock = mockStatic(Util.class)) {
            utilMock
                    .when(() -> Util.getEntryPoints(any(), any(), any()))
                    .thenReturn(List.of(propertyTest1));

            builder.addPropertyWithCheck(propertyTest1);
            Config config1 = builder.build(null);
            assertTrue(config1.properties.contains(propertyTest1));
        }

        // multiple
        try (MockedStatic<Util> utilMock = mockStatic(Util.class)) {
            utilMock
                    .when(() -> Util.getEntryPoints(any(), any(), any()))
                    .thenReturn(List.of(propertyTest1, propertyTest2));

            builder.addPropertyWithCheck(propertyTest1);
            builder.addPropertyWithCheck(propertyTest2);

            Config config = builder.build(null);
            assertTrue(config.properties.contains(propertyTest1));
            assertTrue(config.properties.contains(propertyTest2));
        }

        // no match
        try (MockedStatic<Util> utilMock = mockStatic(Util.class)) {
            utilMock
                    .when(() -> Util.getEntryPoints(any(), any(), any()))
                    .thenReturn(List.of());

            assertTrue(
                    assertThrows(RuntimeException.class, () -> builder.addPropertyWithCheck(propertyTest1))
                            .getMessage().contains("NO MATCHES FOUND FOR")
            );
        }

        // partial match
        try (MockedStatic<Util> utilMock = mockStatic(Util.class)) {
            utilMock
                    .when(() -> Util.getEntryPoints(any(), any(), any()))
                    .thenReturn(List.of(propertyTest1));

            builder.addPropertyWithCheck(new PropertyTest("ValidTest1", "EntryPoint"));

            Config config = builder.build(null);
            assertTrue(config.properties.contains(propertyTest1));
        }

        // multiple match
        try (MockedStatic<Util> utilMock = mockStatic(Util.class)) {
            utilMock
                    .when(() -> Util.getEntryPoints(any(), any(), any()))
                    .thenReturn(List.of(propertyTest1, propertyTest2));

            assertTrue(
                    assertThrows(RuntimeException.class, () -> builder.addPropertyWithCheck(new PropertyTest("ValidTest", "EntryPoint")))
                            .getMessage().contains("MULTIPLE MATCHES FOUND")
            );
        }
    }

    @Test
    void overrideProperty() throws ConfigException, SourceException {
        Config.Builder builder = new Config
                .Builder("A")
                .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
                .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of(), Map.of()), Map.of())
                .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA"), Map.of()))
                .addTestJars(List.of("test-jar.jar"));

        // property override
        try (MockedStatic<Util> utilMock = mockStatic(Util.class)) {
            utilMock
                    .when(() -> Util.getEntryPoints(any(), any(), any()))
                    .thenReturn(List.of(propertyTest1));

            System.setProperty("PropCov.TestPropertyEndpoint", "Tester");

            builder.addPropertyWithCheck(propertyTest1);
            Config config1 = builder.build(null);
            assertEquals(1, config1.properties.size());
            PropertyTest propertyTest = config1.properties.iterator().next();
            assertEquals("manualEndpoint", propertyTest.name);
            assertEquals("Tester", propertyTest.entryPoint);
        }
    }

    @Test
    void setWithProperties() throws ConfigException, SourceException {
        System.setProperty("PropCov.TestPropertyName", "test.name");
        System.setProperty("PropCov.TestPropertyEndpoint", "test.value");

        Config.Builder configBuilder = new Config
                .Builder("A")
                .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
                .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of(), Map.of()), Map.of())
                .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA"), Map.of()))
                .addTestJars(List.of("test-jar.jar"))
                .addProperties(List.of(new PropertyTest("a", "b")));
        Config config = configBuilder.build(null);
        assertEquals(1, config.properties.size());
        PropertyTest propertyTest = config.properties.stream().findAny().orElse(null);
        assertNotNull(propertyTest);
        assertEquals("test.name", propertyTest.name);
        assertEquals("test.value", propertyTest.entryPoint);
    }

    @Test
    void setWithPropertiesNullName() throws ConfigException, SourceException {
        System.setProperty("PropCov.TestPropertyEndpoint", "test.value");

        Config.Builder configBuilder = new Config
                .Builder("A")
                .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
                .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of(), Map.of()), Map.of())
                .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA"), Map.of()))
                .addTestJars(List.of("test-jar.jar"))
                .addProperties(List.of(new PropertyTest("a", "b")));
        Config config = configBuilder.build(null);
        assertEquals(1, config.properties.size());
        PropertyTest propertyTest = config.properties.stream().findAny().orElse(null);
        assertNotNull(propertyTest);
        assertEquals("a", propertyTest.name);
        assertEquals("b", propertyTest.entryPoint);
    }

    @Test
    void setWithPropertiesNullEndpoint() throws ConfigException, SourceException {
        System.setProperty("PropCov.TestPropertyName", "test.name");

        Config.Builder configBuilder = new Config
                .Builder("A")
                .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
                .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of(), Map.of()), Map.of())
                .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA"), Map.of()))
                .addTestJars(List.of("test-jar.jar"))
                .addProperties(List.of(new PropertyTest("a", "b")));
        Config config = configBuilder.build(null);
        assertEquals(1, config.properties.size());
        PropertyTest propertyTest = config.properties.stream().findAny().orElse(null);
        assertNotNull(propertyTest);
        assertEquals("a", propertyTest.name);
        assertEquals("b", propertyTest.entryPoint);
    }



}
