package edu.uic.bitslab.propcov.core.graph;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class ColorNodeTest {

    @Test
    void hasSomeCoverage() {
        ColorNode c0 = new ColorNode.Builder("A").type("COVERAGE0").Build();
        assertFalse(c0.hasSomeCoverage());

        ColorNode c1 = new ColorNode.Builder("A").type("COVERAGE1").Build();
        assertTrue(c1.hasSomeCoverage());

        ColorNode c2 = new ColorNode.Builder("A").type("COVERAGE2").Build();
        assertTrue(c2.hasSomeCoverage());

        ColorNode c3 = new ColorNode.Builder("A").type("COVERAGE3").Build();
        assertTrue(c3.hasSomeCoverage());

        ColorNode c4 = new ColorNode.Builder("A").type("COVERAGE4").Build();
        assertTrue(c4.hasSomeCoverage());

    }

    @SuppressWarnings({"SimplifiableAssertion", "EqualsWithItself", "ConstantValue"})
    @Test
    void testEquals() {
        ColorNode colorNode = new ColorNode.Builder("A").Build();
        assertTrue(colorNode.equals(colorNode));
        assertFalse(colorNode.equals(null));
        assertFalse(colorNode.equals(new Object()));

        ColorNode colorNode2 = new ColorNode.Builder("A").Build();
        assertTrue(colorNode.equals(colorNode2));
    }

    @Test
    void testToString() {
        ColorNode colorNode = new ColorNode.Builder("A").Build();
        assertEquals("A", colorNode.toString());
    }

    @Test
    void testSerialize() throws IOException, ClassNotFoundException {
        ColorNode c1 = new ColorNode.Builder("A").Build();
        byte[] s1;
        ColorNode c2;

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {

            out.writeObject(c1);
            s1 = bos.toByteArray();
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(s1);
             ObjectInputStream in = new ObjectInputStream(bis)) {

            c2 = (ColorNode) in.readObject();
        }

        assertNotSame(c1, c2);
        assertEquals(c1, c2);
    }
}