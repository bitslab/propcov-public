package edu.uic.bitslab.propcov.core.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.config.*;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.util.timer.RunTimer;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArtifactTest {
    private Path tempDir;
    private Config.Builder configBuilder;

    @BeforeEach
    public void setup() throws IOException, ConfigException, SourceException {
        new edu.uic.bitslab.propcov.core.Setup().init();
        new Mocks.Setup().init();

        tempDir = Files.createTempDirectory("ArtifactTest");

        configBuilder = Mocks.getMockConfigBuilder()
                .artifactDirectory(tempDir.resolve("artifact").toString())
                .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of("LocalDirectory", tempDir.toString()), Map.of()), Map.of())
                .testFramework(new YAMLConfig.Extension("TestFrameworkTest", Map.of(), Map.of()))
                .coverage(new YAMLConfig.Extension("CoverageTest", Map.of(), Map.of()));
    }

    @AfterEach
    public void teardown() throws IOException {
        if (tempDir != null) {
            Util.recursiveRemove(tempDir);
            tempDir = null;
        }

        configBuilder = null;
    }

    @Test
    void beforeReports() throws TimerException, IOException {
        Config config = configBuilder.build(null);
        Artifact artifact = Artifact.CreateArtifact(config);
        artifact.beforeReports();

        Path coverageDetailsSer = artifact.fullArtifactPath.resolve("REPORTS").resolve("coverage_details.ser");
        assertTrue(Files.exists(coverageDetailsSer));

        Path coverageDetailsJson = artifact.fullArtifactPath.resolve("REPORTS").resolve("coverage_details.json");
        assertTrue(Files.exists(coverageDetailsJson));

    }

    @SuppressWarnings("unchecked")
    @Test
    void metadata() throws TimerException, IOException {
        Config config = configBuilder.build(null);
        Artifact artifact = Artifact.CreateArtifact(config);
        artifact.setCoverageData(new PropertyTest("A", "B"), CoverageData.CoverageDataType.Coverage, new CoverageData(Map.of("A", new CoverageDetail.Builder().build())));
        artifact.setCoverageData(new PropertyTest("A", "B"), CoverageData.CoverageDataType.PropCov, new CoverageData(Map.of("A", new CoverageDetail.Builder().build())));
        artifact.addError(new Exception("test"));
        artifact.addError(new Exception());
        artifact.addError(new Exception("test2", new Exception("cause")));
        artifact.setLabel("my-label");
        artifact.initMetaData("A");
        artifact.addPropertyError("A", new Exception("PropertyTest"));
        artifact.complete();

        Path metadata = artifact.fullArtifactPath.resolve("REPORTS").resolve("metadata.json");
        assertTrue(Files.exists(metadata));

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = Files.newInputStream(metadata);
        Map<String, Object> json = (Map<String, Object>) mapper.readValue(is, Map.class);

        assertEquals("my-label", json.get("label"));
        assertNull(json.get("subProject"));

        List<String> errors = (List<String>) json.get("errors");
        assertEquals(3, errors.size());
        assertTrue(errors.stream().anyMatch(error -> error.startsWith("test: Exception: test\n")));
        assertTrue(errors.stream().anyMatch(error -> error.startsWith(": Exception: \n")));
        assertTrue(errors.stream().anyMatch(error -> error.startsWith("test2: Exception: cause\n")));

        assertTrue((boolean) ((Map<?, ?>) ((Map<?, ?>) json.get("propertyTest")).get("A")).get("error"));
        assertTrue(((Map<?, ?>) ((Map<?, ?>) json.get("propertyTest")).get("A")).get("errorMsg").toString().startsWith("PropertyTest: Exception: PropertyTest\n"));
    }

    @Test
    void addTests() throws TimerException, IOException {
        Config config = configBuilder.build(null);
        Artifact artifact = Artifact.CreateArtifact(config);
        artifact.setCoverageData(new PropertyTest("A", "B"), CoverageData.CoverageDataType.Coverage, new CoverageData(Map.of("A", new CoverageDetail.Builder().build())));
        artifact.setCoverageData(new PropertyTest("A", "B"), CoverageData.CoverageDataType.PropCov, new CoverageData(Map.of("A", new CoverageDetail.Builder().build())));

        assertThrows(IOException.class, () -> artifact.addTests(new Path[]{ tempDir.resolve("target1"), tempDir.resolve("target2") }));

        artifact.setSubFolder(Path.of(""));

        artifact.addTest("HELLO".getBytes(StandardCharsets.UTF_8), "testTarget");
        Path test1 = artifact.fullArtifactPath.resolve("TESTS").resolve("testTarget");
        assertTrue(Files.exists(test1));
        assertEquals("HELLO", Files.readString(test1));

        Path[] testPaths = new Path[]{
                tempDir.resolve("file2"),
                tempDir.resolve("file3")
        };
        for (Path testPath : testPaths) {
            Files.writeString(testPath, "GOODBYE "+testPath.getFileName());
        }

        artifact.setSubFolder(Path.of("target2"));
        artifact.addTests(testPaths);

        Path test2 = artifact.fullArtifactPath.resolve("TESTS").resolve("target2").resolve("file2");
        assertTrue(Files.exists(test2));
        assertEquals("GOODBYE file2", Files.readString(test2));

        Path test3 = artifact.fullArtifactPath.resolve("TESTS").resolve("target2").resolve("file3");
        assertTrue(Files.exists(test3));
        assertEquals("GOODBYE file3", Files.readString(test3));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addTimes() throws TimerException, IOException {
        Config config = configBuilder.build(null);
        Artifact artifact = Artifact.CreateArtifact(config);
        artifact.setCoverageData(new PropertyTest("A", "B"), CoverageData.CoverageDataType.Coverage, new CoverageData(Map.of("A", new CoverageDetail.Builder().build())));
        artifact.setCoverageData(new PropertyTest("A", "B"), CoverageData.CoverageDataType.PropCov, new CoverageData(Map.of("A", new CoverageDetail.Builder().build())));
        artifact.setSubFolder(Path.of(""));
        artifact.initMetaData("A");

        Logger logger = mock(Logger.class);
        RunTimer timer = mock(RunTimer.class, withSettings().useConstructor("Time1", logger));
        when(timer.elapsedInSeconds()).thenReturn(1.23456789);
        artifact.addPropertyElapsedTime("A", timer);

        artifact.addPropertyElapsedTime("A", "Time2", 2.3456789);

        artifact.addElapsedTime("Time3", 3.456789);

        RunTimer timer2 = mock(RunTimer.class, withSettings().useConstructor("Time4", logger));
        when(timer2.elapsedInSeconds()).thenReturn(4.56789012);
        artifact.addElapsedTime(timer2);

        artifact.complete();
        Path metadata = artifact.fullArtifactPath.resolve("REPORTS").resolve("metadata.json");
        assertTrue(Files.exists(metadata));

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = Files.newInputStream(metadata);
        Map<String, Object> json = (Map<String, Object>) mapper.readValue(is, Map.class);
        assertNotNull(json);

        Map<String, Double> timesCommon = (Map<String, Double>) json.get("timesCommon");
        assertNotNull(timesCommon);
        assertEquals(3.456789, timesCommon.get("Time3"));
        assertEquals(4.56789012, timesCommon.get("Time4"));

        Map<String, Double> propertyTimes = (Map<String, Double>) ((Map<?, ?>) ((Map<?, ?>) json.get("propertyTest")).get("A")).get("propertyTimes");
        assertEquals(1.23456789, propertyTimes.get("Time1"));
        assertEquals(2.3456789, propertyTimes.get("Time2"));
    }
}