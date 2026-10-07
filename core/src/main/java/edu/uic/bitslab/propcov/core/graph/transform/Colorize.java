package edu.uic.bitslab.propcov.core.graph.transform;

import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.Config.NodeType;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import edu.uic.bitslab.propcov.core.util.SUTClassLoader;
import org.jgrapht.Graph;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Executable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * The {@code Colorize} class is responsible for transforming a graph of Strings into a directed graph
 * of {@link ColorNode} objects while applying specific transformations based on coverage data, method-level
 * analysis, and class hierarchy. This class extends {@link AbstractTransform} to facilitate custom graph
 * transformations, leveraging a configuration and coverage details.
 * This transformation process includes:
 * - Setting up vertices using class and method information derived from each provided vertex label.
 * - Determining colors for nodes based on entry-point analysis, coverage ratios, and test or library-based
 *   method identification.
 * - Iteratively updating nodes to propagate coverage-related data from subtypes or overriding methods within
 *   the class structure.
 * - Replacing nodes within the graph to achieve an updated configuration of vertices and edges.
 */
public class Colorize extends AbstractTransform {
    private static final Logger LOGGER = Util.getLogger(Colorize.class);
    private final Config config;

    private AbstractBaseGraph<ColorNode, DefaultEdge> colorGraph;
    private Map<String, ColorNode> vertices;

    /**
     * Constructs a Colorize instance with a specified graph, coverage, and configuration.
     *
     * @param graph the graph to be processed, represented as a {@code Graph<String, DefaultEdge>} instance
     * @param coverage an instance of {@code AbstractCoverage} providing coverage-related functionalities
     * @param config the configuration data required for the processing, encapsulated in a {@code Config} object
     */
    public Colorize(Graph<String, DefaultEdge> graph, AbstractCoverage coverage, Config config) {
        super(graph, coverage);
        this.config = config;
    }

