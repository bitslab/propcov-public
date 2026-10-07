package edu.uic.bitslab.propcov.core.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisConfigTest {

    @Test
    void testCloneCreatesEqualObject() throws CloneNotSupportedException {
        Set<AnalysisConfig.AnalysisFlag> analysisFlags = new HashSet<>();
        analysisFlags.add(AnalysisConfig.AnalysisFlag.EXCEPTION);

        AnalysisConfig originalConfig = new AnalysisConfig.Builder()
                .analysisFlags(analysisFlags)
                .analysisType(AnalysisConfig.AnalysisType.CHA)
                .build();

        AnalysisConfig clonedConfig = originalConfig.clone();

        assertNotNull(clonedConfig);
        assertEquals(originalConfig.analysisType, clonedConfig.analysisType);
        assertEquals(originalConfig.analysisFlags, clonedConfig.analysisFlags);
        assertNotSame(originalConfig, clonedConfig);
    }

    @Test
    void testCloneHandlesEmptyFlags() throws CloneNotSupportedException {
        AnalysisConfig originalConfig = new AnalysisConfig.Builder()
                .analysisFlags(new HashSet<>())
                .analysisType(AnalysisConfig.AnalysisType.CTA)
                .build();

        AnalysisConfig clonedConfig = originalConfig.clone();

        assertNotNull(clonedConfig);
        assertTrue(clonedConfig.analysisFlags.isEmpty());
        assertNotSame(originalConfig, clonedConfig);
    }

    @Test
    void testNullFlagsAnalysisType() {
        AnalysisConfig originalConfig = new AnalysisConfig.Builder()
                .analysisFlags(null)
                .analysisType(null)
                .build();

        assertTrue(originalConfig.analysisFlags.isEmpty());
        assertEquals(AnalysisConfig.AnalysisType.RTA, originalConfig.analysisType);
    }
}