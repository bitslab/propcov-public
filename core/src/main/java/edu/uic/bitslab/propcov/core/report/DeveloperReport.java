package edu.uic.bitslab.propcov.core.report;

import de.siegmar.fastcsv.writer.CsvWriter;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.analyze.Artifact;
import edu.uic.bitslab.propcov.core.analyze.Heuristic;
import edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import edu.uic.bitslab.propcov.core.graph.Serialize;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

import static edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData.ScoreTypes.METHOD;

/**
 * The DeveloperReport class is responsible for generating and managing reports
 * based on code coverage and test data analysis. Reports include categorization
 * of test coverage, overall coverage summaries, and test-by-test coverage details.
 * The class interacts with various configurations and artifacts to fetch and analyze data,
 * ultimately providing meaningful reports for developers.
 * <p>
 * Responsibilities include:
 * - Loading artifact and test data from specified configurations or paths.
 * - Categorizing tests based on coverage results into structured hierarchy files.
 * - Generating detailed CSV reports for overall coverage and individual test cases.
 * - Logging insights and warnings during the report generation process.
 * <p>
 * The class relies on external utility classes for specific tasks such as file serialization,
 * graph traversal, and heuristic computations.
 */
public class DeveloperReport {
    private final Artifact artifact;
    private final Config config;
    private final Logger LOGGER = LoggerFactory.getLogger(DeveloperReport.class);

    /**
     * Creates a new instance of the DeveloperReport class and loads the latest artifact
     * based on the provided configuration.
     *
     * @param config the configuration object used to initialize the developer report and
     *               load the latest artifact
     * @throws IOException if an I/O error occurs during loading the artifact
     * @throws TimerException if a timing-related exception is encountered
     */
    public DeveloperReport(Config config) throws IOException, TimerException {
        this.config = config;
        artifact = Artifact.LoadLastestArtifact(config);
    }

    /**
     * Constructs a DeveloperReport object and initializes it with the provided configuration
     * and artifact subdirectory. Loads the artifact based on the provided configuration and
     * subdirectory.
     *
     * @param config           The configuration object containing settings and parameters.
     * @param artifactSubDir   The subdirectory in which the artifact is located.
     * @throws IOException     If an I/O error occurs while loading the artifact.
     * @throws TimerException  If an error related to timing operations occurs.
     */
    public DeveloperReport(Config config, String artifactSubDir) throws IOException, TimerException {
        this.config = config;
        artifact = Artifact.LoadArtifact(config, artifactSubDir);
    }

    /**
     * Constructs a DeveloperReport instance by loading an artifact from the given file path.
     *
     * @param config The configuration object used to load the artifact.
     * @param artifactFullPath The file path pointing to the artifact to be loaded.
     * @throws TimerException If there is an error related to timing during the artifact loading process.
     * @throws IOException If an I/O error occurs while accessing the artifact file.
     */
    public DeveloperReport(Config config, Path artifactFullPath) throws TimerException, IOException {
        this.config = config;
        artifact = Artifact.LoadArtifactFromPath(config, artifactFullPath);
    }

    /**
     * Executes the process of generating a developer report based on code coverage data.
     * <p>
     * This method serves as the main entry point for initiating the report generation
     * process. Internally, it delegates the task to the `coverageReport` method, which
     * processes code coverage data, generates detailed reports, and performs associated
     * operations like categorizing and comparing coverage metrics.
     *
     * @throws IOException if an I/O operation fails during report generation.
     * @throws ClassNotFoundException if a required class definition for processing is not found.
     */
    public void process() throws IOException, ClassNotFoundException {
        coverageReport();
    }

    /**
     * Generates various code coverage reports for tests contained in the artifact.
     * The method processes test paths from the artifact, calculates coverage results
     * for each test, and creates aggregated reports. The following sub-reports
     * are generated:
     * - Per-test coverage reports
     * - Per-test color categorization reports
     * - A cumulative total coverage report
     * - A coverage comparison report
     * <p>
     * During execution:
     * - For each test directory in the artifact, individual coverage scores are computed.
     * - A CSV report and color-coded tree visualization are generated.
     * - All test coverage results are aggregated into a "totals" report.
     * - A comparative analysis of coverage metrics is performed.
     *
     * @throws IOException if there are I/O issues while accessing test files or writing reports.
     * @throws ClassNotFoundException if deserialization of required objects fails.
     */
    public void coverageReport() throws IOException, ClassNotFoundException {
        List<TotalCoverageByTest> totals = new ArrayList<>();

        for (Path testPath : artifact.getTestPaths()) {
            try {
                String fileName = testPath.getFileName().toString();

                // build coverageByTest csv
                totals.add(coverageByTest(testPath, fileName));

            } catch (NoSuchFileException ignored) {
                LOGGER.warn("No such file: {}", testPath);
            }
        }

        // write totals
        coverageTotalReport(totals);

        // coverage comparison
        coverageComparisonReport();
    }

