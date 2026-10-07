package edu.uic.bitslab.propcov.core.report;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class representing a distinct report for analyzing and aggregating
 * line coverage statistics. This class provides encapsulated data about
 * covered and missed lines as well as unknown methods with their
 * associated lines of code (LOC).
 */
public class DistinctReport {
    /**
     * Represents aggregated statistics about line coverage within a report. This
     * object contains the total number of lines and the subset of those lines that
     * are covered in the analyzed code.
     */
    public final TotalLineStats totalLineStats;
    /**
     * A map that tracks unknown methods to their respective lines of code (LOC).
     * The key is a string representing the fully qualified method signature, and
     * the value is a long representing the total number of lines of code associated
     * with that method. This map serves as a way to identify and aggregate statistics
     * about methods that are unrecognized or unidentified during analysis.
     */
    public final Map<String, Long> unknownMethodsToLOC;

    /**
     * Constructs a DistinctReport object using the provided sets of covered and missed
     * lines, and the map of unknown methods to their corresponding lines of code (LOC).
     * This constructor calculates the total set of lines by combining the covered
     * and missed sets and initializes the TotalLineStats and unknown methods data.
     *
     * @param covered the set of line numbers that are covered
     * @param missed the set of line numbers that are missed
     * @param unknownMethodsToLOC a map of unknown method names to their associated lines of code (LOC)
     */
    public DistinctReport(Set<Long> covered, Set<Long> missed, Map<String, Long> unknownMethodsToLOC) {
        Set<Long> total = new HashSet<>();
        total.addAll(covered);
        total.addAll(missed);

        this.totalLineStats = new TotalLineStats(covered, total);
        this.unknownMethodsToLOC = unknownMethodsToLOC;
    }

    /**
     * Represents aggregated line coverage statistics. This class includes two key sets:
     * one for covered lines and one for all total lines analyzed. Instances of this class
     * are immutable and provide a consistent representation of coverage data.
     */
    public static class TotalLineStats {
        /**
         * A set containing line numbers that have been covered during line coverage analysis.
         */
        public final Set<Long> covered;
        /**
         * Represents the set of all analyzed line numbers within a coverage report.
         */
        public final Set<Long> total;

        private TotalLineStats(Set<Long> covered, Set<Long> total) {
            this.covered = Collections.unmodifiableSet(covered);
            this.total = Collections.unmodifiableSet(total);
        }
    }
}
