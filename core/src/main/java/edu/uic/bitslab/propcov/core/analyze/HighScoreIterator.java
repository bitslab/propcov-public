package edu.uic.bitslab.propcov.core.analyze;

import edu.uic.bitslab.propcov.core.graph.ColorNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData.ScoreTypes.METHOD;

class HighScoreIterator<V extends ColorNode, E extends DefaultEdge> extends DepthFirstIterator<V, E> {
    private final Map<V, HeuristicNodeData> nodeData;

    public HighScoreIterator(Graph<V, E> g, V startVertex, Map<V, HeuristicNodeData> nodeData) {
        super(g, startVertex);
        this.nodeData = nodeData;
    }

    @Override
    protected Set<E> selectOutgoingEdges(V vertex) {
        if (nodeData.getOrDefault(vertex, new HeuristicNodeData()).getScore(METHOD) <= 0) return Set.of();

        return graph
                .outgoingEdgesOf(vertex)
                .stream()
                .filter(e -> nodeData.getOrDefault(graph.getEdgeTarget(e), new HeuristicNodeData()).getScore(METHOD) > 0)
                .sorted(
                        (a, b) -> {
                            double aScore =  nodeData.get(graph.getEdgeTarget(a)).getScore(METHOD);
                            double bScore = nodeData.get(graph.getEdgeTarget(b)).getScore(METHOD);
                            return Double.compare(aScore, bScore); // asc order, since dfs uses stack
                        }
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
