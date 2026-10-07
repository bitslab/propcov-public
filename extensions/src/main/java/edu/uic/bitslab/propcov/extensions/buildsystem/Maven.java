package edu.uic.bitslab.propcov.extensions.buildsystem;

import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.buildsystem.AbstractBuildSystem;
import edu.uic.bitslab.propcov.core.buildsystem.BuildSystemException;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcess;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcess.Result;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcessException;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import edu.uic.bitslab.propcov.extensions.util.LoadXML;
import jakarta.xml.bind.JAXBException;
import jaxb.generated.surefire.Testsuite;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static edu.uic.bitslab.propcov.core.config.Config.TimeoutType.*;

/**
 * Represents the implementation of the Maven build system used for project building,
 * cleaning, and property-based testing. This class extends the functionality of
 * the abstract build system by providing Maven-specific implementation details
 * for these operations.
 */
public class Maven extends AbstractBuildSystem {
    private static final Logger LOGGER = Util.getLogger(Maven.class);

    /**
     * Represents Maven-specific command-line options for customizing
     * the behavior of Maven operations. This variable typically holds
     * additional arguments or flags to be passed to Maven during
     * build system executions, such as custom goals, profiles, or
     * other configuration settings.
     */
    public final String mvnOptions;

    /**
     * Constructs an instance of the Maven build system configuration.
     *
     * @param extension the extension configuration containing build-specific properties and environment variables
     * @param timeouts a map defining timeout values associated with different timeout types
     * @param projectName the name of the main project
     * @param subProjectName the name of the subproject within the main project
     */
    public Maven(YAMLConfig.Extension extension, Map<Config.TimeoutType, Long> timeouts, String projectName, String subProjectName) {
        super(extension, timeouts, projectName, subProjectName, "PropCov.BuildSystem.Maven.");
        String foundMvnOptions = extension.properties.getOrDefault("Options", "");
        mvnOptions = foundMvnOptions == null ? "" : foundMvnOptions;
    }

    @Override
    public void clean() throws TimerException, IOException, ExternalProcessException, InterruptedException {
        LOGGER.info("-------Cleaning target---------");
        (new ExternalProcess(timeouts.getOrDefault(cleanSUT, Long.MAX_VALUE / 1000000000)))
                .command("mvn clean")
                .workingDirectory(localDirectory)
                .environment(env)
                .run();
    }

    @Override
    public void build() throws TimerException, IOException, ExternalProcessException, InterruptedException, BuildSystemException {
        LOGGER.info("-------building target---------");

        Result result = (new ExternalProcess(timeouts.getOrDefault(buildSUT, Long.MAX_VALUE / 1000000000)))
                .command("mvn install -DskipTests")
                .workingDirectory(localDirectory)
                .environment(env)
                .run();

        if (result.exitCode != 0) {
            LOGGER.error("BUILD FAILED");
            throw new BuildSystemException("Build Failed");
        }
    }

    @Override
    public Result testProperty(Config config, PropertyTest propertyTest) throws IOException, TimerException, ExternalProcessException, InterruptedException {
        LOGGER.info("----------PROPERTY------------");
        String entryPoint = propertyTest.entryPoint;
        String propertyName = entryPoint.substring(0, entryPoint.lastIndexOf(".")) + "#" + entryPoint.substring(entryPoint.lastIndexOf(".") + 1, entryPoint.indexOf("("));
        LOGGER.info(propertyName);

        // reset coverage
        config.coverage.reset(config.buildSystem.getFullTargetPath(propertyTest));

        Result result = (new ExternalProcess(timeouts.getOrDefault(testPropertySUT, Long.MAX_VALUE / 1000000000)))
                .command("mvn test " + mvnOptions + " -Dtest=\"" + propertyName + "\"" + (propertyTest.subProject == null ? "" : " -pl=\"" + propertyTest.subProject + "\"") )
                .workingDirectory(localDirectory)
                .environment(env)
                .run();


        // gather errors
        try {
            Path sfPath = propertyTest.subProject == null
                ? localDirectory.resolve("target").resolve("surefire-reports")
                : localDirectory.resolve(propertyTest.subProject).resolve("target").resolve("surefire-reports");

            Path sfXML = sfPath.resolve("TEST-" + propertyName.substring(0, propertyName.indexOf("#")) + ".xml");

            LoadXML<Testsuite> loadTestsuite = new LoadXML<>(Testsuite.class);
            Testsuite testsuiteResults = loadTestsuite.load(sfXML);
            //int tNumTests = Integer.parseInt(testsuiteResults.getTests());
            int tNumFailures = Integer.parseInt(testsuiteResults.getFailures());
            int tNumSkipped = Integer.parseInt(testsuiteResults.getSkipped());
            int tNumErrors = Integer.parseInt(testsuiteResults.getErrors());
            //float tTime = testsuiteResults.getTime(); //This is now a string!

            // something found that is wrong
            if (tNumFailures > 0 || tNumSkipped > 0 || tNumErrors > 0) {
                Path sfTxt = sfPath.resolve(propertyName.substring(0, propertyName.indexOf("#")) + ".txt");
                return new Result(result, String.join("\n", Files.readAllLines(sfTxt).toArray(String[]::new)));
            }
        } catch (JAXBException | ParserConfigurationException | SAXException e) {
            return new Result(result, e.getMessage());
        }

        if (propertyTest.subProject != null) {
            (new ExternalProcess(timeouts.getOrDefault(testPropertySUT, Long.MAX_VALUE / 1000000000)))
                .command("mvn jacoco:report-aggregate -pl propcovrun -am")
                .workingDirectory(localDirectory)
                .environment(env)
                .run();
        }

        // no error, so return result
        return result;
    }
}
