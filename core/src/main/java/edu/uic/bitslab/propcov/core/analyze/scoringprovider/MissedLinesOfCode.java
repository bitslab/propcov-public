package edu.uic.bitslab.propcov.core.analyze.scoringprovider;

import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashMap;
import java.util.Map;

/**
 * The MissedLinesOfCode class extends the AbstractScoringProvider to calculate
 * a score for nodes in the graph based on the number of missed lines of code.
 * The scoring mechanism considers both the missed lines of code in the node
 * itself and the scores of its child nodes in the directed graph.
 * <p>
 * This scoring implementation uses a weighted formula where the parent node's
 * contribution is emphasized more than that of its child nodes. Additionally,
 * the scores are cached in a map to optimize performance for subsequent calculations.
 */
public class MissedLinesOfCode extends AbstractScoringProvider {
    final Map<ColorNode, Double> nodeScore = new HashMap<>();

    /**
     * Constructs a new instance of the MissedLinesOfCode scoring provider.
     *
     * @param config the configuration providing additional settings or parameters
     *               required for the scoring operation.
     * @param graph the directed graph structure that represents the nodes (ColorNode)
     *              and edges (DefaultEdge) whose missed lines of code are to be scored.
     */
    public MissedLinesOfCode(Config config, AbstractBaseGraph<ColorNode, DefaultEdge> graph) {
        super(config, graph);
    }

    @Override
    public double score(ColorNode vertex) {
        final double weightParentScore = 1;
        final double weightChildrenScore = .5;

        // parent score (note: high score is better)
        double parentScore = vertex.linesMissed;

        double totalChildrenScore = graph.outgoingEdgesOf(vertex)
                .stream()
                .mapToDouble(e -> nodeScore.getOrDefault(graph.getEdgeTarget(e), 0.0d))
                .sum();

        double vertexScore = (weightParentScore * parentScore) +
                (weightChildrenScore * totalChildrenScore);

        double roundScore = Math.round(vertexScore * 100) / 100.0d;
        nodeScore.put(vertex, roundScore);

        return roundScore;
    }
}
