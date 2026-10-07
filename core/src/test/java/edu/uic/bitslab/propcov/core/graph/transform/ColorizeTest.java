package edu.uic.bitslab.propcov.core.graph.transform;

import edu.uic.bitslab.propcov.core.Setup;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.ConfigException;
import edu.uic.bitslab.propcov.core.config.Mocks;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import edu.uic.bitslab.propcov.core.source.SourceException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ColorizeTest {

    @Test
    void process() throws ConfigException, SourceException, IOException {
        new Setup().init();
        Mocks.Setup setup = new Mocks.Setup();
        setup.init();

        DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        AbstractCoverage c = setup.getCoverage(new YAMLConfig.Extension("CoverageTest", Map.of(), Map.of()));
        AbstractCoverage coverage = spy(c);
        Config config = Mocks.getMockConfigBuilder().build(null);

        Colorize colorize = new Colorize(graph, coverage, config);

        // Sample Classes
        // Float: Test
        // Integer: Implied
        // Map: Virtual
        // String: SUT
        Map<String,CoverageDetail> data = new HashMap<>();
        data.put("Label0", new CoverageDetail.Builder().build());
        data.put("Label1", new CoverageDetail.Builder().build());
        data.put("java.util.Map.size()I", new CoverageDetail.Builder().build());
        data.put("java.lang.Float.isFinite(F)Z", new CoverageDetail.Builder().build());
        data.put("java.lang.Float.isNaN(F)Z", new CoverageDetail.Builder().addLinesCovered(2).addLinesMissed(4).addMethodsCovered(2).addMethodsMissed(1).build());
        data.put("java.lang.Integer.bitCount(I)I", new CoverageDetail.Builder().build());
        data.put("java.lang.String.getBytes()[B", new CoverageDetail.Builder().addLinesCovered(2).addLinesMissed(4).addMethodsCovered(2).addMethodsMissed(1).build());
        data.put("java.lang.String.startsWith(Ljava.lang.String;)Z", new CoverageDetail.Builder().addLinesCovered(0).addLinesMissed(3).addMethodsCovered(0).addMethodsMissed(1).build());
        data.put("java.lang.String.endsWith(Ljava.lang.String;)Z", new CoverageDetail.Builder().addLinesCovered(6).addLinesMissed(0).addMethodsCovered(1).addMethodsMissed(0).build());
        data.put("java.lang.String.length()I", new CoverageDetail.Builder().addLinesCovered(3).addLinesMissed(2).addMethodsCovered(1).addMethodsMissed(0).build());
        data.put("java.lang.String.toLowerCase()Ljava.lang.String;", new CoverageDetail.Builder().addLinesCovered(5).addLinesMissed(95).addMethodsCovered(1).addMethodsMissed(0).build());


        // add vertex/edges
        data.keySet().forEach(graph::addVertex);
        data.keySet().stream()
            .filter(s -> !s.equals("Label0"))
            .forEach(s -> graph.addEdge("Label0", s));

        doReturn(true).when(coverage).IsTestOrLibraryMethod(any(), eq(Float.class));
        doReturn(false).when(coverage).IsTestOrLibraryMethod(any(), AdditionalMatchers.not(eq(Float.class)));
        doReturn(true).when(coverage).IsImpliedMethod(eq(Integer.class), any());
        doReturn(false).when(coverage).IsImpliedMethod(AdditionalMatchers.not(eq(Integer.class)), any());

        data.forEach((s, d) -> doReturn(d).when(coverage).getPropCovStatistics(s));
        data.forEach((s, d) -> doReturn(d).when(coverage).getOtherStatistics(s));

        Graph<ColorNode, DefaultEdge> graphInColor = colorize.process();
        assertNotNull(graphInColor);

        // check "vertex" set
        assertEquals(graph.vertexSet().size(), graphInColor.vertexSet().size());
        graph.vertexSet().forEach(expected -> {
            ColorNode colorNode = graphInColor.vertexSet().stream()
                .filter(s -> s.label.equals(expected) )
                .findFirst().orElse(null);

            assertNotNull(colorNode);
            CoverageDetail cd = data.get(expected);
            assertEquals(cd.linesCovered, colorNode.linesCovered);
            assertEquals(cd.linesMissed, colorNode.linesMissed);
            assertEquals(cd.methodsTotal > 0 && cd.methodsTotal == cd.methodsCovered, colorNode.covered);
        });

        // check "edge" set
        assertEquals(graph.edgeSet().size(), graphInColor.edgeSet().size());
        graph.edgeSet().forEach(expected ->
            assertTrue(graphInColor.edgeSet().stream().anyMatch(s ->
                graphInColor.getEdgeSource(s).label.equals(graph.getEdgeSource(expected))
                    && graphInColor.getEdgeTarget(s).label.equals(graph.getEdgeTarget(expected))
            ))
        );
    }
}