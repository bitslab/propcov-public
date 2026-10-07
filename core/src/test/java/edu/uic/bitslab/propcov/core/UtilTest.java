package edu.uic.bitslab.propcov.core;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.arbitraries.ArrayArbitrary;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeProperty;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.jqwik.api.Arbitraries.bytes;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class UtilTest {
    private Path tempDir;

    @BeforeEach
    void setupTest() throws IOException {
        tempDir = Files.createTempDirectory("UtilTest");
    }

    @AfterEach
    void teardownTest() throws IOException {
        if (tempDir != null) {
            Util.recursiveRemove(tempDir);
            tempDir = null;
        }
    }

    @BeforeProperty @BeforeTry
    void setupProperty() throws IOException {
        tempDir = Files.createTempDirectory("UtilTest");
    }

    @AfterProperty @AfterTry
    void teardownProperty() throws IOException {
        if (tempDir != null) {
            Util.recursiveRemove(tempDir);
            tempDir = null;
        }
    }

    @Test
    void getCommonClassName() {
        assertEquals("a.b.c", Util.getCommonClassName("a.b.c", null));
        assertEquals("a.b.c", Util.getCommonClassName("a.b.c", "a.b.c"));
        assertTrue(Util.getCommonClassName("a.b.c", "b.c.d").isEmpty());
        assertEquals("a", Util.getCommonClassName("a.b.c", "a.c.d"));
        assertEquals("a.b.c", Util.getCommonClassName("a.b.c", "a.b.c.d"));
    }

    @Test
    void descriptionToPropertyTypes() throws ClassNotFoundException {
        assertThrows(Error.class, () -> Util.descriptionToPropertyTypes("invalid"));
        assertThrows(Error.class, () -> Util.descriptionToPropertyTypes("(V)"));
        assertNull(Util.descriptionToPropertyTypes("(A)"));

        // This condition and truth branch seems suspect.  Need to find a real example of this to confirm
        // this is doing what is expected
        //
        //   int semi =
        //                        (description.substring(i).startsWith(AbstractAnalysisFramework.PARAM_NOT_FOUND) && !description.substring(i).startsWith(AbstractAnalysisFramework.PARAM_NOT_FOUND + ";"))
        //                        ? (i + AbstractAnalysisFramework.PARAM_NOT_FOUND.length())
        //                        : description.indexOf(';', i);
//        assertNull(Util.descriptionToPropertyTypes("(Ljava.lang.String.ParamNotFound)"));
        assertEquals(0, Objects.requireNonNull(Util.descriptionToPropertyTypes("()I")).length);
        assertEquals(0, Objects.requireNonNull(Util.descriptionToPropertyTypes("(")).length);


        // happy path
        Class<?>[] c = Util.descriptionToPropertyTypes("(BCDFIJSZ[ZLjava.lang.String;)V");
        assertNotNull(c);
        assertEquals(10, c.length);
        assertEquals(Byte.TYPE, c[0]);
        assertEquals(Character.TYPE, c[1]);
        assertEquals(Double.TYPE, c[2]);
        assertEquals(Float.TYPE, c[3]);
        assertEquals(Integer.TYPE, c[4]);
        assertEquals(Long.TYPE, c[5]);
        assertEquals(Short.TYPE, c[6]);
        assertEquals(Boolean.TYPE, c[7]);
        assertEquals("boolean[]", c[8].getTypeName());
        assertEquals(String.class, c[9]);
    }

    @Test
    void targetContainsCopyOfAllFilesFromSourceInvalidSource() throws IOException {
        try (MockedStatic<Files> mockedStatic = mockStatic(Files.class)) {
            Path source = tempDir.resolve("invalid");
            mockedStatic.clearInvocations();

            Util.recursiveCopy(source, source);

            // Files.walk(source) should not be called
            //noinspection resource
            mockedStatic.verify(() -> Files.walk(eq(source)), times(0));
        }
    }

    @Property
    void targetContainsCopyOfAllFilesFromSource(@ForAll @Size(min=1,max=20) List<@Size(min=1, max=6) List<@NumericChars @AlphaChars @Chars(value = {'-', '.', ' '}) @StringLength(min=1,max=30) String>> paths) throws IOException {
        Path source = tempDir.resolve("source");
        if (!source.toFile().mkdirs()) throw new RuntimeException("Could not create source directory");

        Path target = tempDir.resolve("target");
        if (!target.toFile().mkdirs()) throw new RuntimeException("Could not create target directory");

        // Setup bytes provider
        ArrayArbitrary<Byte, byte[]> fileBytes = bytes().array(byte[].class).ofMaxSize(100);
        assertNotNull(fileBytes);

        // source files for building as well as for comparing with destination
        Map<Path, byte[]> sourceFiles = paths.stream()
            .filter(parts -> parts.stream().noneMatch( part -> part.startsWith(".") ))
            .map(parts -> Path.of(parts.get(0), parts.subList(1, parts.size()).toArray(String[]::new)))
            .distinct()
            .map(path -> Map.entry(path, Objects.requireNonNull(fileBytes.sample())) )
            .filter( entry -> {
                Path fullPath = source.resolve(entry.getKey());
                byte[] bytes = entry.getValue();

                if (!fullPath.toFile().getParentFile().mkdirs()) return false;

                try {
                    // write source bytes
                    Files.write(fullPath, bytes);
                    return true;

                } catch (IOException e) {
                    return false;

                }
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // run SUT
        Util.recursiveCopy(source, target);

        // confirm target files match expectation
        sourceFiles.forEach((path, expectedBytes) -> {
            Path fullPath = target.resolve(path);
            assertTrue(Files.exists(fullPath));

            try {
                byte[] actualBytes = Files.readAllBytes(fullPath);
                assertArrayEquals(expectedBytes, actualBytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}