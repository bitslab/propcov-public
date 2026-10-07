package edu.uic.bitslab.propcov.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoGraphExceptionTest {
    @Test
    void testNoGraphException() {
        NoGraphException exception = new NoGraphException("Test");
        assertEquals("Test", exception.getMessage());
        assertNull(exception.getCause());
    }
}