package edu.uic.bitslab.propcov;


import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.analysisframework.AnalysisFrameworkException;
import edu.uic.bitslab.propcov.core.analyze.Artifact;
import edu.uic.bitslab.propcov.core.analyze.CoverageData;
import edu.uic.bitslab.propcov.core.analyze.Heuristic;
import edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData;
import edu.uic.bitslab.propcov.core.buildsystem.BuildSystemException;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcess;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcess.Result;
import edu.uic.bitslab.propcov.core.buildsystem.ExternalProcessException;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import edu.uic.bitslab.propcov.core.graph.Export;
import edu.uic.bitslab.propcov.core.graph.Serialize;
import edu.uic.bitslab.propcov.core.graph.transform.Colorize;
import edu.uic.bitslab.propcov.core.report.DeveloperReport;
import edu.uic.bitslab.propcov.core.source.SourceException;
import edu.uic.bitslab.propcov.core.util.NoGraphException;
import edu.uic.bitslab.propcov.core.util.timer.RunTimer;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import org.jgrapht.Graph;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static edu.uic.bitslab.propcov.core.Props.resolveBoolean;
import static edu.uic.bitslab.propcov.core.analyze.CoverageData.CoverageDataType.Coverage;
import static edu.uic.bitslab.propcov.core.analyze.CoverageData.CoverageDataType.PropCov;
import static edu.uic.bitslab.propcov.core.config.Config.TimeoutType.dotGeneration;
import static edu.uic.bitslab.propcov.core.config.Config.TimeoutType.patchSUT;
import static edu.uic.bitslab.propcov.core.config.Config.WorkflowItem.*;

/**
 * Represents the main entry point for managing the software analysis and coverage processes.
 * This class encapsulates operations such as building call graphs, applying coverage, running analysis,
 * patching, downloading, and removing system-under-test (SUT), running property-based coverage tests,
 * and generating reports.
 */
public class Run {
    private final Config config;

    private Graph<String, DefaultEdge> callgraph;
    private AbstractBaseGraph<ColorNode, DefaultEdge> colorGraph;
    private final Artifact artifact;
    private static final Logger LOGGER = Util.getLogger(Run.class);

    private Run(Config config) throws TimerException, IOException {
        this.config = config;
        artifact = Artifact.CreateArtifact(config);

        // set artifact for all of these
        config.analysisFramework.setArtifact(artifact);
        config.source.setArtifact(artifact);
        config.buildSystem.setArtifact(artifact);
        config.coverage.setArtifact(artifact);
        config.testFramework.setArtifact(artifact);
    }

    @SuppressWarnings("RedundantThrows")
    private void BuildCallGraph(PropertyTest propertyTest) throws IOException, TimerException, ExternalProcessException, InterruptedException, NoGraphException, ExecutionException, TimeoutException, AnalysisFrameworkException {
        RunTimer timer = new RunTimer("BuildCallGraph", LOGGER);

        // Build Callgraph(s) using OPAL for each endpoint
        LOGGER.info("Building callgraph");

        callgraph = config.analysisFramework.BuildCallGraph(config, propertyTest);

        Path destination = artifact.addTest(Export.StringGraphToByte(callgraph), "callgraph.dot");

        if (resolveBoolean("PropCov.BuildSVG")) {
            artifact.addTest(
                    Util.dotTo(Util.DotConvertType.SVG, destination.toFile(), config.timeouts.get(dotGeneration)),
                    "callgraph.svg"
            );
        }

        timer.stop();
        artifact.addPropertyElapsedTime(propertyTest.name, timer);
    }

    private void RemoveSUT() throws TimerException, IOException {
        RunTimer timer = new RunTimer("RemoveSUT", LOGGER);
        config.source.localRemove();
        timer.stop();
        artifact.addElapsedTime(timer);
    }

    private void DownloadSUT() throws TimerException, SourceException {
        RunTimer timer = new RunTimer("DownloadSUT", LOGGER);
        config.source.get();
        timer.stop();
        artifact.addElapsedTime(timer);
    }

    private void PatchSUT() throws TimerException, IOException, ExternalProcessException, InterruptedException {
        RunTimer timer = new RunTimer("PatchSUT", LOGGER);

        String patchCommand = String.format("patch --ignore-whitespace -p1 -d %s < %s" , config.source.localDirectory, config.patchFile);
        Result result = (new ExternalProcess(config.timeouts.get(patchSUT)))
                .command(patchCommand)
                .run();

        if (result.exitCode > 0) {
            throw new ExternalProcessException("Patch Command Failed.  Exit Code " + result.exitCode);
        }

        timer.stop();
        artifact.addElapsedTime(timer);
    }

