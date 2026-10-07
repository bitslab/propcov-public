package edu.uic.bitslab.propcov.core.analysisframework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisFrameworkExceptionTest {
    @Test
    void testAnalysisFrameworkException() {
        AnalysisFrameworkException exception = new AnalysisFrameworkException("Test");
        assertEquals("Test", exception.getMessage());
        assertNull(exception.getCause());

        AnalysisFrameworkException exception2 = new AnalysisFrameworkException("Test2", new Exception("Cause"));
        assertEquals("Test2", exception2.getMessage());
        assertEquals("Cause", exception2.getCause().getMessage());
    }
}