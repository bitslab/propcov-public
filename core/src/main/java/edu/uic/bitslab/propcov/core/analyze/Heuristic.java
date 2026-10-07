package edu.uic.bitslab.propcov.core.analyze;

import edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData.ScoreTypes;
import edu.uic.bitslab.propcov.core.analyze.scoringprovider.AbstractScoringProvider;
import edu.uic.bitslab.propcov.core.analyze.scoringprovider.MissedLinesOfCode;
import edu.uic.bitslab.propcov.core.analyze.scoringprovider.MissedMethods;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import edu.uic.bitslab.propcov.core.graph.Export;
import edu.uic.bitslab.propcov.core.util.SUTClassLoader;
import edu.uic.bitslab.propcov.core.util.stream.ObjectInputStreamCustomLoader;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.traverse.DepthFirstIterator;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData.ScoreTypes.*;
import static edu.uic.bitslab.propcov.core.graph.Export.dotFormat;


/**
 * The Heuristic class is responsible for evaluating and scoring paths in a given graph
 * structure based on specific scoring methods. It computes scores, identifies top paths,
 * and generates serialized representations or visual outputs (e.g., Graph DOT format)
 * based on the computed data.
 * <p>
 * The main functionalities include:
 * - Scoring nodes and paths within a graph using depth-first traversal.
 * - Identifying top scoring paths from the graph.
 * - Serializing and deserializing graph-related data structures.
 * - Generating graph representations in DOT format for visualization.
 */
public class Heuristic {
    private static final Logger LOGGER = Util.getLogger(Heuristic.class);
    private final AbstractBaseGraph<ColorNode, DefaultEdge> reachability;
    private final List<ColorNode[]> orderedPaths = new ArrayList<>();
    private final List<Map<ColorNode, HeuristicNodeData>> heuristicPathNodeData = new ArrayList<>();
    private static final int NUM_TOP_PATHS = 3;
    private final Config config;

    /**
     * Constructs a new instance of the Heuristic class.
     *
     * @param reachability the reachability graph, represented as an AbstractBaseGraph
     *                     of ColorNode nodes and DefaultEdge edges. Must not be null.
     * @param config       the configuration object containing necessary parameters
     *                     for the heuristic algorithm. Must not be null.
     */
    public Heuristic(AbstractBaseGraph<ColorNode, DefaultEdge> reachability, Config config) {
        Objects.requireNonNull(reachability);
        this.reachability = reachability;

        Objects.requireNonNull(config);
        this.config = config;
    }

    /**
     * Executes the heuristic processing on a graph given the node data map.
     * This includes identifying the entry point, scoring the graph, and determining
     * the top paths based on the scores.
     *
     * @param nodeData a map linking each {@code ColorNode} in the graph to its associated
     *                 {@code HeuristicNodeData}, providing scoring and heuristic information
     *                 necessary for graph processing.
     */
    public void run(Map<ColorNode, HeuristicNodeData> nodeData) {
        ColorNode entryPoint;

        try {
            entryPoint = Util.getEntryPoint(reachability);
        } catch (NoSuchElementException noSuchElementException) {
            LOGGER.error("Unable to find entry point", noSuchElementException);
            return;
        }

        // score main graph
        scoreGraph(entryPoint, reachability, nodeData);

        // set orderedPaths to paths with the highest scores (highest to lowest)
        topPaths(entryPoint);
    }

    private Double overallScore(HeuristicNodeData heuristicNodeData) {
        return heuristicNodeData.getScore(METHOD) + heuristicNodeData.getScore(LOC);
    }

