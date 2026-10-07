package edu.uic.bitslab.propcov.core.analyze;

import edu.uic.bitslab.propcov.core.analyze.scoringprovider.AbstractScoringProvider;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeuristicTraversalListenerTest {

    static class TestScoringProvider extends AbstractScoringProvider {

        public TestScoringProvider(Config config, AbstractBaseGraph<ColorNode, DefaultEdge> graph) {
            super(config, graph);
        }

        @Override
        public double score(ColorNode vertex) {
            return Double.parseDouble(vertex.label);
        }
    }

    @Test
    void vertexTrip() {
        assertThrows(NullPointerException.class, () -> new HeuristicTraversalListener(null, null, null, null));

        AbstractBaseGraph<ColorNode, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        ColorNode a0 = new ColorNode.Builder("10").Build();
        ColorNode b0 = new ColorNode.Builder("20").Build();
        ColorNode b1 = new ColorNode.Builder("30").Build();
        ColorNode c0 = new ColorNode.Builder("40").Build();
        graph.addVertex(a0);
        graph.addVertex(b0);
        graph.addVertex(b1);
        graph.addVertex(c0);
        graph.addEdge(a0, b0);
        graph.addEdge(a0, b1);
        graph.addEdge(b0, c0);

        Map< ColorNode, HeuristicNodeData> nodeData = new HashMap<>();

        Map< HeuristicNodeData.ScoreTypes, AbstractScoringProvider > scoringProviders = Map.of(
                HeuristicNodeData.ScoreTypes.OVERALL, new TestScoringProvider(null, graph),
                HeuristicNodeData.ScoreTypes.LOC, new TestScoringProvider(null, graph)
        );

        Function<HeuristicNodeData, Double> overallScore = h -> 1.0;

        HeuristicTraversalListener listener = new HeuristicTraversalListener(graph, nodeData, scoringProviders, overallScore);

        listener.vertexTraversed(new VertexTraversalEvent<>(new Object(), a0));
        assertEquals(1, listener.depthCount);

        listener.vertexTraversed(new VertexTraversalEvent<>(new Object(), b0));
        assertEquals(2, listener.depthCount);

        listener.vertexTraversed(new VertexTraversalEvent<>(new Object(), c0));
        assertEquals(3, listener.depthCount);

        listener.vertexFinished(new VertexTraversalEvent<>(new Object(), c0));
        assertEquals(2, listener.depthCount);

        listener.vertexFinished(new VertexTraversalEvent<>(new Object(), b0));
        assertEquals(1, listener.depthCount);

        listener.vertexTraversed(new VertexTraversalEvent<>(new Object(), b1));
        assertEquals(2, listener.depthCount);

        listener.vertexFinished(new VertexTraversalEvent<>(new Object(), b1));
        assertEquals(1, listener.depthCount);

        listener.vertexFinished(new VertexTraversalEvent<>(new Object(), a0));
        assertEquals(0, listener.depthCount);


        // nodeData test
        assertEquals(11.0, nodeData.get(a0).getScore(HeuristicNodeData.ScoreTypes.OVERALL));
        assertEquals(10.0, nodeData.get(a0).getScore(HeuristicNodeData.ScoreTypes.LOC));
        assertEquals(21.0, nodeData.get(b0).getScore(HeuristicNodeData.ScoreTypes.OVERALL));
        assertEquals(20.0, nodeData.get(b0).getScore(HeuristicNodeData.ScoreTypes.LOC));
        assertEquals(31.0, nodeData.get(b1).getScore(HeuristicNodeData.ScoreTypes.OVERALL));
        assertEquals(30.0, nodeData.get(b1).getScore(HeuristicNodeData.ScoreTypes.LOC));
        assertEquals(41.0, nodeData.get(c0).getScore(HeuristicNodeData.ScoreTypes.OVERALL));
        assertEquals(40.0, nodeData.get(c0).getScore(HeuristicNodeData.ScoreTypes.LOC));
    }

    @Test
    void vertexFinished() {
    }
}