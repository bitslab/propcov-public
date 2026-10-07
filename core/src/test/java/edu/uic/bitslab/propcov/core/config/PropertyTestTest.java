package edu.uic.bitslab.propcov.core.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyTestTest {

    @Test
    void entryPointAsPath() {
        PropertyTest propertyTest = new PropertyTest("entryPoint", "entryPointValue");
        assertEquals(Path.of("entryPointValue"), propertyTest.entryPointAsPath());
    }
}