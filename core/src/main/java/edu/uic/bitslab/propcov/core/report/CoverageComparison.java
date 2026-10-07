package edu.uic.bitslab.propcov.core.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.analyze.CoverageData;
import edu.uic.bitslab.propcov.core.analyze.Heuristic;
import edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import edu.uic.bitslab.propcov.core.util.SUTClassLoader;
import edu.uic.bitslab.propcov.core.util.Serialize;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.numbers.NumberColumnFormatter;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.io.csv.CsvWriter;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;

/**
 * The CoverageComparison class analyzes and compares code coverage metrics
 * based on provided test results, generating detailed reports. The class computes
 * coverage data, distinct coverage reports, and formatted summary statistics for reporting purposes.
 * <p>
 * The class interacts with coverage data serialized in specific file formats and
 * integrates data from multiple sources to provide insights into coverage, missed lines of code,
 * and method-level details. Additionally, it generates summary and distinct reports
 * for coverage improvement analysis.
 * <p>
 * Features of the CoverageComparison class include:
 * - Parsing and processing serialized coverage data from source files.
 * - Generating detailed coverage reports with metrics such as lines of code (LOC) coverage
 *   and method coverage.
 * - Identifying unknown coverage methods and filtering out test/library methods from reports.
 * - Generating aggregated statistical summaries like mean and standard deviation for coverage results.
 * - Creating serialized output for coverage distinctions (covered/missed lines) in JSON format.
 * <p>
 * Usage:
 * - Instantiate the class with a map of source LOC details.
 * - Use the report method to generate a CSV report containing metrics for various property-specific
 *   and overall coverage statistics.
 * - Use the distinctReport method to generate a JSON-formatted report that highlights unique
 *   coverage characteristics, including unknown methods and their associated LOC.
 * <p>
 * Dependencies:
 * - Logger: Provides logging capabilities.
 * - Table and CsvWriter: Used for creating and exporting data into CSV files.
 * - LOCTracker: Resolves the lines of code information from source LOC details.
 * - CoverageData: Handles coverage data parsing and processing.
 * - HeuristicNodeData: Incorporates heuristic scoring for property-specific coverage.
 * - Jackson ObjectMapper: Serializes distinct coverage data into JSON format.
 * <p>
 * Exceptions:
 * - IOException: Thrown when file I/O operations fail.
 * - ClassNotFoundException: Thrown when attempting to load classes that do not exist.
 * - IllegalStateException: Thrown when color node configurations or data parsing fails.
 */
