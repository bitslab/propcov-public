package edu.uic.bitslab.propcov.core.analyze.scoringprovider;

import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashMap;
import java.util.Map;

import static edu.uic.bitslab.propcov.core.config.Config.NodeType.COVERAGE0;

/**
 * The MissedMethods class extends the AbstractScoringProvider to implement
 * a specific scoring mechanism for nodes in a directed graph. Each node is
 * given a score based on its color and the scores of its child nodes.
 * The scoring mechanism assigns a weight to the node's own color (parent score)
 * and the cumulative scores of its child nodes (children score). These are combined
 * to produce the final score for a node, which is stored for potential reuse.
 */
public class MissedMethods extends AbstractScoringProvider {
    final Map<ColorNode, Double> nodeScore = new HashMap<>();

    /**
     * Creates an instance of the MissedMethods class, initializing it with the
     * provided configuration and graph structure. This constructor sets up the
     * base scoring mechanism for graph nodes, associating each node with a score
     * based on its color and its relationship with other nodes in the graph.
     *
     * @param config The configuration object that contains settings and parameters
     *               for the scoring mechanism and behavior.
     * @param graph  The directed graph structure containing nodes of type ColorNode
     *               and edges of type DefaultEdge, which represents the relationships
     *               between nodes.
     */
    public MissedMethods(Config config, AbstractBaseGraph<ColorNode, DefaultEdge> graph) {
        super(config, graph);
    }

    @Override
    public double score(ColorNode vertex) {
        final double weightParentScore = 1;
        final double weightChildrenScore = .5;

        // parent score (note: high score is better)
        double parentScore = vertexColorToInt(vertex.color);

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


    private double vertexColorToInt(String color) {
        if (color.equals(config.nodeColor[COVERAGE0.ordinal()])) {
            return 1.00;
        }

        return 0.00;
    }
}
