package edu.uic.bitslab.propcov.core.coverage;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * The CoverageDetail class represents detailed coverage information for codebase analysis.
 * It includes metrics such as the number of covered and missed lines, branches, and methods,
 * as well as maps for tracking specific covered and missed lines per source file.
 * This class is immutable and designed for serialization, cloning, and detailed code coverage analysis.
 */
public class CoverageDetail implements Cloneable, Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * Indicates whether coverage is achieved for a specific code segment or context.
     * This boolean flag is immutable and part of the overall coverage details tracked
     * in the CoverageDetail class.
     */
    public final boolean covered;
    /**
     * Represents the number of lines covered during code coverage analysis.
     * This value is typically used to track the subset of total lines
     * in the codebase that have been successfully executed during testing.
     */
    public final long linesCovered;
    /**
     * Represents the number of lines of code that were not covered during the coverage analysis.
     */
    public final long linesMissed;
    /**
     * Represents the total number of lines in the coverage detail.
     * This value typically includes both covered and missed lines.
     */
    public final long linesTotal;
    /**
     * Represents the number of branches covered during a code coverage analysis.
     * This field is used to store the count of branches in the code that are
     * successfully exercised during the testing process, as determined by the
     * coverage analysis tools.
     */
    public final long branchesCovered;
    /**
     * Represents the number of branches in the code that have been missed during coverage analysis.
     * This metric is often used to measure the thoroughness of testing, where a higher value indicates
     * that more branches in the code were not executed by the test suite.
     */
    public final long branchesMissed;
    /**
     * Represents the total number of branches in a code coverage analysis.
     * This variable is part of the detailed code coverage statistics and
     * provides the aggregate count of all branches, including covered and
     * missed branches, in the analyzed codebase.
     */
    public final long branchesTotal;
    /**
     * Represents the count of methods that have been covered during a coverage analysis process.
     */
    public final long methodsCovered;
    /**
     * Represents the total number of methods that were missed during the coverage analysis.
     * This variable holds a value indicating how many methods within the analyzed code
     * base were not executed or tested according to the coverage process.
     */
    public final long methodsMissed;
    /**
     * Represents the total number of methods associated with a particular coverage detail.
     * This value combines both covered and missed methods to give an overall count of methods
     * in the context of coverage analysis.
     */
    public final long methodsTotal;
    /**
     * Represents a mapping of source files to the lines of code that have been covered during execution.
     * This map uses {@link SourceFileID} as the key to uniquely identify source files,
     * while the value is a {@link HashSet} of {@link Long} objects, representing the line numbers
     * covered in the corresponding source file.
     */
    public final Map<SourceFileID, HashSet<Long>> mapOfLinesCovered;
    /**
     * Represents a mapping of source files to the sets of line numbers that were missed during coverage analysis.
     * Each entry in the map corresponds to a source file, identified by an instance of {@code SourceFileID},
     * and contains a set of line numbers that were not covered during execution.
     * This structure is used for tracking missing line coverage data on a per-file basis for detailed analysis.
     */
    public final Map<SourceFileID, HashSet<Long>> mapOfLinesMissed;
    /**
     * Indicates whether an unknown method is detected or flagged in the other coverage process.
     * This variable is used as a boolean flag to represent the presence or absence of an unidentified
     * or unsupported method during the analysis of code coverage using another coverage system.
     */
    public final boolean otherUnknownMethod;


    private CoverageDetail(Builder builder) {
        linesCovered = builder.linesCovered;
        linesMissed = builder.linesMissed;
        linesTotal = linesCovered + linesMissed;

        branchesCovered = builder.branchesCovered;
        branchesMissed = builder.branchesMissed;
        branchesTotal = branchesCovered + branchesMissed;

        methodsCovered = builder.methodsCovered;
        methodsMissed = builder.methodsMissed;
        methodsTotal = methodsCovered + methodsMissed;

        /*
         * @todo JaCoCo considered the class covered if it was loaded (even if no methods were executed). With PropCov,
         *   the lines are not added as covered because all methods are pruned if it is UR. So, PropCov MR for lines
         *   could be off bounded at # of classes in SUT. This makes PropCov look worse then JaCoCo.
         */

        mapOfLinesCovered = Collections.unmodifiableMap(builder.mapOfLinesCovered);
        mapOfLinesMissed = Collections.unmodifiableMap(builder.mapOfLinesMissed);

        otherUnknownMethod = builder.otherUnknownMethod;
        covered = (methodsCovered > 0 && methodsMissed == 0);
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream stream) throws Exception {
        stream.writeLong(linesCovered);
        stream.writeLong(linesMissed);
        stream.writeLong(linesTotal);
        stream.writeLong(branchesCovered);
        stream.writeLong(branchesMissed);
        stream.writeLong(branchesTotal);
        stream.writeLong(methodsCovered);
        stream.writeLong(methodsMissed);
        stream.writeLong(methodsTotal);
        stream.writeObject(mapOfLinesCovered);
        stream.writeObject(mapOfLinesMissed);
        stream.writeBoolean(otherUnknownMethod);
        stream.writeBoolean(covered);
    }

    private void setFinal(String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field id = this.getClass().getDeclaredField(name);
        id.setAccessible(true);
        id.set(this, value);
        id.setAccessible(false);
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
        setFinal("linesCovered", stream.readLong());
        setFinal("linesMissed", stream.readLong());
        setFinal("linesTotal", stream.readLong());
        setFinal("branchesCovered", stream.readLong());
        setFinal("branchesMissed", stream.readLong());
        setFinal("branchesTotal", stream.readLong());
        setFinal("methodsCovered", stream.readLong());
        setFinal("methodsMissed", stream.readLong());
        setFinal("methodsTotal", stream.readLong());
        setFinal("mapOfLinesCovered", stream.readObject());
        setFinal("mapOfLinesMissed", stream.readObject());
        setFinal("otherUnknownMethod", stream.readBoolean());
        setFinal("covered", stream.readBoolean());
    }


    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public CoverageDetail clone() {
        CoverageDetail.Builder builder = new CoverageDetail.Builder();
        builder.addTo(this);
        return builder.build();
    }

    /**
     * A builder class for constructing instances of {@link CoverageDetail}.
     * This class provides methods to incrementally add and accumulate
     * coverage metrics such as lines covered, lines missed, branches covered,
     * branches missed, methods covered, and methods missed.
     * Additionally, it maintains maps of covered and missed lines categorized
     * by source file identifiers.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        protected long linesCovered = 0;
        protected long linesMissed = 0;
        protected long branchesCovered = 0;
        protected long branchesMissed = 0;
        protected long methodsCovered = 0;
        protected long methodsMissed = 0;
        protected boolean otherUnknownMethod = false;
        protected final Map<SourceFileID, HashSet<Long>> mapOfLinesCovered = new HashMap<>();
        protected final Map<SourceFileID, HashSet<Long>> mapOfLinesMissed = new HashMap<>();

        /**
         * Constructs a new instance of the {@code Builder} class. This class is
         * used to aggregate and manage coverage details, providing methods to
         * update and build a {@code CoverageDetail} object.
         */
        public Builder() {
        }

        /**
         * Aggregates the coverage details from the specified {@code CoverageDetail} instance into the current builder.
         *
         * @param coverageDetail the {@code CoverageDetail} instance whose coverage data will be added to this builder
         * @return Builder instance for method chaining
         */
        public Builder addTo(final CoverageDetail coverageDetail) {
            addBranchesCovered(coverageDetail.branchesCovered);
            addBranchesMissed(coverageDetail.branchesMissed);
            addMethodsCovered(coverageDetail.methodsCovered);
            addMethodsMissed(coverageDetail.methodsMissed);
            addOtherUnknownMethod(coverageDetail.otherUnknownMethod);

            coverageDetail.mapOfLinesCovered.forEach(
                (sourceFileID, lineNumbers) ->
                    mapOfLinesCovered
                        .computeIfAbsent(sourceFileID, k -> new HashSet<>())
                        .addAll(lineNumbers)
            );
            this.linesCovered = mapOfLinesCovered.values().stream().mapToLong(Collection::size).sum();

            coverageDetail.mapOfLinesMissed.forEach(
                (sourceFileID, lineNumbers) ->
                    mapOfLinesMissed
                        .computeIfAbsent(sourceFileID, k -> new HashSet<>())
                        .addAll(lineNumbers)
            );
            this.linesMissed = mapOfLinesMissed.values().stream().mapToLong(Collection::size).sum();

            return this;
        }

        /**
         * Adds a set of missed line numbers to the internal mapping for the specified source file.
         * If the source file is not already present in the mapping, it is added and initialized.
         *
         * @param sourceFileID the unique identifier of the source file to which the missed line numbers should be added
         * @param lineNumbers the set of line numbers within the source file to mark as missed
         * @return the current instance of {@code Builder} to allow for method chaining
         */
        public Builder addMissedLineNumbers(final SourceFileID sourceFileID, final Set<Long> lineNumbers) {
            mapOfLinesMissed.computeIfAbsent(sourceFileID, k -> new HashSet<>()).addAll(lineNumbers);
            return this;
        }

        /**
         * Adds a missed line number to the internal mapping for the specified source file.
         * Updates the internal data structure to include the given line number as missed
         * for the provided {@code SourceFileID}. If the source file is not already present
         * in the mapping, it is added and initialized.
         *
         * @param sourceFileID the unique identifier of the source file to which the missed line number should be added
         * @param lineNumber the line number within the source file to mark as missed
         * @return the current instance of {@code Builder} to allow for method chaining
         */
        public Builder addMissedLineNumber(final SourceFileID sourceFileID, final long lineNumber) {
            mapOfLinesMissed.computeIfAbsent(sourceFileID, k -> new HashSet<>()).add(lineNumber);
            return this;
        }

        /**
         * Adds a set of covered line numbers to the internal mapping for the specified source file.
         * If the source file is not already present in the mapping, it is added.
         *
         * @param sourceFileID the unique identifier of the source file to which the line numbers should be added
         * @param lineNumbers  the set of line numbers within the source file to mark as covered
         * @return the current instance of {@code Builder} to allow for method chaining
         */
        public Builder addCoveredLineNumbers(final SourceFileID sourceFileID, final Set<Long> lineNumbers) {
            mapOfLinesCovered.computeIfAbsent(sourceFileID, k -> new HashSet<>()).addAll(lineNumbers);
            return this;
        }

        /**
         * Adds a covered line number for the specified source file.
         * Updates the internal mapping of covered lines to associate the given line number
         * with the specified {@code SourceFileID}.
         *
         * @param sourceFileID the unique identifier of the source file for which the line number is being added
         * @param lineNumber the line number within the source file to mark as covered
         * @return the current instance of {@code Builder} to allow for method chaining
         */
        public Builder addCoveredLineNumber(final SourceFileID sourceFileID, final long lineNumber) {
            mapOfLinesCovered.computeIfAbsent(sourceFileID, k -> new HashSet<>()).add(lineNumber);
            return this;
        }

        /**
         * Sets the other coverage method unknown method flag for the builder. This flag typically indicates whether
         * there are methods in the coverage data that are not accounted for or recognized.
         *
         * @param otherUnknownMethod a boolean indicating whether the other coverage unknown method flag
         *                            should be set to true or false
         * @return the current instance of {@code Builder} to allow for method chaining
         */
        public Builder addOtherUnknownMethod(final boolean otherUnknownMethod) {
            this.otherUnknownMethod = otherUnknownMethod;
            return this;
        }

        /**
         * Adds the specified number of lines that are covered to the current builder instance.
         *
         * @param linesCovered the number of covered lines to add; must be a non-negative value
         * @return the current Builder instance with the updated number of covered lines
         */
        public Builder addLinesCovered(long linesCovered) {
            this.linesCovered += linesCovered;
            return this;
        }

        /**
         * Adds the specified number of missed lines to the current count of missed lines in the builder instance.
         *
         * @param linesMissed the number of missed lines to add
         * @return the current instance of the Builder for method chaining
         */
        public Builder addLinesMissed(long linesMissed) {
            this.linesMissed += linesMissed;
            return this;
        }

        /**
         * Adds the specified number of covered branches to the current builder instance.
         *
         * @param branchesCovered the number of covered branches to add; must be a non-negative value
         * @return the current instance of {@code Builder} to allow for method chaining
         */
        public Builder addBranchesCovered(long branchesCovered) {
            this.branchesCovered += branchesCovered;
            return this;
        }

        /**
         * Adds the specified number of missed branches to the current count in the builder instance.
         *
         * @param branchesMissed the number of branches missed to add; must be a non-negative value
         * @return the current instance of the Builder to allow for method chaining
         */
        public Builder addBranchesMissed(long branchesMissed) {
            this.branchesMissed += branchesMissed;
            return this;
        }

        /**
         * Adds the specified number of covered methods to the current builder instance.
         *
         * @param methodsCovered the number of methods covered to add; must be a non-negative value
         * @return the current instance of {@code Builder} to allow for method chaining
         */
        public Builder addMethodsCovered(long methodsCovered) {
            this.methodsCovered += methodsCovered;
            return this;
        }

        /**
         * Adds the specified number of methods missed to the current count in the builder instance.
         *
         * @param methodsMissed the number of missed methods to add; must be a non-negative value
         * @return the current instance of the Builder to allow method chaining
         */
        public Builder addMethodsMissed(long methodsMissed) {
            this.methodsMissed += methodsMissed;
            return this;
        }

        /**
         * Constructs and returns a new {@code CoverageDetail} instance using the current state of the {@code Builder}.
         * The returned object encapsulates the coverage details that have been aggregated within this builder.
         *
         * @return a new instance of {@code CoverageDetail} containing the coverage data accumulated in the builder
         */
        public CoverageDetail build() {
            return new CoverageDetail(this);
        }
    }

    /**
     * Represents a unique identifier for source files, combining a package name and a source file name.
     * This class provides mechanisms to create, cache, and retrieve specific identifiers.
     */
    public static class SourceFileID implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private static final HashMap<String, SourceFileID> sourceFileIDs = new HashMap<>();

        /**
         * Represents the name of the package associated with a source file.
         */
        public final String packageName;
        /**
         * Represents the name of the source file associated with a specific instance
         * in the context of source file management and identification.
         */
        public final String sourceFileName;

        protected SourceFileID(String packageName, String sourceFileName) {
            this.packageName = packageName;
            this.sourceFileName = sourceFileName;
        }

        @Override
        public String toString() {
            return packageName.replace('.', '/') + "/" + sourceFileName;
        }

        /**
         * Retrieves or creates a unique {@code SourceFileID} instance for the given package name and source file name.
         * If an identifier corresponding to the provided package name and source file name already exists in the cache,
         * it will be returned. Otherwise, a new {@code SourceFileID} instance is created and stored in the cache.
         *
         * @param packageName the name of the package to associate with the source file, must not be null
         * @param sourceFileName the name of the source file to associate with the package, must not be null
         * @return the {@code SourceFileID} instance representing the unique identifier for the specified package and source file
         */
        public static SourceFileID get(String packageName, String sourceFileName) {
            String key = packageName + '*' + sourceFileName;
            return sourceFileIDs.computeIfAbsent(key, k -> new SourceFileID(packageName, sourceFileName));
        }

    }
}