    private void coverageTotalReport(List<TotalCoverageByTest> totals) throws IOException {
        Path output = Files.createTempFile("t-", "-csv.tmp");
        try (CsvWriter csv = CsvWriter.builder().build(output)) {
            csv.writeRecord("Test", "Score", "TotalLinesCovered", "TotalLines", "CoveredPercentage");

            for (TotalCoverageByTest total : totals) {
                float coverage = (float) total.propCov.linesCovered / total.propCov.linesTotal;

                String[] record = {
                        total.testName,
                        String.format("%.2f", total.methodScore),
                        Long.toString(total.propCov.linesCovered),
                        Long.toString(total.propCov.linesTotal),
                        String.format("%.2f%%", (coverage * 100))
                };

                csv.writeRecord(record);
            }
        }

        // copy to Artifact
        String targetFilename = "summary-" + config.project + ".csv";
        artifact.addReport(Files.readAllBytes(output), targetFilename);

        LOGGER.info("Coverage Summary Report Written to {}", artifact.targetPathReport.resolve(targetFilename));
    }

    private double getEntryPointScore(AbstractBaseGraph<ColorNode, DefaultEdge> g, Map<ColorNode, HeuristicNodeData> nodeData) {
        try {
            // totals
            ColorNode entryPoint = Util.getEntryPoint(g);
            return nodeData.get(entryPoint).getScore(METHOD);
        } catch (NoSuchElementException noSuchElementException) {
            return 0.00;
        }
    }

    private TotalCoverageByTest coverageByTest(Path testPath, String testName) throws IOException, ClassNotFoundException {
        // load graph
        AbstractBaseGraph<ColorNode, DefaultEdge> g = Serialize.Read(Files.readAllBytes(testPath.resolve("coverage.ser")));
        Map<ColorNode, HeuristicNodeData> nodeData = Heuristic.deserializeNodeData(g, Files.readAllBytes(testPath.resolve("heuristicNodeData.ser")));
        Path output = Files.createTempFile("t-", "-csv.tmp");

        double entryPointScore = getEntryPointScore(g, nodeData);

        TotalCoverageByTest total = new TotalCoverageByTest(testName, entryPointScore, 0.00);
        CoverageDetail.Builder coverageStatistics = new CoverageDetail.Builder();

        try (CsvWriter csv = CsvWriter.builder().build(output)) {
            csv.writeRecord("Method", "Color", "Type", "Score", "LinesMissed", "LinesCovered", "TotalLines", "CoveredPercentage");

            for (ColorNode c : g.vertexSet()) {
                long linesTotal = c.linesCovered + c.linesMissed;
                float coverage = (float) c.linesCovered / linesTotal;

                String[] record = {
                        c.label,
                        c.color,
                        c.type,
                        String.format("%.2f", nodeData.getOrDefault(c, new HeuristicNodeData()).getScore(METHOD)),
                        Long.toString(c.linesMissed),
                        Long.toString(c.linesCovered),
                        Long.toString(linesTotal),
                        String.format("%.2f%%", (coverage * 100))
                };

                csv.writeRecord(record);

                if (c.type.startsWith("COVERAGE")) {
                    // agg record
                    coverageStatistics.addLinesMissed(c.linesMissed);
                    coverageStatistics.addLinesCovered(c.linesCovered);
                }
            }
        }

        total.propCov = coverageStatistics.build();

        // copy to Artifact
        String targetFilename = "detail-" + testName + ".csv";
        artifact.addReport(Files.readAllBytes(output), targetFilename);

        LOGGER.info("Coverage By Test Report Written to {}", artifact.targetPathReport.resolve(targetFilename));
        return total;
    }

    private void coverageComparisonReport() throws IOException, ClassNotFoundException {
        Map<String, LOCTracker.LOCDetail> locsFromSource = new HashMap<>();
        Map<String, String> inherit = new HashMap<>();
        LOCTracker locTracker = new LOCTracker(config.coverage);
        for (Path jar : config.mainJars) {
            locTracker.run(jar, locsFromSource, inherit);
        }

//  /* This isn't used as the coverage details are built from the json to csv for the paper and this is not
//   *  completely accurate. The coverage_details.json ensures we accurately count all lines/methods correctly.
//   */
//        CoverageComparison coverageComparison = new CoverageComparison(locsFromSource);
//
//        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
//        Writer writer = new OutputStreamWriter(byteOutStream);
//        coverageComparison.report(
//            artifact.targetPathReport.resolve("coverage_details.ser"),
//            artifact.getTestPaths(),
//            writer,
//            config
//        );
//        artifact.addReport(byteOutStream.toByteArray(), "CoverageComparison.csv");
//
//        artifact.addReport(
//            coverageComparison.distinctReport(config).getBytes(StandardCharsets.UTF_8),
//            "distinct-report.json"
//        );
    }
}