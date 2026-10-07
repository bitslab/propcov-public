package edu.uic.bitslab.propcov.core.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


class ConverterTest {
    static final String pathV0;
    static final String pathV0b;
    static final String pathV99999;

    @TempDir
    private Path tempDir;

    static {
        ClassLoader classLoader = ConverterTest.class.getClassLoader();
        try {
            pathV0 = Path.of(Objects.requireNonNull(classLoader.getResource("v0.yaml")).toURI()).toString();
            pathV0b = Path.of(Objects.requireNonNull(classLoader.getResource("v0-b.yaml")).toURI()).toString();
            pathV99999 = Path.of(Objects.requireNonNull(classLoader.getResource("v99999.yaml")).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream errStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outStreamCaptor));
        System.setErr(new PrintStream(errStreamCaptor));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void noArgs() throws IOException {
        Converter.setExitHandler(exitCode -> {});
        String[] args = {};
        Converter.main(args);
        assertTrue(errStreamCaptor.toString().contains("Usage: Converter <source_path> <target_path>"));
    }

    @Test
    void v0Test() throws IOException {
        String target = tempDir.resolve("v1-target.yaml").toString();

        String[] args = {
                pathV0,
                target
        };
        Converter.main(args);

        YAMLConfig v1 = YAMLConfig.loadFromFile(target);
        assertEquals("test project", v1.name);
        assertEquals("core", v1.subProject);
        assertEquals("edu.uic.bitslab.propcov.extensions.source.Git", v1.source.extensionClass);
        assertEquals("https://foo/bar/test.git", v1.source.properties.get("URL"));
        assertEquals("01e09ee1424be08ec70f3787874118d6638cc63d", v1.source.properties.get("checkoutID"));
        assertEquals("artifacts/configs/foo/foo.patch", v1.patchName);
        assertEquals("foo.jar", v1.mainJar.get(0));
        assertEquals("foo-with-dependencies.jar", v1.mainJarWithDependencies.get(0));
        assertEquals("foo-tests.jar", v1.testJar.get(0));
        assertEquals(1, v1.properties.size());
        assertEquals("Foo#PropOne", v1.properties.get(0).name);
        assertEquals("foo.bar.PropOne()V", v1.properties.get(0).entryPoint);
        assertEquals("edu.uic.bitslab.propcov.extensions.buildsystem.Maven", v1.buildSystem.extensionClass);
        assertEquals("edu.uic.bitslab.propcov.extensions.testframework.JunitQuickCheck", v1.testFramework.extensionClass);
        assertEquals("edu.uic.bitslab.propcov.extensions.coverage.JaCoCo", v1.coverage.extensionClass);
        assertEquals("edu.uic.bitslab.propcov.extensions.analysisframework.Opal", v1.analysisFramework.extensionClass);
        assertEquals(0, v1.analysisFramework.properties.size());

        assertThrows(IllegalArgumentException.class, () -> Converter.main(args));
    }

    @Test
    void v0TestB() throws IOException {
        String target = tempDir.resolve("v1-b-target.yaml").toString();

        String[] args = {
                pathV0b,
                target
        };
        Converter.main(args);

        YAMLConfig v1 = YAMLConfig.loadFromFile(target);
        assertEquals("test project", v1.name);
        assertEquals("core", v1.subProject);
        assertEquals("edu.uic.bitslab.propcov.extensions.source.Git", v1.source.extensionClass);
        assertEquals("https://foo/bar/test.git", v1.source.properties.get("URL"));
        assertEquals("01e09ee1424be08ec70f3787874118d6638cc63d", v1.source.properties.get("checkoutID"));
        assertEquals("artifacts/configs/foo/foo.patch", v1.patchName);
        assertEquals("foo.jar", v1.mainJar.get(0));
        assertEquals("foo-with-dependencies.jar", v1.mainJarWithDependencies.get(0));
        assertEquals("foo-tests.jar", v1.testJar.get(0));
        assertEquals(1, v1.properties.size());
        assertEquals("Foo#PropOne", v1.properties.get(0).name);
        assertEquals("foo.bar.PropOne()V", v1.properties.get(0).entryPoint);
        assertEquals("edu.uic.bitslab.propcov.extensions.buildsystem.Maven", v1.buildSystem.extensionClass);
        assertEquals(1, v1.buildSystem.properties.size());
        assertEquals("bar_opt", v1.buildSystem.properties.get("fooOpt"));
        assertEquals(1, v1.buildSystem.env.size());
        assertEquals("bar_env", v1.buildSystem.env.get("fooEnv"));
        assertEquals("testFrw", v1.testFramework.extensionClass);
        assertEquals("edu.uic.bitslab.propcov.extensions.coverage.JaCoCo", v1.coverage.extensionClass);
        assertEquals("edu.uic.bitslab.propcov.extensions.analysisframework.Opal", v1.analysisFramework.extensionClass);
        assertEquals(3, v1.analysisFramework.properties.size());
        assertEquals("RTA", v1.analysisFramework.properties.get("AnalysisType"));
        assertEquals("EXCEPTION", v1.analysisFramework.properties.get("AnalysisFlags"));
        assertEquals("WARN", v1.analysisFramework.properties.get("OPALLoggerType"));
        assertThrows(IllegalArgumentException.class, () -> Converter.main(args));
    }

    @Test
    void v99999() throws IOException {
        String target = tempDir.resolve("v99999-target.yaml").toString();
        String[] args = {
                pathV99999,
                target
        };
        Converter.main(args);
        assertTrue(outStreamCaptor.toString().contains("Unable to convert from version 99999."));
    }
}