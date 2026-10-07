package edu.uic.bitslab.propcov.core.graph;

import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.coverage.Writeable;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * The GraphCoverageStatistics class computes and displays coverage statistics for a graph,
 * including nodes, edges, lines, and branches. It also provides functionality for exporting
 * these statistics to a CSV file.
 */
public class GraphCoverageStatistics {

    private static final Logger LOGGER = Util.getLogger(GraphCoverageStatistics.class);
    private static final String SEPARATOR = "#############################";

    @Writeable
    private long edgeCount = 0;
    @Writeable
    private long nodesCovered = 0;
    @Writeable
    private long nodeCount = 0;
    @Writeable
    private long linesCovered = 0;
    @Writeable
    private long linesMissed = 0;
    @Writeable
    private long branchesCovered = 0;
    @Writeable
    private long branchesMissed = 0;

    /**
     * Quantifies the coverage quality of the graph
     *
     * @param graph the graph to quantify coverage quality for
     */
    private GraphCoverageStatistics(Graph<ColorNode, DefaultEdge> graph) {
        for (ColorNode node : graph.vertexSet()) {
            if (node.excluded) {
                continue;
            }

            this.linesCovered += node.linesCovered;
            this.linesMissed += node.linesMissed;
            this.branchesCovered += node.branchesCovered;
            this.branchesMissed += node.branchesMissed;
            this.edgeCount += graph.outDegreeOf(node);
            this.nodeCount++;

            if (node.covered) {
                this.nodesCovered += 1;
            }
        }
    }

    /**
     * Analyzes the coverage statistics of a graph and writes the calculated statistics to a CSV file.
     *
     * @param graph the graph to be analyzed for coverage statistics
     * @param outputName the name of the output file where the CSV data will be written
     */
    public static void analyze(Graph<ColorNode, DefaultEdge> graph, String outputName) {
        GraphCoverageStatistics statistics = new GraphCoverageStatistics(graph);
        statistics.announce();
        try {
            toCsv(statistics, outputName);
        } catch (Exception e) {
            LOGGER.error("Error writing {} to CSV!", outputName);
        }
    }

    private static void toCsv(GraphCoverageStatistics statistics, String fileName) throws Exception {
        if (statistics == null) return;
        FileWriter writer = new FileWriter(fileName);
        Arrays.stream(GraphCoverageStatistics.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Writeable.class))
                .forEach(
                        f -> {
                            try {
                                writer.write(f.getName() + "," + f.get(statistics) + "\n");
                            } catch (IllegalAccessException | IOException e) {
                                LOGGER.error("Unable to write statistics to {}", fileName, e);
                            }
                        });
        writer.close();
    }

    private void announce() {
        float nodeCoverage = ((float) this.nodesCovered) / this.nodeCount * 100;
        float lineCoverage = ((float) this.linesCovered) / (this.linesCovered + linesMissed) * 100;
        float branchCoverage =
                ((float) this.branchesCovered) / (this.branchesCovered + this.branchesMissed) * 100;

        nodeCoverage = (Float.isNaN(nodeCoverage)) ? 0 : nodeCoverage;
        lineCoverage = (Float.isNaN(lineCoverage)) ? 0 : lineCoverage;
        branchCoverage = (Float.isNaN(branchCoverage)) ? 0 : branchCoverage;

        LOGGER.info(SEPARATOR);
        LOGGER.info("Coverage Statistics:");
        LOGGER.info("Edge Count:            {}", this.edgeCount);
        LOGGER.info("Node Count:            {}", this.nodeCount);
        LOGGER.info("Nodes Covered:         {}", this.nodesCovered);
        LOGGER.info("Lines Covered:         {}", this.linesCovered);
        LOGGER.info("Lines Missed:          {}", this.linesMissed);
        LOGGER.info("Branches Covered:      {}", this.branchesCovered);
        LOGGER.info("Branches Missed:       {}", this.branchesMissed);
        LOGGER.info("Method Coverage:       {}%", String.format("%.2f", nodeCoverage));
        LOGGER.info("Line Coverage:         {}%", String.format("%.2f", lineCoverage));
        LOGGER.info("Branch Coverage:       {}%", String.format("%.2f", branchCoverage));
        LOGGER.info(SEPARATOR);
    }
}