    private void scoreGraph(ColorNode entryPoint, AbstractBaseGraph<ColorNode, DefaultEdge> reachability, Map<ColorNode, HeuristicNodeData> nodeData) {
        LOGGER.info("Heuristic running for {}", entryPoint.label);

        Map<ScoreTypes, AbstractScoringProvider> scoringProviders = new HashMap<>();
        scoringProviders.put(METHOD, new MissedMethods(config, reachability));
        scoringProviders.put(LOC, new MissedLinesOfCode(config, reachability));

        DepthFirstIterator<ColorNode, DefaultEdge> dfi = new DepthFirstIterator<>(reachability, entryPoint);
        dfi.addTraversalListener(new HeuristicTraversalListener(reachability, nodeData, scoringProviders, this::overallScore));

        // traverse the graph to set the scores
        while (dfi.hasNext()) {
            ColorNode n = dfi.next();
            LOGGER.debug("Scoring node {}", n.label);
        }
    }

    @SuppressWarnings("unchecked")
    private AbstractBaseGraph<ColorNode, DefaultEdge> getCloneOfGraph() {
        return (AbstractBaseGraph<ColorNode, DefaultEdge>) reachability.clone();
    }

    private void topPaths(ColorNode entryPoint) {
        orderedPaths.clear();

        // clone reachability, so we can remove nodes as needed
        AbstractBaseGraph<ColorNode, DefaultEdge> graph = getCloneOfGraph();

        // result
        for (int i = 0; i < NUM_TOP_PATHS; i++) {
            // score graph (start with empty map)
            Map<ColorNode, HeuristicNodeData> nodeData = new HashMap<>();
            scoreGraph(entryPoint, graph, nodeData);

            List<ColorNode> working = new ArrayList<>();
            HighScoreIterator<ColorNode, DefaultEdge> highScoreIterator = new HighScoreIterator<>(graph, entryPoint, nodeData);

            while (highScoreIterator.hasNext()) {
                ColorNode colorNode = highScoreIterator.next();
                working.add(colorNode);

                if (graph.outDegreeOf(colorNode) == 0) {
                    // found leaf... so at the end of path
                    orderedPaths.add(working.toArray(ColorNode[]::new));

                    // remove node and we will search again for next path
                    if (!colorNode.equals(entryPoint))
                        graph.removeVertex(colorNode);
                    break;
                }
            }

            heuristicPathNodeData.add(nodeData);
        }
    }

