package edu.uic.bitslab.propcov.core;

import edu.uic.bitslab.propcov.core.analysisframework.AbstractAnalysisFramework;
import edu.uic.bitslab.propcov.core.buildsystem.AbstractBuildSystem;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.source.AbstractSource;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;

import java.util.Map;

import static edu.uic.bitslab.propcov.core.Props.readProperties;


/**
 * The Setup class extends AbstractSetup and provides implementation for initializing
 * and managing core-defined properties.
 */
public class Setup extends AbstractSetup {
    private static boolean isInitialized = false;

    public void init() {
        if (isInitialized) return;
        SharedProps.init();
        readProperties("core-defined-properties.yaml");
        AbstractSetup.bridges.add(this);
        isInitialized = true;
    }

    @Override
    public AbstractSource getSource(YAMLConfig.Extension extension, String projectName) {
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
        return null;
    }

    @Override
    public AbstractBuildSystem getBuildSystem(YAMLConfig.Extension extension, Map<Config.TimeoutType, Long> timeouts, String projectName, String subProjectName) {
        return null;
    }
}
