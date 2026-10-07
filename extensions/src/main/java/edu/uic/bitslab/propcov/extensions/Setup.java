package edu.uic.bitslab.propcov.extensions;

import edu.uic.bitslab.propcov.core.AbstractSetup;
import edu.uic.bitslab.propcov.core.analysisframework.AbstractAnalysisFramework;
import edu.uic.bitslab.propcov.core.buildsystem.AbstractBuildSystem;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.source.AbstractSource;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import edu.uic.bitslab.propcov.extensions.analysisframework.Opal;
import edu.uic.bitslab.propcov.extensions.buildsystem.Maven;
import edu.uic.bitslab.propcov.extensions.buildsystem.Gradle;
import edu.uic.bitslab.propcov.extensions.coverage.JaCoCo;
import edu.uic.bitslab.propcov.extensions.source.Git;
import edu.uic.bitslab.propcov.extensions.source.Local;
import edu.uic.bitslab.propcov.extensions.testframework.JQwik;
import edu.uic.bitslab.propcov.extensions.testframework.JunitQuickCheck;

import java.util.Map;

import static edu.uic.bitslab.propcov.core.Props.readProperties;

/**
 * Setup class for provided PropCov extensions.
 */
public class Setup extends AbstractSetup {
    private static boolean isInitialized = false;

    public void init() {
        if (isInitialized) return;
        readProperties("extensions-defined-properties.yaml");
        AbstractSetup.bridges.add(this);
        isInitialized = true;
    }

    @Override
    public AbstractSource getSource(YAMLConfig.Extension extension, String projectName) throws SourceException {
        return switch (extension.extensionClass) {
            case "Git", "edu.uic.bitslab.propcov.extensions.source.Git" -> new Git(extension, projectName);
            case "Local", "edu.uic.bitslab.propcov.extensions.source.Local" -> new Local(extension, projectName);
            default -> null;
        };
    }

    @Override
    public AbstractTestFramework getTestFramework(YAMLConfig.Extension extension) {
        return switch (extension.extensionClass) {
            case "JunitQuickCheck", "edu.uic.bitslab.propcov.extensions.testframework.JunitQuickCheck" -> new JunitQuickCheck(extension);
            case "JQwik", "edu.uic.bitslab.propcov.extensions.testframework.JQwik" -> new JQwik(extension);
            default -> null;
        };
    }

    @Override
    public AbstractCoverage getCoverage(YAMLConfig.Extension extension) {
        return switch (extension.extensionClass) {
            case "JaCoCo", "edu.uic.bitslab.propcov.extensions.coverage.JaCoCo" -> new JaCoCo(extension);
            default -> null;
        };
    }

    @Override
    public AbstractAnalysisFramework getAnalysisFramework(YAMLConfig.Extension extension) {
        return switch (extension.extensionClass) {
            case "Opal", "edu.uic.bitslab.propcov.extensions.analysisframework.Opal" -> new Opal(extension);
            default -> null;
        };
    }

    @Override
    public AbstractBuildSystem getBuildSystem(YAMLConfig.Extension extension, Map<Config.TimeoutType, Long> timeouts, String projectName, String subProjectName) {
        return switch (extension.extensionClass) {
            case "Maven", "edu.uic.bitslab.propcov.extensions.buildsystem.Maven" -> new Maven(extension, timeouts, projectName, subProjectName);
            case "Gradle", "edu.uic.bitslab.propcov.extensions.buildsystem.Gradle" -> new Gradle(extension, timeouts, projectName, subProjectName);
            default -> null;
        };
    }
}
