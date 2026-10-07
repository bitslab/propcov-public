package edu.uic.bitslab.propcov.core.analyze.scoringprovider;

import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.graph.ColorNode;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * AbstractScoringProvider provides a base class for implementing scoring mechanisms
 * for nodes in a graph. Subclasses should define the scoring logic by implementing
 * the {@link #score(ColorNode)} method.
 * This class is designed to operate on directed graphs where each node may be
 * assigned a score. The provided graph and configuration objects are common dependencies
 * for subclasses enabling customization and scoring analysis.
 */
public abstract class AbstractScoringProvider {
    final AbstractBaseGraph<ColorNode, DefaultEdge> graph;
    final Config config;

    /**
     * Constructs an AbstractScoringProvider with the specified configuration and graph.
     *
     * @param config the configuration object that provides settings or parameters
     *               necessary for scoring operations
     * @param graph  the graph structure containing nodes and edges on which scoring
     *               will be performed
     */
    public AbstractScoringProvider(Config config, AbstractBaseGraph<ColorNode, DefaultEdge> graph) {
        this.config = config;
        this.graph = graph;
    }

    /**
     * Calculates the score for the given vertex in the graph.
     * Subclasses should implement this method to define specific
     * scoring mechanisms based on the context and configuration.
     *
     * @param vertex the {@link ColorNode} for which the score is to be calculated
     * @return the computed score as a double value for the specified vertex
     */
    public abstract double score(ColorNode vertex);
}
