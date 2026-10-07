package edu.uic.bitslab.propcov.core.config;

import edu.uic.bitslab.propcov.core.AbstractSetup;
import edu.uic.bitslab.propcov.core.analysisframework.AbstractAnalysisFramework;
import edu.uic.bitslab.propcov.core.buildsystem.AbstractBuildSystem;
import edu.uic.bitslab.propcov.core.buildsystem.BuildSystemException;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcess;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcessException;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.report.LOCTracker;
import edu.uic.bitslab.propcov.core.source.AbstractSource;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.testframework.AbstractTestFramework;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.opalj.br.Method;
import scala.Tuple2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Place to keep useful mocks for Propcov objects
 */
public class Mocks {
    /**
     * Setup function for Mock
     */
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public static class Setup extends AbstractSetup {
        private static boolean isInitialized = false;

        public void init() {
            if (isInitialized) return;
            AbstractSetup.bridges.add(this);
            isInitialized = true;
        }

        @Override
        public AbstractSource getSource(YAMLConfig.Extension extension, String projectName) {
            return switch (extension.extensionClass) {
                case "SourceTest" -> new SourceTest(extension, projectName, "A");
                default -> null;
            };
        }

        @Override
        public AbstractTestFramework getTestFramework(YAMLConfig.Extension extension) {
            return switch (extension.extensionClass) {
                case "TestFrameworkTest" -> new TestFrameworkTest(extension, "PropCov.TestFrameworkTest.");
                default -> null;
            };
        }

        @Override
        public AbstractCoverage getCoverage(YAMLConfig.Extension extension) {
            return switch (extension.extensionClass) {
                case "CoverageTest" -> new CoverageTest(extension, "PropCov.CoverageTest.");
                default -> null;
            };
        }

        @Override
        public AbstractAnalysisFramework getAnalysisFramework(YAMLConfig.Extension extension) {
            return switch (extension.extensionClass) {
                case "AnalysisFrameworkTest" -> new AnalysisFrameworkTest(extension);
                default -> null;
            };
        }

        @Override
        public AbstractBuildSystem getBuildSystem(YAMLConfig.Extension extension, Map<Config.TimeoutType, Long> timeouts, String projectName, String subProjectName) {
            return switch (extension.extensionClass) {
                case "BuildSystemTest" ->
                        new BuildSystemTest(extension, timeouts, projectName, subProjectName, "PropCov.BuildSystem.");
                default -> null;
            };
        }
    }

    /**
     * Support Classes for Testing
     */
    static class SourceTest extends AbstractSource {
        protected SourceTest(YAMLConfig.Extension extension, String projectName, String propertyPrefix) {
            super(extension, projectName, propertyPrefix);
        }

        @Override
        public void get() {
        }
    }

    static class BuildSystemTest extends AbstractBuildSystem {
        protected BuildSystemTest(YAMLConfig.Extension extension, Map<Config.TimeoutType, Long> timeouts, String projectName, String subProjectName, String propertyPrefix) {
            super(extension, timeouts, projectName, subProjectName, propertyPrefix);
        }

        @Override
        public void clean() {

        }

        @Override
        public void build() throws BuildSystemException, TimerException, IOException, ExternalProcessException, InterruptedException {

        }

        @Override
        public ExternalProcess.Result testProperty(Config config, PropertyTest propertyTest) {
            return null;
        }
    }

    static class AnalysisFrameworkTest extends AbstractAnalysisFramework {
        protected AnalysisFrameworkTest(YAMLConfig.Extension extension) {
            super(extension, "PropCov.AnalysisFramework.");
        }

        @Override
        public Graph<String, DefaultEdge> BuildCallGraph(Config config, PropertyTest propertyTest) {
            return null;
        }

        @Override
        public List<Tuple2<String, String>> GetProperties(InputStream inputStream, AbstractTestFramework testFramework) {
            return List.of();
        }
    }

    static class TestFrameworkTest extends AbstractTestFramework {

        public TestFrameworkTest(YAMLConfig.Extension extension, String propertyPrefix) {
            super(extension, propertyPrefix);
        }

        @Override
        public Boolean isTestProperty(Method method) {
            return null;
        }
    }

    static class CoverageTest extends AbstractCoverage {

        protected CoverageTest(YAMLConfig.Extension extension, String propertyPrefix) {
            super(extension, propertyPrefix);
        }

        @Override
        public Map<String, CoverageDetail> getOtherStatistics() {
            return Map.of();
        }

        @Override
        public Map<String, CoverageDetail> getPropCovStatistics() {
            return Map.of();
        }

        @Override
        public Set<String> getMissingNodesPropCov() {
            return Set.of();
        }

        @Override
        public CoverageDetail getOtherStatistics(String name) {
            return null;
        }

        @Override
        public CoverageDetail getPropCovStatistics(String name) {
            return null;
        }

        @Override
        public void apply(Graph<String, DefaultEdge> g, Config config, PropertyTest property) {

        }

        @Override
        public void reset(Path target) {

        }

        @Override
        public Path[] getResultPaths(Path start) {
            return new Path[0];
        }

        @Override
        public void buildLOC(byte[] clazzBytes, Map<String, LOCTracker.LOCDetail> locs, Map<String, String> inherit) {

        }
    }

    /**
     * Config Builder that is primed with useful Mock objects.
     *
     * @return Config.Builder to allow additional config settings by the caller.
     * @throws ConfigException When a configuration is not found.
     * @throws SourceException When a source configuration fails to configure.
     */
    public static Config.Builder getMockConfigBuilder() throws ConfigException, SourceException {
        return new Config
                .Builder("A")
                .source(new YAMLConfig.Extension("SourceTest", Map.of(), Map.of()))
                .buildSystem(new YAMLConfig.Extension("BuildSystemTest", Map.of(), Map.of()), Map.of())
                .analysisFramework(new YAMLConfig.Extension("AnalysisFrameworkTest", Map.of("AnalysisType", "RTA"), Map.of()))
                .addTestJars(List.of("test-jar.jar"))
                .addProperties(List.of(new PropertyTest("a", "b")));
    }
}
