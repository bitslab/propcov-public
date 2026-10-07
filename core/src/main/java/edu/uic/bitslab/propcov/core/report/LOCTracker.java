package edu.uic.bitslab.propcov.core.report;

import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;


/**
 * The LOCTracker class is responsible for analyzing class and jar files to determine
 * line-of-code (LOC) details for methods, including covered and missed lines.
 * It also provides functionality to populate methods and their corresponding LOC information
 * into data structures.
 */
public class LOCTracker {
    private static final Logger LOGGER = Util.getLogger(LOCTracker.class);
    private final AbstractCoverage coverage;

    /**
     * Initializes a new instance of the LOCTracker class with the provided coverage implementation.
     *
     * @param coverage the AbstractCoverage instance to be used for analyzing coverage data
     */
    public LOCTracker(AbstractCoverage coverage) {
        this.coverage = coverage;
    }

    private void runJar(Path jarFile, Map<String, LOCDetail> locs, Map<String, String> inherit) throws IOException {
        try(JarFile jar = new JarFile(jarFile.toFile())) {
            jar.stream()
                .filter( s -> s.getName().endsWith(".class"))
                .map( s -> {
                    try {
                        return jar.getInputStream(s).readAllBytes();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach( (clazzBytes) -> runClass(clazzBytes, locs, inherit) );
        }
    }

    private void runClass(Path classFile, Map<String, LOCDetail> locs, Map<String, String> inherit) {
        try {
            runClass(Files.readAllBytes(classFile), locs, inherit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void runClass(byte[] clazzBytes, Map<String, LOCDetail> locs, Map<String, String> inherit) {
        coverage.buildLOC(clazzBytes, locs, inherit);
    }

    /**
     * Represents the details of lines of code with classifications into missed and covered lines.
     */
    public static class LOCDetail {
        /**
         * A set containing line numbers that were not executed or covered during code analysis.
         * This set represents the missed lines and can be used to calculate coverage metrics
         * or verify unexecuted sections of code.
         */
        public final Set<Long> linesMissed;
        /**
         * A set containing line numbers that have been covered during code analysis.
         * Each value in the set represents a specific line of code that was executed or
         * verified as part of the analysis. This set is used to track which lines
         * of code have been successfully tested or utilized, contributing to overall
         * coverage metrics.
         */
        public final Set<Long> linesCovered;

        private LOCDetail(Builder builder) {
            linesMissed = Collections.unmodifiableSet(builder.linesMissed);
            linesCovered = Collections.unmodifiableSet(builder.linesCovered);
        }

        /**
         * Checks if a given line number is present in either the set of missed lines or the set of covered lines.
         *
         * @param l the line number to check
         * @return true if the line number is present in the linesMissed or linesCovered sets, false otherwise
         */
        public boolean contains(long l) {
            return linesMissed.contains(l) || linesCovered.contains(l);
        }

        /**
         * Computes the total number of lines by summing up the sizes of the sets
         * representing missed lines and covered lines.
         *
         * @return the total number of lines, which is the sum of the sizes from
         *         the linesMissed and linesCovered sets
         */
        public long linesTotal() {
            return linesMissed.size() + linesCovered.size();
        }

        /**
         * Determines the smallest line number among the missed and covered lines.
         * If neither set contains any line numbers, an exception is thrown.
         *
         * @return the smallest line number from the missed or covered lines
         * @throws NoSuchElementException if both the missed and covered line sets are empty
         */
        public long firstLine() {
            long firstMissed = linesMissed.stream().sorted().findFirst().orElse(Long.MAX_VALUE);
            long firstCovered = linesCovered.stream().sorted().findFirst().orElse(Long.MAX_VALUE);
            long minimum = Math.min(firstMissed, firstCovered);
            if (minimum == Long.MAX_VALUE) throw new NoSuchElementException("No lines found.");
            return minimum;
        }

        /**
         * A builder class for creating instances of the {@code LOCDetail} class. This class
         * provides methods to add missed and covered lines and construct a new {@code LOCDetail}
         * object based on the collected data.
         */
        public static class Builder {
            private final Set<Long> linesMissed = new HashSet<>();
            private final Set<Long> linesCovered = new HashSet<>();

            /**
             * Adds a missed line to the set of missed lines.
             *
             * @param lineNumber the line number that is missed and to be added to the set
             */
            public void addMissed(long lineNumber) {
                linesMissed.add(lineNumber);
            }

            /**
             * Adds a line number to the set of covered lines.
             *
             * @param lineNumber the line number to be added to the covered lines set
             */
            public void addCovered(long lineNumber) {
                linesCovered.add(lineNumber);
            }

            /**
             * Builds and returns a new {@code LOCDetail} instance based on the current state
             * of the {@code Builder}.
             *
             * @return a {@code LOCDetail} instance initialized with the missed and covered lines
             *         that were added to the {@code Builder}.
             */
            public LOCDetail build() {
                return new LOCDetail(this);
            }
        }

    }

    /**
     * Processes the given file and extracts or analyzes line-of-code (LOC) details
     * based on the provided mapping of LOC details and inheritance relationships.
     * Delegates the processing to another method that handles file paths.
     *
     * @param file the file path as a string to process
     * @param locs a map where the keys are method names and the values are {@code LOCDetail} objects representing
     *             missed and covered line information
     * @param inherit a map representing inheritance relationships where the key is a class name
     *                and the value is its parent class name
     * @throws IOException if an I/O error occurs during file processing
     */
    public void run(String file, Map<String, LOCDetail> locs, Map<String, String> inherit) throws IOException {
        run(Path.of(file), locs, inherit);
    }

    /**
     * Processes the specified input file to update line-of-code details, handling .class and .jar files.
     * If the input file is a .class file, it delegates processing to the runClass method.
     * If the input file is a .jar file, it delegates processing to the runJar method.
     * If the file type is unsupported, an exception is thrown.
     *
     * @param in the path to the input file to be processed; it must exist and either be a .class or .jar file
     * @param locs a map where the key represents method names and the value contains the details of lines of code
     * @param inherit a map containing method inheritance information where the key is the child method and the value is the parent method
     * @throws IOException if an I/O error occurs during processing
     * @throws UnsupportedOperationException if the file type is not supported
     */
    public void run(Path in, Map<String, LOCDetail> locs, Map<String, String> inherit) throws IOException {
        if (!Files.exists(in)) {
            LOGGER.error("File not found: {}", in);
            return;
        }

        if (in.toString().endsWith(".class")) {
            runClass(in, locs, inherit);
            return;
        }

        if (in.toString().endsWith(".jar")) {
            runJar(in, locs, inherit);
            return;
        }

        throw new UnsupportedOperationException("Unsupported file type: " + in);
    }

    /**
     * Populate linesOfCode based on unknown methods and locs.
     * @param unknownMethods Set of unknown method names
     * @param locs Map of method names to the LOCDetail for that methd
     * @param linesOfCode Missed methods with total of lines of code
     */
    public void getLOCs(Set<String> unknownMethods, Map<String, LOCDetail> locs, Map<String, Long> linesOfCode) {
        if (unknownMethods == null) return;

        for (String unknownMethod : unknownMethods) {
            LOCDetail locSet = locs.get(unknownMethod);

            if (locSet != null) {
                linesOfCode.put(unknownMethod, locSet.linesTotal());
            }
        }
    }
}