    /**
     * Serializes the provided map of {@code ColorNode} to {@code HeuristicNodeData} into a byte array format.
     *
     * @param nodeData the map containing {@code ColorNode} keys and their associated {@code HeuristicNodeData} values
     *                 to be serialized. This map represents the data structure to be converted into a byte array.
     * @return a byte array representing the serialized form of the input map.
     * @throws IOException if an I/O error occurs during the serialization process.
     */
    public byte[] serializedObject(Map<ColorNode, HeuristicNodeData> nodeData) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(nodeData);
            return bos.toByteArray();
        }
    }

    /**
     * Deserializes the provided byte array into a map of {@code ColorNode} to {@code HeuristicNodeData}.
     * The method reconstructs {@code ColorNode} objects using their labels, ensuring compatibility
     * with the provided graph. This is necessary because the original {@code ColorNode} objects may
     * not survive serialization.
     *
     * @param g the graph containing the original {@code ColorNode} objects, represented as an
     *          {@code AbstractBaseGraph} of {@code ColorNode} nodes and {@code DefaultEdge} edges.
     *          Used to remap deserialized nodes based on their labels.
     * @param bytes the byte array representing the serialized {@code Map<ColorNode, HeuristicNodeData>} data.
     *              This array contains serialized {@code ColorNode} and {@code HeuristicNodeData} pairs.
     * @return a map linking the reconstructed {@code ColorNode} objects from the provided graph
     *         to their corresponding {@code HeuristicNodeData}.
     * @throws IOException if an I/O error occurs during deserialization.
     * @throws ClassNotFoundException if the class of a serialized object cannot be found.
     */
    public static Map<ColorNode, HeuristicNodeData> deserializeNodeData(AbstractBaseGraph<ColorNode, DefaultEdge> g, byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStreamCustomLoader(bis, SUTClassLoader.get())) {

            //noinspection unchecked
            Map<ColorNode, HeuristicNodeData> deNodeData = (Map<ColorNode, HeuristicNodeData>) in.readObject();

            // build label to vertex map for g
            Map<String, ColorNode> labelToColorNode = new HashMap<>();
            for (ColorNode colorNode : g.vertexSet()) {
                labelToColorNode.put(colorNode.label, colorNode);
            }

            // convert to new objects using labels, since they didn't live through serialization
            Map<ColorNode, HeuristicNodeData> nodeData = new HashMap<>();
            for (Map.Entry<ColorNode, HeuristicNodeData> colorNodeHeuristicNodeDataEntry : deNodeData.entrySet()) {
                ColorNode colorNode = colorNodeHeuristicNodeDataEntry.getKey();
                HeuristicNodeData heuristicNodeData = colorNodeHeuristicNodeDataEntry.getValue();

                String label = colorNode.label;
                ColorNode newColorNode = labelToColorNode.get(label);
                nodeData.put(newColorNode, heuristicNodeData);
            }

            return nodeData;
        }
    }

    /**
     * Generates a DOT representation of a graph based on the provided node data and configuration.
     * This method processes the graph, including calculating edge attributes and vertex identifiers,
     * and returns the serialized DOT representation as a byte array.
     *
     * @param nodeData a map linking each {@code ColorNode} in the graph to its associated {@code HeuristicNodeData},
     *                 providing heuristic information and scoring used to generate the DOT representation.
     * @return a byte array containing the serialized DOT representation of the graph.
     * @throws IOException if an I/O error occurs while generating the DOT representation.
     */
    public byte[] generateGraphDOT(Map<ColorNode, HeuristicNodeData> nodeData) throws IOException {
        Map<DefaultEdge, BitSet> edges = new HashMap<>();

        // setup edges that appear in the orderedPaths
        for (int p = 0; p < orderedPaths.size(); p++) {
            ColorNode[] path = orderedPaths.get(p);

            for (int i = 0; i < path.length - 1; i++) {
                DefaultEdge edge = reachability.getEdge(path[i], path[i + 1]);
                BitSet bitSet = edges.getOrDefault(edge, new BitSet(orderedPaths.size()));
                bitSet.set(p);
                edges.put(edge, bitSet);
            }
        }

        Export<ColorNode, DefaultEdge> exporter =
                (new Export.Builder<>(reachability))
                    .edgeAttributeProvider(
                        (edge) -> {
                            Map<String, Attribute> map = new LinkedHashMap<>();

                            if (edges.containsKey(edge)) {
                                BitSet bitSet = edges.get(edge);
                                int firstPathNum = bitSet.nextSetBit(0);

                                if (firstPathNum < config.pathImprovementColors.length) {
                                    map.put("color", DefaultAttribute.createAttribute(config.pathImprovementColors[firstPathNum]));
                                    map.put("penwidth", DefaultAttribute.createAttribute(3.0));

                                    String stringPathNumbers = bitSet.stream().mapToObj(String::valueOf).collect(Collectors.joining("/"));
                                    map.put("label", DefaultAttribute.createAttribute("P" + stringPathNumbers));
                                }
                            }

                            return map;
                        }
                    )
                    .vertexIdProvider(
                        (colorNode) -> {
                            HeuristicNodeData h = nodeData.getOrDefault(colorNode, new HeuristicNodeData());
                            return dotFormat(colorNode.label + " [missed " + h.getMissedLinesOfCode() + "] [depth " + h.getDepth() + "] [scoreMethod " + h.getScore(METHOD) + "] [scoreLOC " + h.getScore(LOC) + "] [scoreOverall " + h.getScore(OVERALL) + "]");
                        }
                    )
                    .Build();

        LOGGER.info("Graph generated");

        return exporter.asByteArray();
    }
}