package edu.uic.bitslab.propcov.core.util.timer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Test RunTimer
 */
public class RunTimerTest {
    @Test
    void testTimer() throws TimerException, InterruptedException {
        Logger logger = mock(Logger.class);

        RunTimer runTimer = new RunTimer("test1", logger);
        assertTrue(runTimer.elapsed() > 0);
        assertThrows(TimerException.class, runTimer::start);

        RunTimer runTimer2 = new RunTimer("test1", logger, false);
        assertEquals(-1, runTimer2.elapsed());
        assertThrows(TimerException.class, runTimer2::stop);
        runTimer2.start();
        Thread.sleep(1000);
        runTimer2.stop();
        assertTrue(runTimer2.elapsed() > 0);
        assertTrue(runTimer2.elapsedInSeconds() > 0);
        assertEquals((double) runTimer2.elapsed() / 1_000_000_000, runTimer2.elapsedInSeconds());
    }
}
