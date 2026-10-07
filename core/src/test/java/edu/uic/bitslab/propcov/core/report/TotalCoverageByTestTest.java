package edu.uic.bitslab.propcov.core.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotalCoverageByTestTest {

    @Test
    void TotalCoverageByTest() {
        TotalCoverageByTest totalCoverageByTest = new TotalCoverageByTest("test", 1.2, 0.9);
        assertEquals("test", totalCoverageByTest.testName);
        assertEquals(1.2, totalCoverageByTest.methodScore);
        assertEquals(0.9, totalCoverageByTest.locScore);
        assertNull(totalCoverageByTest.propCov);
        assertNull(totalCoverageByTest.other);
    }
}