    private void RunBuild() throws TimerException, IOException, BuildSystemException, ExternalProcessException, InterruptedException {
        RunTimer timer = new RunTimer("RunBuild", LOGGER);

        config.buildSystem.clean();

        try {
            config.buildSystem.build();
        } catch (BuildSystemException buildSystemException) {
            artifact.addError(buildSystemException);
            artifact.complete();
            throw buildSystemException;
        }

        artifact.addRuntimes(config.mainJars);
        artifact.addRuntimes(config.mainJarsWithDependencies);
        artifact.addRuntimes(config.testJars);

        timer.stop();
        artifact.addElapsedTime(timer);
    }

    private void RunPropertyCoverage(PropertyTest propertyTest) throws TimerException, ExternalProcessException, InterruptedException, BuildSystemException {
        try {
            Result result = config.buildSystem.testProperty(config, propertyTest);

            // add files to artifact results with information about each run
            artifact.addTests(config.coverage.getResultPaths(config.buildSystem.getFullTargetPath(propertyTest)));
            Path propcovrun = config.buildSystem.getFullTargetPath(new PropertyTest(propertyTest.name, propertyTest.entryPoint, "propcovrun"));
            if (Files.exists(propcovrun)) {
                artifact.addTests(new Path[]{propcovrun.resolve("site")});
            }

            // log time in results metadata
            artifact.addPropertyElapsedTime(propertyTest.name, "RunPropertyCoverage", result.elapsedTimeInSeconds);

            artifact.addPropertyTrials(
                propertyTest.name,
                Long.parseLong(result.jsonData.getOrDefault("overrideTrials", "-1")),
                Long.parseLong(result.jsonData.getOrDefault("trialsExpected", "-1")),
                Long.parseLong(result.jsonData.getOrDefault("trialsRan", "-1"))
            );

            if (!result.errors.isEmpty()) {
                artifact.addPropertyError(propertyTest.name, new BuildSystemException(result.errors));
            }
        } catch (IOException ioException) {
            throw new BuildSystemException("Error while running property coverage", ioException);
        }
    }

