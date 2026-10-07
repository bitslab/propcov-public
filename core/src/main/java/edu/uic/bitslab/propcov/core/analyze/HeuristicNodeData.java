package edu.uic.bitslab.propcov.core.analyze;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents data associated with a heuristic node, including scoring metrics,
 * missed lines of code, and node depth, used to analyze and evaluate nodes in
 * a specific context..
 */
public class HeuristicNodeData implements Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 2024051615580000000L;

    /**
     * Enum representing different types of scores associated with a heuristic node.
     * These scores are used to evaluate and analyze various metrics related to
     * the node's functionality or characteristics.
     */
    public enum ScoreTypes {
        /**
         * Represents the score type associated with method-level metrics.
         */
        METHOD,
        /**
         * Represents the Lines of Code (LOC) score type in the context of a heuristic node.
         */
        LOC,
        /**
         * Represents the overall score of a heuristic node.
         */
        OVERALL
    }

    private final Map<ScoreTypes, Double> scores = new HashMap<>();
    private long missedLinesOfCode = 0L;
    private long depth = 0L;

    /**
     * Adds a score to the specified {@code ScoreTypes} and updates the cumulative score.
     *
     * @param type The type of score to be updated. Valid values are defined in the {@code ScoreTypes} enum.
     * @param score The value of the score to be added.
     */
    public void addScore(ScoreTypes type, double score) {
        scores.merge(type, score, Double::sum);
    }

    /**
     * Retrieves the score associated with the specified {@code ScoreTypes}.
     * If no score is present for the given type, it returns a default value of 0.00.
     *
     * @param type The type of score to retrieve. Valid values are defined in the {@code ScoreTypes} enum.
     * @return The score corresponding to the specified {@code ScoreTypes}, or 0.00 if not found.
     */
    public Double getScore(ScoreTypes type) {
        return scores.getOrDefault(type, 0.00);
    }

    /**
     * Adds the specified number of missed lines of code to the current total.
     *
     * @param m the number of missed lines of code to add
     */
    public void addMissedLinesOfCode(long m) {
        missedLinesOfCode += m;
    }
    /**
     * Retrieves the number of missed lines of code associated with this heuristic node data.
     *
     * @return the total number of missed lines of code as a long value.
     */
    public long getMissedLinesOfCode() {
        return missedLinesOfCode;
    }

    /**
     * Sets the depth value for this node.
     *
     * @param d the depth to be assigned to the node
     */
    public void setDepth(long d) {
        depth = d;
    }
    /**
     * Returns the depth of the heuristic node.
     *
     * @return the depth of the node as a long value.
     */
    public long getDepth() {
        return depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeuristicNodeData that = (HeuristicNodeData) o;
        return this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(scores, missedLinesOfCode, depth);
    }

    @Override
    public HeuristicNodeData clone() throws CloneNotSupportedException {
        return (HeuristicNodeData) super.clone();
    }
}
