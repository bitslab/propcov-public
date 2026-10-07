package edu.uic.bitslab.propcov.core.report;

import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LOCTrackerTest {

    /**
     * This class tests the `run` method in the `LOCTracker` class, which processes
     * a given file (Java class or JAR) and delegates the logic to build line-of-code
     * coverage details to the provided `AbstractCoverage` instance.
     */

    @Test
    void testRunWithClassFile() throws IOException {
        AbstractCoverage mockCoverage = mock(AbstractCoverage.class);
        LOCTracker tracker = new LOCTracker(mockCoverage);
        Path mockClassFile = Files.createTempFile("dummy", ".class");
        Map<String, LOCTracker.LOCDetail> locs = new HashMap<>();
        Map<String, String> inherit = new HashMap<>();

        tracker.run(mockClassFile, locs, inherit);

        verify(mockCoverage, times(1)).buildLOC(any(byte[].class), eq(locs), eq(inherit));

        Files.deleteIfExists(mockClassFile);
    }

    @Test
    void testRunWithJarFile() throws IOException {
        AbstractCoverage mockCoverage = mock(AbstractCoverage.class);
        LOCTracker tracker = new LOCTracker(mockCoverage);

        Path mockJarFile = Files.createTempFile("dummy", ".jar");
        JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(mockJarFile));
        jarOutputStream.putNextEntry(new java.util.jar.JarEntry("dummy.class"));
        jarOutputStream.close();

        Map<String, LOCTracker.LOCDetail> locs = new HashMap<>();
        Map<String, String> inherit = new HashMap<>();

        tracker.run(mockJarFile, locs, inherit);

        verify(mockCoverage, atLeastOnce()).buildLOC(any(byte[].class), eq(locs), eq(inherit));
        Files.deleteIfExists(mockJarFile);
    }

    @Test
    void testRunWithInvalidFileType() throws IOException {
        AbstractCoverage mockCoverage = mock(AbstractCoverage.class);
        LOCTracker tracker = new LOCTracker(mockCoverage);

        Path invalidFile = Files.createTempFile("invalid", ".txt");
        Map<String, LOCTracker.LOCDetail> locs = new HashMap<>();
        Map<String, String> inherit = new HashMap<>();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                tracker.run(invalidFile, locs, inherit)
        );

        assertEquals("Unsupported file type: " + invalidFile, exception.getMessage());
    }

    @Test
    void testRunWithNonExistentFile() throws IOException {
        AbstractCoverage mockCoverage = mock(AbstractCoverage.class);
        LOCTracker tracker = new LOCTracker(mockCoverage);

        Path nonExistentFile = Path.of("nonexistent.class");
        Map<String, LOCTracker.LOCDetail> locs = new HashMap<>();
        Map<String, String> inherit = new HashMap<>();

        tracker.run(nonExistentFile, locs, inherit);
        verify(mockCoverage, never()).buildLOC(any(byte[].class), any(), any());
    }

    @Test
    void testRunWithEmptyLocsAndInheritMaps() throws IOException {
        AbstractCoverage mockCoverage = mock(AbstractCoverage.class);
        LOCTracker tracker = new LOCTracker(mockCoverage);

        Path mockClassFile = Files.createTempFile("dummy", ".class");
        Map<String, LOCTracker.LOCDetail> locs = new HashMap<>();
        Map<String, String> inherit = new HashMap<>();

        tracker.run(mockClassFile.toString(), locs, inherit);

        assertTrue(locs.isEmpty());
        assertTrue(inherit.isEmpty());
        verify(mockCoverage, times(1)).buildLOC(any(byte[].class), eq(locs), eq(inherit));

        Files.deleteIfExists(mockClassFile);
    }

    @Test
    void testRunWithCoverageDelegation() throws IOException {
        AbstractCoverage mockCoverage = mock(AbstractCoverage.class);
        LOCTracker tracker = new LOCTracker(mockCoverage);

        Path mockClassFile = Files.createTempFile("dummy", ".class");
        Map<String, LOCTracker.LOCDetail> locs = new HashMap<>();
        Map<String, String> inherit = new HashMap<>();
        byte[] mockBytes = Files.readAllBytes(mockClassFile);

        tracker.run(mockClassFile, locs, inherit);

        verify(mockCoverage).buildLOC(eq(mockBytes), eq(locs), eq(inherit));
        Files.deleteIfExists(mockClassFile);
    }

    @Test
    void getLOC() {
        AbstractCoverage mockCoverage = mock(AbstractCoverage.class);
        LOCTracker tracker = new LOCTracker(mockCoverage);

        Set<String> unknownMethods = new HashSet<>();
        Map<String, LOCTracker.LOCDetail> locs = new HashMap<>();
        Map<String, Long> linesOfCode = new HashMap<>();

        // null
        tracker.getLOCs(null, locs, linesOfCode);
        assertTrue(locs.isEmpty());
        assertTrue(linesOfCode.isEmpty());

        // empty
        tracker.getLOCs(unknownMethods, locs, linesOfCode);
        assertTrue(locs.isEmpty());
        assertTrue(linesOfCode.isEmpty());

        unknownMethods.add("unknownMethod1");
        unknownMethods.add("method");
        LOCTracker.LOCDetail.Builder builder = new LOCTracker.LOCDetail.Builder();
        builder.addCovered(1);
        builder.addMissed(2);
        locs.put("method", builder.build());
        tracker.getLOCs(unknownMethods, locs, linesOfCode);
        assertEquals(2, linesOfCode.get("method").longValue());
        assertFalse(linesOfCode.containsKey("unknownMethod1"));
    }

    @Test
    void contains() {
        Map<String, LOCTracker.LOCDetail> locs = new HashMap<>();
        LOCTracker.LOCDetail.Builder builder;

        builder = new LOCTracker.LOCDetail.Builder();
        builder.addCovered(1);
        locs.put("method1", builder.build());
        assertTrue(locs.get("method1").contains(1));

        builder = new LOCTracker.LOCDetail.Builder();
        builder.addMissed(2);
        locs.put("method2", builder.build());
        assertFalse(locs.get("method2").contains(1));
        assertTrue(locs.get("method2").contains(2));
    }

    @Test
    void firstLine() {
        Map<String, LOCTracker.LOCDetail> locs = new HashMap<>();
        LOCTracker.LOCDetail.Builder builder = new LOCTracker.LOCDetail.Builder();
        locs.put("method0", builder.build());
        assertThrows(NoSuchElementException.class, () -> locs.get("method0").firstLine());

        builder = new LOCTracker.LOCDetail.Builder();
        builder.addCovered(1);
        locs.put("method1", builder.build());
        assertEquals(1, locs.get("method1").firstLine());

        builder = new LOCTracker.LOCDetail.Builder();
        builder.addMissed(2);
        locs.put("method2", builder.build());
        assertEquals(2, locs.get("method2").firstLine());

        builder = new LOCTracker.LOCDetail.Builder();
        builder.addCovered(1);
        builder.addMissed(2);
        locs.put("method3", builder.build());
        assertEquals(1, locs.get("method3").firstLine());
    }
}