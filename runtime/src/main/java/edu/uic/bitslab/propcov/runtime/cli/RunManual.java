package edu.uic.bitslab.propcov.runtime.cli;

import edu.uic.bitslab.propcov.Run;
import edu.uic.bitslab.propcov.core.buildsystem.BuildSystemException;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcessException;
import edu.uic.bitslab.propcov.core.cli.AbstractCLI;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.ConfigException;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static edu.uic.bitslab.propcov.core.Props.resolveString;

/**
 * The {@code RunManual} class provides the implementation for a Command Line Interface (CLI) tool
 * that processes configurations and executes specific operations using the provided configurations.
 * It extends the {@code AbstractCLI} class and overrides its methods to perform validation
 * of command-line arguments and to execute the processing logic.
 * -
 * Example:
 * {code}
 * java -DPropCov.LocalDirectory=../propcov-sut/mph-table -DTargetPath=target -DmainJar=mph-table-1.0.6-SNAPSHOT.jar -DmainJarsWithDependencies=mph-table-1.0.6-SNAPSHOT-jar-with-dependencies.jar -DTestPropertyEndpoint="com.indeed.mph.serializers.TestSmartListSerializer.canRoundTripSerializableLists(Ljava/util/List;Ljava/util/List;Ljava/util/List;)V" -DtestJar=mph-table-1.0.6-SNAPSHOT-tests.jar -jar runtime/target/propcov-runtime-0.1-SNAPSHOT-jar-with-dependencies.jar runmanual
 * {/code}
 */
public class RunManual extends AbstractCLI {
    @Override
    protected void validate(String[] args) throws IllegalArgumentException {
        if (args.length == 1) return;
        throw new IllegalArgumentException("Should have no arguments.");
    }

    private String fuzzyProp(String givenProp, Set<String> props) {
        if (props.contains(givenProp)) return givenProp;

        for (String prop : props) {
            if (prop.equalsIgnoreCase(givenProp)) return prop;
            if (prop.equalsIgnoreCase("PropCov." + givenProp)) return prop;
        }

        return null;
    }

    @Override
    public void process(String[] args) throws ExternalProcessException, InterruptedException, ClassNotFoundException {
        validate(args);

        Set<String> requiredProperties = Set.of(
            "PropCov.LocalDirectory",
            "PropCov.TargetPath",
            "PropCov.MainJar",
            "PropCov.TestJar",
            "PropCov.MainJarsWithDependencies",
            "PropCov.TestPropertyEndpoint"
        );

        Map<String,String> foundProperties = System.getProperties().stringPropertyNames().stream()
            .map(p -> {
                String fp = fuzzyProp(p, requiredProperties);
                return fp == null ? null : Map.entry(fp, System.getProperty(p));
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        SortedSet<String> missingProperties = requiredProperties.stream()
                .filter(p -> !foundProperties.containsKey(p))
                .collect(Collectors.toCollection(TreeSet::new));

        if (!missingProperties.isEmpty()) {
            System.out.println("Missing required properties: \n " + String.join("\n ", missingProperties) + "\n");
            System.out.println("Please set them in the JVM arguments or in a properties file.");
            System.exit(1);
        }

        // set found properties to System
        foundProperties.forEach(System::setProperty);

        try {
            Config config = ConfigFromProperties();
            Run.process(config);
        } catch (ConfigException e) {
            Main.help(Main.ACTION.runmanual);
        } catch (SourceException e) {
            System.out.println("Source configuration is invalid: " + e.getMessage());
            System.exit(1);
        } catch (TimerException e) {
            System.out.println("Error while executing timer: " + e.getMessage());
            System.exit(1);
        } catch (BuildSystemException e) {
            System.out.println("Build system configuration is invalid: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("IO Exception occurred: " + e.getMessage());
            System.exit(1);
        }
    }

    static Config ConfigFromProperties() throws ConfigException, SourceException {
        HashMap<String, String> env = new HashMap<>(System.getenv());

        return new Config
                .Builder("manual")
                .subProject("")
                .source(
                        new YAMLConfig.Extension(
                                resolveString("PropCov.Source.ExtensionClass", "edu.uic.bitslab.propcov.extensions.source.Local"),
                                Map.of("Directory", resolveString("PropCov.Source.Local.Directory")),
                                env
                        )
                )
                .buildSystem(
                        new YAMLConfig.Extension(
                                resolveString("PropCov.BuildSystem.ExtensionClass"),
                                Map.of(),
                                Map.copyOf(env)
                        ),
                        Config.Builder.getDefaultTimeout()
                )
                .coverage(
                        new YAMLConfig.Extension(
                                resolveString("PropCov.Coverage.ExtensionClass"),
                                Map.of(),
                                Map.copyOf(env)
                        )
                )
                .addMainJars(List.of(resolveString("PropCov.MainJar")))
                .addMainJarsWithDependencies(List.of(resolveString("PropCov.MainJarWithDependencies")))
                .addTestJars(List.of(resolveString("PropCov.TestJar")))
                .patchFile(null)
                .testFramework(
                        new YAMLConfig.Extension(
                                resolveString("PropCov.TestFramework.ExtensionClass"),
                                Map.of(),
                                Map.copyOf(env)
                        )
                )
                .analysisFramework(
                        new YAMLConfig.Extension(
                                resolveString("PropCov.AnalysisFramework.ExtensionClass"),
                                Map.of(),
                                Map.copyOf(env)
                        )
                )
                .addPropertyWithCheck(new PropertyTest())
                .outputPath("output")
                .artifactDirectory("artifacts/manual/")
                .workflowRemove(Set.of(Config.WorkflowItem.Remove, Config.WorkflowItem.Download, Config.WorkflowItem.Patch))
                .build(null);
    }
}
