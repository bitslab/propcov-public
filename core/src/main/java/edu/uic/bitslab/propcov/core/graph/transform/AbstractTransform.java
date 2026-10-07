package edu.uic.bitslab.propcov.core.graph.transform;

import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.graph.AbstractNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

/**
 * AbstractTransform is an abstract base class designed for transforming graph structures.
 * It provides a blueprint for processing a graph and transforming its nodes into a different structure or representation.
 * Classes extending AbstractTransform must implement the process method to define their specific transformation logic.
 */
abstract public class AbstractTransform {
    /**
     * Represents a graph structure where nodes are defined as Strings
     * and edges are of type DefaultEdge. This variable is immutable
     * and is intended to be manipulated or analyzed by classes extending
     * the AbstractTransform base class.
     */
    public final Graph<String, DefaultEdge> graph;

    /**
     * Represents an instance of {@code AbstractCoverage} associated with the transformation process.
     * This variable holds coverage-related information or logic and is used to analyze and process
     * graph structures for coverage computations or verifications.
     */
    public final AbstractCoverage coverage;

    /**
     * Constructs an instance of the AbstractTransform class.
     *
     * @param graph the graph structure to be processed, represented as a {@code Graph<String, DefaultEdge>} instance
     * @param coverage the coverage object used for analyzing or transforming the graph, represented as an {@code AbstractCoverage} instance
     */
    public AbstractTransform(Graph<String, DefaultEdge> graph, AbstractCoverage coverage) {
        this.graph = graph;
        this.coverage = coverage;
    }

    /**
     * Transforms the input graph into a new graph representation where the nodes extend AbstractNode.
     * Subclasses define the specific logic for this transformation process, such as applying custom rules or mappings.
     *
     * @return a new graph where the nodes are derived from AbstractNode and the edges remain of type DefaultEdge
     */
    public abstract Graph<? extends AbstractNode, DefaultEdge> process();
}