    @Override
    public AbstractBaseGraph<ColorNode, DefaultEdge> process() {
        LOGGER.info("Applying coverage!");

        // new graph
        colorGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        vertices = new HashMap<>();

        for (DefaultEdge e : graph.edgeSet()) {
            ColorNode v1 = setupVertex( graph.getEdgeSource(e) );
            ColorNode v2 = setupVertex( graph.getEdgeTarget(e) );

            colorGraph.addEdge(v1, v2);
        }

        // fix up interface method color by looking at subtypes of each interface method
        ReplaceNodePair[] removePairs = colorGraph.vertexSet().stream()
            .filter( colorNode -> colorNode.color.equals(config.nodeColor[NodeType.VIRTUAL.ordinal()]) )
            .map( colorNode -> {

                // find all items that are subtypes of this one
                long linesCovered = 0, linesMissed = 0, branchesCovered = 0, branchesMissed = 0;
                for (ColorNode otherNode : colorGraph.vertexSet()) {
                    if (otherNode.clazz == null) {
                        LOGGER.warn("otherNode.clazz is null for {}", otherNode.label);
                        continue;
                    }

                    Class<?> sup = otherNode.clazz.getSuperclass();
                    if (sup == null) continue;

                    if (interfaceFound(colorNode, sup) || overrideFound(colorNode, otherNode)) {
                        linesCovered += otherNode.linesCovered;
                        linesMissed += otherNode.linesMissed;
                        branchesCovered += otherNode.branchesCovered;
                        branchesMissed += otherNode.branchesMissed;
                    }
                }

                long linesTotal = linesMissed + linesCovered;

                if (linesTotal > 0) {
                    float lineRatio = (float) linesCovered / linesTotal;

                    return new ReplaceNodePair(
                        colorNode,
                        (new ColorNode.Builder(colorNode))
                            .color(chooseColor(colorNode.label, colorNode.clazz, colorNode.method, lineRatio))
                            .isEntryPoint(isEntryPoint(colorNode.label))
                            .linesCovered(linesCovered)
                            .linesMissed(linesMissed)
                            .branchesCovered(branchesCovered)
                            .branchesMissed(branchesMissed)
                            .Build()
                    );

                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toArray(ReplaceNodePair[]::new);

        // process nodes
        for (ReplaceNodePair removePair : removePairs) {
            replaceNode(removePair.existing, removePair.target);
        }

        return colorGraph;
    }

    private boolean interfaceFound(ColorNode colorNode, Class<?> sup) {
        // if not interface... return false
        if (!colorNode.clazz.isInterface()) return false;

        // if colorNode.clazz doesn't exist in interfaces... return false
        if (Arrays.stream(sup.getInterfaces()).noneMatch( c -> c.equals(colorNode.clazz) )) return false;

        // is method a part of any interface?
        return Arrays
            .stream(sup.getInterfaces())
            .anyMatch( c -> {
                if (colorNode.method == null)
                    return false;
                try {
                    c.getMethod(colorNode.method.getName(), colorNode.method.getParameterTypes());
                    return true;
                } catch (NoSuchMethodException e) {
                    return false;
                }
            });
    }

    private boolean overrideFound(ColorNode colorNode, ColorNode otherNode) {
        return (otherNode.clazz.getSuperclass().equals(colorNode.clazz));
    }


    /**
     * Replace a node by adding, copying edges, and removing
     *
     */
    private void replaceNode(ColorNode currentNode, ColorNode newNode) {
        // get a list of incoming edges for the current node
        Set<ColorNode> inV = colorGraph.incomingEdgesOf(currentNode).stream().map(colorGraph::getEdgeSource).collect(Collectors.toSet());
        Set<ColorNode> outV = colorGraph.outgoingEdgesOf(currentNode).stream().map(colorGraph::getEdgeSource).collect(Collectors.toSet());

        // remove current node
        colorGraph.removeVertex(currentNode);
        vertices.remove(currentNode.label);

        // add new node
        colorGraph.addVertex(newNode);
        vertices.put(newNode.label, newNode);

        // add incoming edges from whatever to the new node
        for (ColorNode incomingNode : inV) {
            colorGraph.addEdge(incomingNode, newNode);
        }

        // add outgoing edges from new to whatever they connected to before
        for (ColorNode outgoingNode : outV) {
            try {
                colorGraph.addEdge(newNode, outgoingNode);
            } catch (IllegalArgumentException | NullPointerException e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }

    private ColorNode setupVertex(String vertexLabel) {
        {
            ColorNode v = vertices.get(vertexLabel);
            if (v != null) return v;
        }

        Class<?> clazz;
        Executable method;

        Matcher matcher =  Util.labelToParts.matcher(vertexLabel);
        if (matcher.matches()) {
            Class<?> givenClazz = getClassForName(Util.fullClass(matcher));
            method = Util.methodFrom(givenClazz, matcher.group("method"), matcher.group("descriptor"));
            clazz = (method == null) ? givenClazz : method.getDeclaringClass();
        } else {
            LOGGER.warn("Label does not match expected format, so unable to break it apart. Label: {}", vertexLabel);
            clazz = null;
            method = null;
        }

        ColorNode v = node(vertexLabel, clazz, method);
        colorGraph.addVertex(v);
        vertices.put(vertexLabel, v);
        return v;
    }

    private Class<?> getClassForName(String className) {
       try {
            return Class.forName(className, false, SUTClassLoader.get());
        } catch (ClassNotFoundException | NoClassDefFoundError exception) {
            LOGGER.warn("Class not found for {} - Exception: {}", className, exception.getMessage());
            return null;
        }
    }

    private ColorNode node(String label, Class<?> clazz, Executable method) {
        CoverageDetail other = coverage.getOtherStatistics(label);
        CoverageDetail propCov = coverage.getPropCovStatistics(label);

        float lineRatio = propCov == null ? 0 : (float) propCov.linesCovered / (float) propCov.linesTotal;
        String color = chooseColor(label, clazz, method, lineRatio);

        return new ColorNode
                .Builder(label, clazz, method, other, propCov, isEntryPoint(label), isTestOrLibraryMethod(config, clazz))
                .color(color)
                .type(colorToType(color))
                .Build();
    }

    private boolean isTestOrLibraryMethod(Config config, Class<?> clazz) {
        try {
            return coverage.IsTestOrLibraryMethod(config, clazz);
        } catch (IOException e) {
            return false;
        }
    }

    private String colorToType(String color) {
        String[] nodeColor = config.nodeColor;

        for (int i = 0; i < nodeColor.length; i++) {
            if (nodeColor[i].equals(color)) {
                return Config.NodeType.values()[i].name();
            }
        }

        return "";
    }

    private boolean isEntryPoint(String label) {
        // assuming inDegreeOf 0 must be entryPoint
        return graph.inDegreeOf(label) == 0 && graph.outDegreeOf(label) > 0;
    }

    private String chooseColor(String label, Class<?> clazz, Executable method, float lineRatio) {
        if (isEntryPoint(label)) {
            return config.nodeColor[NodeType.ENTRYPOINT.ordinal()];
        }

        // is this a test method?
        try {
            if (coverage.IsTestOrLibraryMethod(config, clazz)) {
                return config.nodeColor[NodeType.TEST.ordinal()];
            }
        } catch (IOException e){
            return config.nodeColor[NodeType.ERROR.ordinal()];
        }


        // is this implied?
        if (coverage.IsImpliedMethod(clazz, method)) {
            return config.nodeColor[NodeType.IMPLIED.ordinal()];
        }

        // is it the default no-args constructor?
        if (method != null && method.getParameterCount() == 0 && clazz != null && clazz.getDeclaredConstructors().length == 1 && Float.isNaN(lineRatio) && method.getName().equals(clazz.getName()))
            return config.nodeColor[NodeType.IMPLIED.ordinal()];

        if (clazz != null && clazz.isInterface() && Float.isNaN(lineRatio)) {
            return config.nodeColor[NodeType.VIRTUAL.ordinal()];
        }

        // method is covered, mark based on how much it is covered
        if (lineRatio > 0.75) return config.nodeColor[NodeType.COVERAGE4.ordinal()];
        if (lineRatio > 0.50) return config.nodeColor[NodeType.COVERAGE3.ordinal()];
        if (lineRatio > 0.25) return config.nodeColor[NodeType.COVERAGE2.ordinal()];
        if (lineRatio > 0.02) return config.nodeColor[NodeType.COVERAGE1.ordinal()];
        return config.nodeColor[NodeType.COVERAGE0.ordinal()];
    }

    private static class ReplaceNodePair {
        final ColorNode existing;
        final ColorNode target;

        ReplaceNodePair(ColorNode existing, ColorNode target) {
            this.existing = existing;
            this.target = target;
        }
    }
}
