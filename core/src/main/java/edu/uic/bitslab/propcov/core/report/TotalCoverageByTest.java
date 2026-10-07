package edu.uic.bitslab.propcov.core.report;

import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;

/**
 * Represents the total code coverage information for a specific test case.
 * This class contains details about the test name, method coverage score,
 * line of code (LOC) coverage score, and additional coverage details for
 * property-based and coverage metrics.
 */
public class TotalCoverageByTest {
    /**
     * The name of the test case associated with the code coverage details.
     */
    public final String testName;
    /**
     * Represents the method coverage score for a specific test case.
     */
    public final double methodScore;
    /**
     * Represents the line of code (LOC) coverage score for a specific test case.
     * This score indicates the proportion of code lines covered during the
     * execution of the test case.
     */
    public final double locScore;
    /**
     * Represents the property-based coverage details for a specific test case.
     */
    public CoverageDetail propCov;
    /**
     * Holds coverage details for another coverage system for a specific test case.
     */
    public CoverageDetail other;

    TotalCoverageByTest(String testName, double methodScore, @SuppressWarnings("SameParameterValue") double locScore) {
        this.testName = testName;
        this.methodScore = methodScore;
        this.locScore = locScore;
    }
}
