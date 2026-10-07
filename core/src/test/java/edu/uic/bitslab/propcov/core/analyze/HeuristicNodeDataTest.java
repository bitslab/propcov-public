package edu.uic.bitslab.propcov.core.analyze;

import org.junit.jupiter.api.Test;

import static edu.uic.bitslab.propcov.core.analyze.HeuristicNodeData.ScoreTypes.*;
import static org.junit.jupiter.api.Assertions.*;

class HeuristicNodeDataTest {

    @Test
    void nodeDataTest() throws CloneNotSupportedException {
        HeuristicNodeData heuristicNodeData = new HeuristicNodeData();
        assertEquals(0, heuristicNodeData.getScore(OVERALL));
        assertEquals(0, heuristicNodeData.getScore(LOC));
        assertEquals(0, heuristicNodeData.getScore(METHOD));
        assertEquals(0, heuristicNodeData.getMissedLinesOfCode());

        heuristicNodeData.addScore(OVERALL, 1.0);
        assertEquals(1.0, heuristicNodeData.getScore(OVERALL));

        heuristicNodeData.addScore(LOC, 2.2);
        assertEquals(2.2, heuristicNodeData.getScore(LOC));

        heuristicNodeData.addScore(METHOD, 3.3);
        assertEquals(3.3, heuristicNodeData.getScore(METHOD));

        heuristicNodeData.addMissedLinesOfCode(4);
        assertEquals(4, heuristicNodeData.getMissedLinesOfCode());

        heuristicNodeData.addMissedLinesOfCode(Long.MAX_VALUE - 4);
        assertEquals(Long.MAX_VALUE, heuristicNodeData.getMissedLinesOfCode());

        heuristicNodeData.addMissedLinesOfCode(0);
        assertEquals(Long.MAX_VALUE, heuristicNodeData.getMissedLinesOfCode());

        heuristicNodeData.setDepth(1);
        assertEquals(1, heuristicNodeData.getDepth());

        heuristicNodeData.setDepth(123);
        assertEquals(123, heuristicNodeData.getDepth());

        assertEquals(heuristicNodeData, heuristicNodeData);
        HeuristicNodeData heuristicNodeDataClone = heuristicNodeData.clone();
        assertEquals(heuristicNodeData, heuristicNodeDataClone);
        assertNotSame(heuristicNodeData, heuristicNodeDataClone);

        HeuristicNodeData heuristicNodeData2 = new HeuristicNodeData();
        assertNotEquals(heuristicNodeData, heuristicNodeData2);

        heuristicNodeData2.setDepth(123);
        heuristicNodeData2.addMissedLinesOfCode(Long.MAX_VALUE);
        heuristicNodeData2.addScore(OVERALL, 1.0);
        heuristicNodeData2.addScore(LOC, 2.2);
        heuristicNodeData2.addScore(METHOD, 3.3);

        assertEquals(heuristicNodeData, heuristicNodeData2);
        assertNotSame(heuristicNodeData, heuristicNodeDataClone);

        //noinspection SimplifiableAssertion,ConstantValue
        assertFalse(heuristicNodeData.equals(null));
        //noinspection SimplifiableAssertion
        assertFalse(heuristicNodeData.equals(new Object()));
    }
}