package edu.uic.bitslab.propcov.core.config;

import edu.uic.bitslab.propcov.core.config.YAMLExperiments.YAMLExperimentConfig;
import edu.uic.bitslab.propcov.core.config.YAMLExperiments_v0.YAMLExperimentConfig_v0;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The ConverterExperiment class is a utility for handling the conversion and persistence of YAML configurations.
 * It provides functionality to transform version 0 YAML configuration files into version 1 configurations
 * and save the converted configurations to disk in the YAML format.
 * <p>
 * This class includes the following main features:
 * 1. Conversion of YAML configuration version 0 to version 1 using the `fromV0` method.
 * 2. Writing the YAML configuration to a specified file path in a formatted and non-overwriting manner.
 * 3. A command-line interface for converting and saving a YAML configuration.
 * <p>
 * The expected YAMLConfig object consists of specific fields such as version, name, subProject, source,
 * build system, test framework, analysis framework, and other configuration details. These are internally
 * mapped from the data in a YAMLConfig_v0 object during the conversion process.
 * <p>
 * Important Notes:
 * - The `fromV0` method handles mapping fields from an older YAML structure (v0) to the newer structure (v1).
 * - The `writeYaml` method saves the YAML configuration to a specified file path and throws exceptions if
 *   the path is invalid or if it would overwrite an existing file.
 * - The `main` method serves as an entry point to convert and save configurations via command-line arguments.
 */
public class ConverterExperiment {
    private static Consumer<Integer> ExitHandler = System::exit;

    private ConverterExperiment() {
        super();
    }

    private static YAMLConfig overrideFromV0(YAMLConfig_v0 v0) {
        if (v0 == null) return null;

        YAMLConfig v1 = new YAMLConfig();
        v1.version = 1;

        if (v0.analysisType != null || v0.analysisFlags != null || v0.opalLoggerType != null) {
            Map<String, String> analysisFrameworkProperties = new HashMap<>();
            if (v0.analysisType != null) analysisFrameworkProperties.put("AnalysisType", v0.analysisType);
            if (v0.analysisFlags != null) analysisFrameworkProperties.put("AnalysisFlags", String.join(",", v0.analysisFlags));
            if (v0.opalLoggerType != null) analysisFrameworkProperties.put("OPALLoggerType", v0.opalLoggerType);
            v1.analysisFramework = new YAMLConfig.Extension(
                    "edu.uic.bitslab.propcov.extensions.analysisframework.Opal",
                    analysisFrameworkProperties,
                    Map.of()
            );
        }

        if (v0.timeouts != null) v1.timeouts = v0.timeouts;

        return v1;
    }

    private static YAMLExperiments fromV0(YAMLExperiments_v0 v0) {
        YAMLExperiments v1 = new YAMLExperiments();
        v1.version = 1;
        v1.preset = new YAMLExperimentConfig();
        v1.preset.version = 1;
        v1.preset.trials = v0.preset.trials;
        v1.preset.iterations = v0.preset.iterations;
        v1.preset.projects = v0.preset.projects;
        v1.preset.overrideConfig = overrideFromV0(v0.preset.overrideConfig);
        v1.experiments = v0.experiments.entrySet().stream()
            .map( e -> {
                YAMLExperimentConfig_v0 ecv0 = e.getValue();
                YAMLExperimentConfig ecv1 = new YAMLExperimentConfig();
                ecv1.version = 1;
                ecv1.trials = ecv0.trials;
                ecv1.iterations = ecv0.iterations;
                ecv1.projects = ecv0.projects;
                ecv1.overrideConfig = overrideFromV0(ecv0.overrideConfig);

                return Map.entry(e.getKey(), ecv1);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return v1;
    }

    private static void writeYaml(YAMLExperiments source, Path target, boolean overwrite) throws IOException {
        if (target.toFile().exists()) {
            if (overwrite) {
                target.toFile().delete();
            } else {
                throw new IllegalArgumentException("Cowardly refusing to overwrite existing target file.");
            }
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowReadOnlyProperties(false);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(new OrderedRepresenter(options), options);
        String out = yaml.dump(source).replaceFirst("^!![^\\n]+\n", "");

        Files.writeString(target, out, StandardOpenOption.CREATE_NEW);
    }

    static void setExitHandler(Consumer<Integer> exitHandler) {
        ExitHandler = exitHandler;
    }

    /**
     * The main entry point for the Converter application. This method is responsible for handling the
     * command-line arguments, determining the YAML version of the source configuration file, and performing
     * the conversion if applicable. It exits the application with an error message if improper arguments
     * are provided or if the conversion cannot be performed.
     *
     * @param args the command-line arguments. Expects exactly two arguments:
     *             1. The file path to the source YAML configuration to be converted.
     *             2. The file path to store the converted YAML configuration.
     * @throws IOException if there is an issue reading from or writing to the files specified in the arguments.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 3) {
            System.err.println("Usage: ConverterExperiment <source_path> <target_path>");
            ExitHandler.accept(1);
            return;
        }

        String source = args[0];
        String target = args[1];
        boolean overwrite = args.length == 3 && args[2].equals("--overwrite");

        System.out.println("Converting Experiment from " + source + " to " + target);

        int yamlVersion = YAMLExperiments.getYamlVersion(source);
        if (yamlVersion == 0) {
            YAMLExperiments v1 = fromV0(YAMLExperiments_v0.loadFromFile(source));
            writeYaml(v1, Path.of(target), overwrite);
            System.out.println("Wrote YAML to " + target);

        } else {
            System.out.println("Unable to convert from version " + yamlVersion + ".");
        }
    }
}