public class CoverageComparison {
    final Table report = Table.create("Report");
    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageComparison.class);
    private Set<String> isPercentage;
    private Set<String> coverageUnknownMethods;
    private Set<Long> coveredLines;
    private Set<Long> missedLines;
    private final Map<String, LOCTracker.LOCDetail> locsFromSource;

    /**
     * Constructs a new CoverageComparison instance using the provided source data for lines of code (LOC).
     *
     * @param locsFromSource a map where the keys are method signatures and the values are {@link LOCTracker.LOCDetail}
     *                       objects containing details about covered and missed lines of code.
     */
    public CoverageComparison(Map<String, LOCTracker.LOCDetail> locsFromSource) {
        this.locsFromSource = locsFromSource;
    }

    private String totalColumn(String mean, String stdDev, boolean isPercentage) {
        try {
            if (isPercentage) {
                return String.format("%.2f%% ± %.2f%%", Double.parseDouble(mean) * 100.0, Double.parseDouble(stdDev) * 100.0);
            } else {
                return String.format("%.2f ± %.2f", Double.parseDouble(mean), Double.parseDouble(stdDev));
            }
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    /**
     * Generates a coverage report based on the provided coverage data file, test directories, and configuration.
     * Writes the generated report to the specified writer and includes information on property coverage, line coverage,
     * and method coverage, with additional statistics and summaries.
     *
     * @param coverageFile the path to the coverage data file, which contains serialized coverage details
     * @param testPaths an array of paths representing directories for tests; each directory is expected to contain specific serialized files for coverage and heuristic data
     * @param writer the writer to which the generated CSV report will be written
     * @param config the configuration object containing additional project-specific metadata
     * @throws IOException if an I/O error occurs while reading files or writing the report
     * @throws ClassNotFoundException if a required class is not found during deserialization
     */
    public void report(Path coverageFile, Path[] testPaths, Writer writer, Config config) throws IOException, ClassNotFoundException {
        report.clear();

        isPercentage = new HashSet<>();
        coverageUnknownMethods = new HashSet<>();
        coveredLines = new HashSet<>();
        missedLines = new HashSet<>();


        StringColumn cProperty = StringColumn.create("Property");
        LongColumn cPropCovMethodsCovered = LongColumn.create("PropCov.Methods.Covered");
        LongColumn cPropCovMethodsReachable = LongColumn.create("PropCov.Methods.Reachable");

        DoubleColumn cPropCovMethodsPercentage = DoubleColumn.create("PropCov.Methods.Percentage");
        cPropCovMethodsPercentage.setPrintFormatter(NumberColumnFormatter.percent(1));
        isPercentage.add(cPropCovMethodsPercentage.name());

        DoubleColumn cPropCovMethodsScore = DoubleColumn.create("PropCov.Methods.Score");
        cPropCovMethodsScore.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(1));

        LongColumn cPropCovLOCCovered = LongColumn.create("PropCov.LOC.Covered");
        LongColumn cPropCovLOCReachable = LongColumn.create("PropCov.LOC.Reachable");

        DoubleColumn cPropCovLOCPercentage = DoubleColumn.create("PropCov.LOC.Percentage");
        cPropCovLOCPercentage.setPrintFormatter(NumberColumnFormatter.percent(1));
        isPercentage.add(cPropCovLOCPercentage.name());

        DoubleColumn cPropCovLOCScore = DoubleColumn.create("PropCov.LOC.Score");
        cPropCovLOCScore.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(1));

        LongColumn cCoverageMethodsCovered = LongColumn.create("Coverage.Methods.Covered");
        LongColumn cCoverageMethodsReachable = LongColumn.create("Coverage.Methods.Reachable");

        DoubleColumn cCoverageMethodsPercentage = DoubleColumn.create("Coverage.Methods.Percentage");
        cCoverageMethodsPercentage.setPrintFormatter(NumberColumnFormatter.percent(1));
        isPercentage.add(cCoverageMethodsPercentage.name());

        LongColumn cCoverageLOCCovered = LongColumn.create("Coverage.LOC.Covered");
        LongColumn cCoverageLOCReachable = LongColumn.create("Coverage.LOC.Reachable");

        DoubleColumn cCoverageLOCPercentage = DoubleColumn.create("Coverage.LOC.Percentage");
        cCoverageLOCPercentage.setPrintFormatter(NumberColumnFormatter.percent(1));
        isPercentage.add(cCoverageLOCPercentage.name());

        report.addColumns(
                cProperty,
                cPropCovMethodsCovered,
                cPropCovMethodsReachable,
                cPropCovMethodsPercentage,
                cPropCovMethodsScore,
                cPropCovLOCCovered,
                cPropCovLOCReachable,
                cPropCovLOCPercentage,
                cPropCovLOCScore,
                cCoverageMethodsCovered,
                cCoverageMethodsReachable,
                cCoverageMethodsPercentage,
                cCoverageLOCCovered,
                cCoverageLOCReachable,
                cCoverageLOCPercentage
        );

        // coverage info
        Map<String, Map<CoverageData.CoverageDataType, CoverageData>> coverage = Serialize.Read(Files.readAllBytes(coverageFile));

        for (Path directory : testPaths) {
            String testName = directory.toFile().getName();

            try {
                AbstractBaseGraph<ColorNode, DefaultEdge> g = Serialize.Read(Files.readAllBytes(directory.resolve("coverage.ser")));
                Map<ColorNode, HeuristicNodeData> heuristicNodeData = Heuristic.deserializeNodeData(g, Files.readAllBytes(directory.resolve("heuristicNodeData.ser")));

                // Coverage for this test
                CoverageDetail.Builder coverageTotalBuild = new CoverageDetail.Builder();
                coverage.get(testName).get(CoverageData.CoverageDataType.Coverage).details.values().forEach(coverageTotalBuild::addTo);
                CoverageDetail CoverageTotal = coverageTotalBuild.build();

                // propCov for this test
                CoverageDetail.Builder propCovTotalBuild = new CoverageDetail.Builder();
                coverage.get(testName).get(CoverageData.CoverageDataType.PropCov).details.values().forEach(propCovTotalBuild::addTo);
                CoverageDetail propCovTotal = propCovTotalBuild.build();

                ColorNode[] colorNodes = g.vertexSet().stream()
                    .filter( s -> g.inDegreeOf(s) == 0 && g.outDegreeOf(s) > 0 )
                    .toArray(ColorNode[]::new);
                if (colorNodes.length != 1) {
                    throw new IllegalStateException("colorNodes should contain exactly one node.");
                }

                // get heuristic info (or default to a new object if no heuristic data available)
                HeuristicNodeData heuristicNodeDatum = heuristicNodeData.getOrDefault(colorNodes[0], new HeuristicNodeData());

                cProperty.append(testName);
                cPropCovMethodsCovered.append(propCovTotal.methodsCovered);
                cPropCovMethodsReachable.append(propCovTotal.methodsTotal);
                cPropCovMethodsPercentage.append(propCovTotal.methodsCovered / (double) propCovTotal.methodsTotal);
                cPropCovMethodsScore.append(heuristicNodeDatum.getScore(HeuristicNodeData.ScoreTypes.METHOD));
                cPropCovLOCCovered.append(propCovTotal.linesCovered);
                cPropCovLOCReachable.append(propCovTotal.linesTotal);
                cPropCovLOCPercentage.append(propCovTotal.linesCovered / (double) propCovTotal.linesTotal);
                cPropCovLOCScore.append(heuristicNodeDatum.getScore(HeuristicNodeData.ScoreTypes.LOC));
                cCoverageMethodsCovered.append(CoverageTotal.methodsCovered);
                cCoverageMethodsReachable.append(CoverageTotal.methodsTotal);
                cCoverageMethodsPercentage.append(CoverageTotal.methodsCovered / (double) CoverageTotal.methodsTotal);
                cCoverageLOCCovered.append(CoverageTotal.linesCovered);
                cCoverageLOCReachable.append(CoverageTotal.linesTotal);
                cCoverageLOCPercentage.append(CoverageTotal.linesCovered / (double) CoverageTotal.linesTotal);

            } catch (NoSuchFileException exception){
                LOGGER.error("For {} unable to find object: {}", testName, exception.getMessage());
            }
        }

        CsvWriter csv = new CsvWriter();
        csv.write(report, CsvWriteOptions.builder(writer).usePrintFormatters(true).build());

        if (report.rowCount() > 0) {
            Table summary = report.summary();
            int rowMean = summary.column(0).indexOf("Mean");
            int rowStdDev = summary.column(0).indexOf("Std. Dev");

            Table total = Table.create(
                    report.columnNames().stream()
                            .map(n -> StringColumn.create(
                                    n,
                                    List.of(
                                            totalColumn(
                                                    summary.column(n).asStringColumn().get(rowMean),
                                                    summary.column(n).asStringColumn().get(rowStdDev),
                                                    isPercentage.contains(n)
                                            )
                                    )
                            ))
                            .toArray(StringColumn[]::new)
            );
            ((StringColumn) total.column(0)).set(0, config.project);

            csv.write(total, CsvWriteOptions.builder(writer).header(false).build());
        }
    }

    /**
     * Generates a distinct coverage report by filtering unknown methods, excluding test and library methods,
     * and computing their respective covered and missed lines of code. The report is serialized as a structured
     * JSON string.
     *
     * @param config the configuration object containing project-specific metadata and coverage-related settings
     * @return a JSON string representation of the distinct coverage report
     * @throws IOException if an error occurs during the serialization of the report
     */
    public String distinctReport(Config config) throws IOException {
        // resolve LOCs for unknown methods
        final Map<String, Long> unknownMethodsToLOC = new HashMap<>();

        LOCTracker locTracker = new LOCTracker(config.coverage);
        locTracker.getLOCs(coverageUnknownMethods, locsFromSource, unknownMethodsToLOC);

        // filter out test methods
        for (String s : unknownMethodsToLOC.keySet()) {
            try {
                Matcher m = Util.labelToParts.matcher(s);
                if (!m.matches()) throw new IllegalStateException("Invalid label: " + s);
                String clazz = Util.fullClass(m);

                if (config.coverage.IsTestOrLibraryMethod(config, Class.forName(clazz, false, SUTClassLoader.get()))) {
                    unknownMethodsToLOC.remove(s);
                }
            } catch (IllegalStateException | ClassNotFoundException e) {
                LOGGER.warn("Error processing unknown method {} with error {}.", s, e.getMessage());
            }
        }

        // add total unknown methods LoC to missedLines
        missedLines.addAll(unknownMethodsToLOC.values());
        DistinctReport distinctReport = new DistinctReport(coveredLines, missedLines, unknownMethodsToLOC);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(distinctReport);
    }
}