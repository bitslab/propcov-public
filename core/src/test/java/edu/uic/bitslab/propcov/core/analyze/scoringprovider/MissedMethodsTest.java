package edu.uic.bitslab.propcov.core.analyze.scoringprovider;

import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.ConfigException;
import edu.uic.bitslab.propcov.core.config.Mocks;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import edu.uic.bitslab.propcov.core.source.SourceException;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static edu.uic.bitslab.propcov.core.config.Config.NodeType.COVERAGE0;
import static edu.uic.bitslab.propcov.core.config.Config.NodeType.COVERAGE4;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MissedMethodsTest {

    @BeforeEach
    void setUp() {
        new edu.uic.bitslab.propcov.core.Setup().init();
        new Mocks.Setup().init();
    }

    @Test
    void score() throws ConfigException, SourceException {
        Config.Builder builder = Mocks.getMockConfigBuilder();
        Config config = builder.build(null);
        DefaultDirectedGraph<ColorNode, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        MissedMethods missedMethods = new MissedMethods(config, graph);

        ColorNode colorNode0 = new ColorNode.Builder("Label0").color(config.nodeColor[COVERAGE0.ordinal()]).Build();
        graph.addVertex(colorNode0);
        assertEquals(1.0, missedMethods.score(colorNode0));

        ColorNode colorNode1 = new ColorNode.Builder("Label1").color(config.nodeColor[COVERAGE0.ordinal()]).Build();
        ColorNode colorNode2 = new ColorNode.Builder("Label2").color(config.nodeColor[COVERAGE4.ordinal()]).Build();
        ColorNode colorNode3 = new ColorNode.Builder("Label3").color(config.nodeColor[COVERAGE0.ordinal()]).Build();
        graph.addVertex(colorNode1);
        graph.addVertex(colorNode2);
        graph.addVertex(colorNode3);
        graph.addEdge(colorNode0, colorNode1);
        graph.addEdge(colorNode0, colorNode2);
        graph.addEdge(colorNode0, colorNode3);
        missedMethods.score(colorNode1);
        missedMethods.score(colorNode2);
        missedMethods.score(colorNode3);
        assertEquals(2.0, missedMethods.score(colorNode0));
    }
}