package edu.uic.bitslab.propcov.core.analyze;

import edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData.ScoreTypes;
import edu.uic.bitslab.propcov.core.analyze.scoringprovider.AbstractScoringProvider;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import org.jgrapht.Graph;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import java.util.Map;
import java.util.function.Function;

import static edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData.ScoreTypes.OVERALL;


class HeuristicTraversalListener extends TraversalListenerAdapter<ColorNode, DefaultEdge> {
    private final Graph<ColorNode, DefaultEdge> graph;
    private final Map<ColorNode, HeuristicNodeData> nodeData;
    private final Map<ScoreTypes, AbstractScoringProvider> scoringProviders;
    private final Function<HeuristicNodeData, Double> overallScore;

    long depthCount = 0L;

    HeuristicTraversalListener(Graph<ColorNode, DefaultEdge> graph, Map<ColorNode, HeuristicNodeData> nodeData, Map<ScoreTypes, AbstractScoringProvider> scoringProviders, Function<HeuristicNodeData, Double> overallScore) {
        super();

        if (graph == null) throw new NullPointerException("graph is null");

        this.graph = graph;
        this.nodeData = nodeData;
        this.scoringProviders = scoringProviders;
        this.overallScore = overallScore;
    }

    @Override
    public void vertexTraversed(VertexTraversalEvent<ColorNode> vertexTraversalEvent) {
        depthCount++;
    }

    @Override
    public void vertexFinished(VertexTraversalEvent<ColorNode> vertexTraversalEvent) {
        ColorNode parentVertex = vertexTraversalEvent.getVertex();
        HeuristicNodeData h = nodeData.getOrDefault(parentVertex, new HeuristicNodeData());

        // run provided scoring providers
        scoringProviders.forEach( (scoreType, scoringProvider) -> h.addScore(scoreType, scoringProvider.score(parentVertex)));

        // calculate overate score
        h.addScore(OVERALL, overallScore.apply(h));

        // missed loc
        h.addMissedLinesOfCode(ScoreMissed(parentVertex));

        // depth
        h.setDepth(depthCount);
        depthCount--;

        nodeData.put(parentVertex, h);
    }

    private long ScoreMissed(ColorNode vertex) {
        return
            // this vertex linesMissed
            vertex.linesMissed
                // children of vertex
                + graph.outgoingEdgesOf(vertex).stream()
                .mapToLong(e -> nodeData.getOrDefault(graph.getEdgeTarget(e), new HeuristicNodeData()).getMissedLinesOfCode())
                .sum();
    }
}