    private void ApplyCoverage(PropertyTest property) throws TimerException {
        RunTimer timer = new RunTimer("ApplyCoverage", LOGGER);

        Path destination;

        try {
            // apply
            config.coverage.apply(callgraph, config, property);

            // set coverage data for each coverage type
            artifact.setCoverageData(property, Coverage, new CoverageData(config.coverage.getOtherStatistics()));
            artifact.setCoverageData(property, PropCov, new CoverageData(config.coverage.getPropCovStatistics()));

            // write csv text as well for missed
            artifact.addTest((String.join("\n", config.coverage.getMissingNodesPropCov()).getBytes(StandardCharsets.UTF_8)), "missingNodesPropCov.csv");

            Colorize colorize = new Colorize(callgraph, config.coverage, config);
            colorGraph = colorize.process();

            artifact.addTest(Serialize.Write(colorGraph), "coverage.ser");
            destination = artifact.addTest(Export.GraphToByte(colorGraph), "coverage.dot");
        } catch (IOException e) {
            LOGGER.error("Could not write coverage.dot graph.", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (resolveBoolean("PropCov.BuildSVG")) {
            try {
                artifact.addTest(
                        Util.dotTo(Util.DotConvertType.SVG, destination.toFile(), config.timeouts.get(dotGeneration)),
                        "coverage.svg"
                );
            } catch (IOException e) {
                LOGGER.error("Could not convert dot to svg using filename coverage.svg.", e);
            } catch (InterruptedException e) {
                LOGGER.error("DOT conversion was interrupted.", e);
            } catch (Exception e) {
                LOGGER.error("DOT conversion ended with exception.", e);
            }
        }

        timer.stop();
        artifact.addPropertyElapsedTime(property.name, timer);
    }

    private void Analysis(PropertyTest property) throws TimerException, IOException, ExternalProcessException, InterruptedException {
        RunTimer timer = new RunTimer("Analysis", LOGGER);

        // heuristic node data
        Map<ColorNode, HeuristicNodeData> nodeData = new HashMap<>();

        // run heuristic
        Heuristic heuristic = new Heuristic(colorGraph, config);
        heuristic.run(nodeData);

        // output serialized heuristic node data
        artifact.addTest(heuristic.serializedObject(nodeData), "heuristicNodeData.ser");

        // output graph dot
        Path destination = artifact.addTest(heuristic.generateGraphDOT(nodeData), "heuristic.dot");

        if (resolveBoolean("PropCov.BuildSVG")) {
            // output graph svg
            artifact.addTest(
                    Util.dotTo(Util.DotConvertType.SVG, destination.toFile(), config.timeouts.get(dotGeneration)),
                    "heuristic.svg"
            );
        }

        timer.stop();
        artifact.addPropertyElapsedTime(property.name, timer);
    }

    protected void Report(String artifactSubDir) throws TimerException, IOException, ClassNotFoundException {
        if (artifactSubDir == null) throw new NullPointerException("artifactSubDir must not be null");

        RunTimer timer = new RunTimer("Report", LOGGER);

        DeveloperReport developerReport = new DeveloperReport(config, artifactSubDir);
        developerReport.process();

        timer.stop();
        artifact.addElapsedTime("Report", timer.elapsedInSeconds());
    }

    /**
     * Processes the given configuration and performs associated operations.
     *
     * @param config the configuration object used to define the behavior of the process
     * @throws TimerException if a timing-related error occurs during the process
     * @throws IOException if an I/O error occurs during the process
     * @throws ExternalProcessException if an error occurs while invoking an external process
     * @throws InterruptedException if the process is interrupted
     * @throws SourceException if an error related to the source code occurs
     * @throws BuildSystemException if an error occurs in the build system
     * @throws ClassNotFoundException if a required class cannot be found during the process
     */
    public static void process(Config config) throws TimerException, IOException, ExternalProcessException, InterruptedException, SourceException, BuildSystemException, ClassNotFoundException {
        process(config, null);
    }

    /**
     * Processes the given configuration and executes associated operations based on the provided workflow.
     * Handles tasks including artifact preparation, applying patches, building the system, running property tests,
     * generating reports, and cleaning up. Catches and logs errors during the processing of individual properties
     * to ensure the workflow continues.
     *
     * @param config the configuration object that defines the workflow steps and properties to be processed
     * @param label an optional label to assign to the artifact being processed
     * @return the full path to the artifact generated during the process
     * @throws TimerException if a timing-related issue occurs during execution
     * @throws IOException if I/O operations fail during the process
     * @throws ExternalProcessException if an issue occurs while interacting with external processes
     * @throws InterruptedException if the operation is interrupted
     * @throws SourceException if an error related to the source code occurs
     * @throws BuildSystemException if the build system encounters an issue
     * @throws ClassNotFoundException if a required class cannot be found during execution
     */
    public static Path process(Config config, String label) throws TimerException, IOException, ExternalProcessException, InterruptedException, SourceException, BuildSystemException, ClassNotFoundException {
        Run run = new Run(config);
        if (label != null) run.artifact.setLabel(label);

        // clean up
        if (config.workflow.contains(Remove)) run.RemoveSUT();
        if (config.workflow.contains(Download)) run.DownloadSUT();

        // patch
        if (config.workflow.contains(Patch)) run.PatchSUT();

        // build
        if (config.workflow.contains(RunBuild)) run.RunBuild();

        for (PropertyTest property : config.properties) {
            try {
                run.artifact.setSubFolder(property.entryPointAsPath());
                run.artifact.initMetaData(property.name);

                if (config.workflow.contains(BuildCallGraph)) run.BuildCallGraph(property);
                if (config.workflow.contains(RunPropertyCoverage)) run.RunPropertyCoverage(property);
                if (config.workflow.contains(ApplyCoverage)) run.ApplyCoverage(property);
                if (config.workflow.contains(Analysis)) run.Analysis(property);

                run.artifact.propertyDone(property.name);
            } catch (Throwable e) {
                // Nothing should stop property processing.
                // One property throwing an exception should not stop other properties from being processed.
                run.artifact.addPropertyError(property.name, e);
                LOGGER.error("Error while running property coverage", e);
            }
        }
        run.artifact.unSetSubFolder();

        // run reports
        try {
            run.artifact.beforeReports();
            run.Report(run.artifact.artifactSubDir);
        } catch (NoSuchFileException e) {
            LOGGER.error("Error with coverage report when generating developer reports.", e);
            run.artifact.addError(e);
        }

        LOGGER.info("Process completed");
        run.artifact.complete();

        return run.artifact.fullArtifactPath;
    }
}