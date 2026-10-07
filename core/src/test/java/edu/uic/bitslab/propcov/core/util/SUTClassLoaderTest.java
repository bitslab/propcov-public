package edu.uic.bitslab.propcov.core.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SUTClassLoaderTest {

    @Test
    void addPaths() {
        Path[] paths = { Path.of("#") };
        assertThrows(RuntimeException.class, () -> SUTClassLoader.addPaths(paths));
    }
}