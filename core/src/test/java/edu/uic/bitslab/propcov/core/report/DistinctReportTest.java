package edu.uic.bitslab.propcov.core.report;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistinctReportTest {
    @Test
    void testDistinctReport() {

        Set<Long> covered = Set.of(1L, 2L, 3L);
        Set<Long> missed = Set.of(4L, 5L, 6L);
        Set<Long> total = Set.of(1L, 2L, 3L, 4L, 5L, 6L);
        Map<String, Long> unknownMethodsToLOC = Map.of("unknownMethod1", 1L, "unknownMethod2", 2L);

        DistinctReport distinctReport = new DistinctReport(covered, missed, unknownMethodsToLOC);
        assertEquals(unknownMethodsToLOC, distinctReport.unknownMethodsToLOC);
        assertEquals(covered, distinctReport.totalLineStats.covered);
        assertEquals(total, distinctReport.totalLineStats.total);
    }

}