package edu.uic.bitslab.propcov.runtime.cli;

import edu.uic.bitslab.propcov.core.Props;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.analysisframework.AbstractAnalysisFramework;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.ConfigException;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import edu.uic.bitslab.propcov.extensions.Setup;
import edu.uic.bitslab.propcov.extensions.buildsystem.Maven;
import edu.uic.bitslab.propcov.extensions.coverage.JaCoCo;
import edu.uic.bitslab.propcov.extensions.source.Local;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;


/**
 * The GenConfig class is responsible for generating YAML configuration files for a given SUT.
 * Example:
 * {code}
 * java -jar runtime/target/propcov-runtime-0.1-SNAPSHOT-jar-with-dependencies.jar genconfig ../propcov-sut/mph-table/target/mph-table-1.0.6-SNAPSHOT-tests.jar
 * {/code}
 */
public class GenConfig {
    private final static int ARG_INDEX_JAR_FILE_NAME = 0;
    private final static int ARG_INDEX_OUTPUT_YAML_FILE_NAME = 1;

    private File jarFile;
    private Path outputYamlPath;

    private void validate(String[] args) throws IllegalArgumentException {
        switch (args.length) {
            case 2:
                outputYamlPath = args[ARG_INDEX_OUTPUT_YAML_FILE_NAME].isEmpty() ? null : Path.of(args[ARG_INDEX_OUTPUT_YAML_FILE_NAME]);

            case 1:
                if (args[ARG_INDEX_JAR_FILE_NAME].isEmpty()) {
                    throw new IllegalArgumentException("Argument jarFileName is missing or invalid.");
                }

                jarFile = new File(args[ARG_INDEX_JAR_FILE_NAME]);
                break;

            case 0:
                throw new IllegalArgumentException("No arguments provided.");

            default:
                throw new IllegalArgumentException("Max of 3 arguments allowed.");
        }

        if (!jarFile.exists()) {
            throw new IllegalArgumentException("Test jar filename (" + jarFile + ") does not exist.");
        }
    }

    /**
     * Processes the command-line arguments to generate a YAML configuration file.
     * This method first validates the input arguments and then generates the YAML
     * configuration file using the provided parameters in the input arguments.
     *
     * @param args An array of strings containing the following arguments:
     *             args[0] - The file path to the JAR file to analyze.
     *             args[1] - The file path where the generated YAML configuration should be stored (optional).
     * @throws Exception If validation fails or there are errors during YAML generation.
     */
    public void process(String[] args) throws Exception {
        validate(args);

        // optional properties
        String projectName = Props.resolveString("PropCov.ProjectName", "manual", true);
        String analysisFrameworkClass = Props.resolveString("ProvCov.AnalysisFramework.ExtensionClass", "edu.uic.bitslab.propcov.extensions.analysisframework.Opal", true);
        String testFrameworkClass = Props.resolveString("ProvCov.TestFramework.ExtensionClass", "edu.uic.bitslab.propcov.extensions.testframework.JunitQuickCheck", true);

        Setup.load();

        buildYaml(jarFile, outputYamlPath, projectName, analysisFrameworkClass, testFrameworkClass);
    }

    /**
     * Generates a YAML configuration file based on the provided input parameters and writes it to the specified output file.
     * If the output file is null, the generated YAML will be printed to the standard output instead.
     *
     * @param jarFileObj The JAR file to analyze.
     * @param outputFile The path to the file where the generated YAML configuration should be written
     *                   (if null, the YAML content will be printed to the console).
     * @param projectName Name of the project to use in generating the config.
     * @param analysisFrameworkClass Name of the analysis class.
     * @param testFrameworkClass Name of the test framework class.
     * @throws IOException If an I/O error occurs while writing the YAML configuration to the output file.
     * @throws ConfigException If config is not built using defaults, then this exception is thrown
     */
    private void buildYaml(File jarFileObj, Path outputFile, String projectName, String analysisFrameworkClass, String testFrameworkClass) throws IOException, ConfigException, SourceException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Config.Builder configBuilder = new Config.Builder(projectName);

        Path tempDir = Files.createTempDirectory("propcov");

        // Source (locked to Local)
        configBuilder.source(new YAMLConfig.Extension(
            Local.class.getName(),
            Map.of(
                "Directory", tempDir.toString(),
                "localDirectory", tempDir.toString()
            ),
            Map.of()
        ));

        // Build System (locked to Maven)
        configBuilder.buildSystem(new YAMLConfig.Extension(
            Maven.class.getName(), Map.of(), Map.of()
        ), Map.of());

        // Coverage System (locked to JaCoCo)
        configBuilder.coverage(new YAMLConfig.Extension(
            JaCoCo.class.getName(), Map.of(), Map.of()
        ));

        // Analysis Framework
        YAMLConfig.Extension analysisFrameworkExtension = new YAMLConfig.Extension(
            analysisFrameworkClass, Map.of(), Map.of()
        );
        configBuilder.analysisFramework(analysisFrameworkExtension);
        AbstractAnalysisFramework analysisFramework = (AbstractAnalysisFramework) Class.forName(analysisFrameworkClass).getDeclaredConstructor(YAMLConfig.Extension.class).newInstance(analysisFrameworkExtension);

        // Test Framework
        YAMLConfig.Extension testFrameworkExtension = new YAMLConfig.Extension(
            testFrameworkClass, Map.of(), Map.of()
        );
        configBuilder.testFramework(testFrameworkExtension);
        AbstractTestFramework testFramework = (AbstractTestFramework) Class.forName(testFrameworkClass).getDeclaredConstructor(YAMLConfig.Extension.class).newInstance(testFrameworkExtension);

        // Properties
        configBuilder.addProperties(Util.getEntryPoints(jarFileObj, testFramework, analysisFramework));
        Config config = configBuilder.build(null);

        String out = config.toYaml();

        if (outputFile == null) {
            System.out.printf("\n\n *** YAML PROPERTIES BELOW *** \n\n%s\n\n ******************** \n\n", out);
            return;
        }

        try {
            Files.writeString(outputFile, out, StandardOpenOption.CREATE_NEW);
            System.out.printf("\nWrote YAML to %s\n", outputFile);
        } catch (IOException ioException) {
            System.out.printf("\n\n *** IO Error writing file (contents printed below) *** \n\n%s\n\n ******************** \n\n", out);
            throw ioException;
        }
    }

}