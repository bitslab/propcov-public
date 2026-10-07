package edu.uic.bitslab.propcov.core.util;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static edu.uic.bitslab.propcov.core.util.EnumHelper.stringToCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnumHelperTest {
    enum TestCaseEnum {
        A, B, C;
    }

    @Test
    void testStringToCollection() {
        Collection<TestCaseEnum> expected1 = List.of(TestCaseEnum.A);
        Collection<TestCaseEnum> actual1 = stringToCollection("A", TestCaseEnum.class);
        assertEquals(new HashSet<>(expected1), new HashSet<>(actual1));

        Collection<TestCaseEnum> expected2 = List.of(TestCaseEnum.A, TestCaseEnum.B, TestCaseEnum.C);
        Collection<TestCaseEnum> actual2a = stringToCollection("A,B,C", TestCaseEnum.class);
        assertEquals(new HashSet<>(expected2), new HashSet<>(actual2a));
        Collection<TestCaseEnum> actual2b = stringToCollection("A~B~C", TestCaseEnum.class, "~");
        assertEquals(new HashSet<>(expected2), new HashSet<>(actual2b));
        Collection<TestCaseEnum> actual2c = stringToCollection("A\tB\tC", TestCaseEnum.class, "\t");
        assertEquals(new HashSet<>(expected2), new HashSet<>(actual2c));
        Collection<TestCaseEnum> actual2d = stringToCollection("A,B,C", TestCaseEnum.class, ",");
        assertEquals(new HashSet<>(expected2), new HashSet<>(actual2d));

        Collection<TestCaseEnum> expected3 = List.of();
        Collection<TestCaseEnum> actual3a = stringToCollection("", TestCaseEnum.class);
        assertEquals(new HashSet<>(expected3), new HashSet<>(actual3a));
        Collection<TestCaseEnum> actual3b = stringToCollection(null, TestCaseEnum.class);
        assertEquals(new HashSet<>(expected3), new HashSet<>(actual3b));
        Collection<TestCaseEnum> actual3c = stringToCollection("", TestCaseEnum.class, "~");
        assertEquals(new HashSet<>(expected3), new HashSet<>(actual3c));

        assertThrows(IllegalArgumentException.class, () -> stringToCollection("A,B,D", TestCaseEnum.class));
        assertThrows(IllegalArgumentException.class, () -> stringToCollection("A~B~D", TestCaseEnum.class, "~"));
    }
}