package edu.uic.bitslab.propcov.core.analyze;

import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents coverage data for source files, including detailed coverage metrics
 * such as lines covered, lines missed, and total lines.
 */
public class CoverageData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * A map that holds detailed coverage metrics for source files.
     * The key represents the name or identifier of a source file,
     * and the value is an instance of {@code CoverageDetail}, which contains
     * detailed information about the coverage for the corresponding file.
     */
    public final Map<String, CoverageDetail> details = new HashMap<>();
    /**
     * Represents the total number of lines of code that have been executed or covered during
     * testing processes, aggregated from the detailed coverage data of all source files.
     * This value is computed from the unique lines covered across all file coverage details.
     */
    public final long linesCovered;
    /**
     * Represents the count of source code lines that were not covered during execution
     * based on the associated coverage data.
     */
    public final long linesMissed;
    /**
     * Represents the total number of lines available in the source files covered by this instance of coverage data.
     * This value is the sum of {@code linesCovered} and {@code linesMissed}, indicating the full extent of lines
     * in the analyzed source files.
     */
    public final long linesTotal;

    /**
     * Enumeration representing the types of coverage data included in a coverage analysis.
     */
    public enum CoverageDataType {
        /**
         * This value specifies the inclusion of general coverage metrics (e.g. JaCoCo).
         */
        Coverage,
        /**
         * This value specifies the inclusion of property-based coverage data in the coverage analysis.
         */
        PropCov
    }

    /**
     * Constructs a new instance of {@code CoverageData} using the provided map of coverage details.
     * The constructor initializes the coverage data metrics (covered lines, missed lines, and total lines)
     * based on the aggregated data from the input map.
     *
     * @param coverageDetails a map where the key is a string representing the identifier of a source file
     *                        and the value is an instance of {@code CoverageDetail}, containing detailed
     *                        coverage information for that file.
     */
    public CoverageData(Map<String, CoverageDetail> coverageDetails) {
        details.putAll(coverageDetails);

        // get size of all lines covered (without repeats)
        Map<CoverageDetail.SourceFileID, Set<Long>> allCoverageMap = new HashMap<>();
        details.values().stream()
            .map(e -> e.mapOfLinesCovered)
            .forEach(allCoverageMap::putAll);
        linesCovered = allCoverageMap.values().stream().mapToLong(Set::size).sum();

        // get size of all missed lines (without repeats)
        Map<CoverageDetail.SourceFileID, Set<Long>> allMissedMap = new HashMap<>();
        details.values().stream()
                .map(e -> e.mapOfLinesMissed)
                .forEach(allMissedMap::putAll);
        linesMissed = allMissedMap.values().stream().mapToLong(Set::size).sum();

        // calculate total
        linesTotal = linesCovered + linesMissed;
    }
}
