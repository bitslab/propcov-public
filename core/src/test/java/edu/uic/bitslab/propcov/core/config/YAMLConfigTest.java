package edu.uic.bitslab.propcov.core.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YAMLConfigTest {
    static final String pathV0;
    static final String pathV1;

    static {
        ClassLoader classLoader = ConverterTest.class.getClassLoader();
        try {
            pathV0 = Path.of(Objects.requireNonNull(classLoader.getResource("v0.yaml")).toURI()).toString();
            pathV1 = Path.of(Objects.requireNonNull(classLoader.getResource("v1.yaml")).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void loadFromFileV0() {
        Assertions.assertThrows(RuntimeException.class, () -> YAMLConfig.loadFromFile(pathV0));
    }

    @Test
    void loadFromFileV1() throws IOException {
        YAMLConfig source = YAMLConfig.loadFromFile(pathV1);
        assertEquals(1, source.version);
    }

    @Test
    void testOrderedRepresenter() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        TestYaml testYaml = new TestYaml();
        testYaml.foo = "foo";
        testYaml.bar = "bar";

        OrderedRepresenter representer = new OrderedRepresenter(options);
        representer.addTypeDescription(new TypeDescription(TestYaml.class, "!TestYaml"));

        Yaml yaml = new Yaml(representer, options);
        String out = yaml.dump(testYaml).replaceFirst("^!![^\\n]+\n", "");
        assertEquals("""
                !TestYaml
                bar: bar
                foo: foo
                """, out);
    }

    static class TestYaml {
        public String foo;

        @YamlOutput(order = 1)
        public String bar;
    }

    @Test
    void testLoadProjectYamlConfig() throws IOException {
        Path v1 = Files.createDirectories(Path.of("artifacts","configs", "v1"));
        Files.copy(Path.of(pathV1), v1.resolve("v1.yaml"), StandardCopyOption.REPLACE_EXISTING);
        YAMLConfig yamlConfig = YAMLConfig.loadProjectYamlConfig("v1");
        assertEquals(1, yamlConfig.version);

        Files.deleteIfExists(Path.of("artifacts", "configs", "v1", "v1.yaml"));
        Files.deleteIfExists(Path.of("artifacts", "configs", "v1"));
        Files.deleteIfExists(Path.of("artifacts", "configs"));
        Files.deleteIfExists(Path.of("artifacts"));
    }
}