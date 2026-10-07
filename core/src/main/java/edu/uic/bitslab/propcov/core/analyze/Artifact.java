package edu.uic.bitslab.propcov.core.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.analyze.CoverageData.CoverageDataType;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.util.Serialize;
import edu.uic.bitslab.propcov.core.util.timer.RunTimer;
import edu.uic.bitslab.propcov.core.util.timer.TimerException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents an artifact produced during the build or execution process of a project.
 * This class provides methods for creating, loading, and managing artifacts,
 * as well as organizing their associated files such as reports, test data, and runtime data.
 */
public class Artifact {
    private static final Logger LOGGER = Util.getLogger(Artifact.class);

    private final Path targetPathTest;
    private final Path targetPathRuntime;

    /**
     * Represents the file system path where report files related to the artifact are stored.
     * This is typically used to manage or access report files generated during the artifact's lifecycle.
     */
    public final Path targetPathReport;
    /**
     * Represents the subdirectory where the artifact and its related files
     * are stored or organized. This variable is typically used to specify
     * a unique subfolder name within the file system for storing files
     * associated with an artifact.
     */
    public final String artifactSubDir;
    /**
     * Represents the full file system path to the artifact, combining the base path and
     * the artifact-specific subdirectory.
     */
    public final Path fullArtifactPath;

    private static final DateTimeFormatter fileFriendlyDateTime = DateTimeFormatter.ofPattern("yyyyMMdd'_'HHmmss");
    private Path subFolder;
    private final MetaData metaData = new MetaData();

    /**
     * Stores coverage data mapped by property name and respective coverage data types.
     * The outer map's key represents the name of the property for which the coverage data
     * is associated, and the inner map holds the specific coverage data categorized by
     * {@link CoverageDataType}.
     */
    public final Map<String, Map<CoverageDataType, CoverageData>> coverage = new HashMap<>();

    /**
     * Represents the type of artifact paths within the system. The enum is used
     * to differentiate specific categories of artifacts and their corresponding
     * paths used in various operations, such as runtime artifacts, test-related
     * artifacts, and report-related artifacts.
     */
    public enum Type {
        /**
         * Represents the type associated with runtime artifacts within the system.
         */
        RUNTIME,
        /**
         * Represents the type associated with test-related artifacts within the system.
         */
        TESTS,
        /**
         * Represents the type associated with report-related artifacts within the system.
         */
        REPORTS
    }

    /**
     * Creates a new artifact instance with the specified configuration.
     *
     * @param config the configuration object containing necessary settings and parameters
     *               for artifact creation
     * @return a newly instantiated Artifact object
     * @throws IOException if an I/O error occurs during artifact creation
     * @throws TimerException if a timing-related error occurs during artifact creation
     */
    public static Artifact CreateArtifact(Config config) throws IOException, TimerException {
        return new Artifact(config, LocalDateTime.now().format(fileFriendlyDateTime));
    }

    /**
     * Loads the latest artifact based on the configuration provided.
     * This method identifies the most recent artifact subdirectory, derives its path,
     * and creates an instance of the 'Artifact' class for it.
     *
     * @param config the configuration object specifying paths and settings.
     * @return an Artifact object representing the latest artifact's state and data.
     * @throws IOException if there is an error in reading or accessing the file system.
     * @throws TimerException if a timing-related error occurs during the process.
     */
    public static Artifact LoadLastestArtifact(Config config) throws IOException, TimerException {
        return new Artifact(config, getLatestSubDir(getPathStart(config)));
    }

    /**
     * Loads an existing artifact from the specified subdirectory.
     *
     * @param config the configuration object containing necessary settings for loading the artifact
     * @param givenArtifactSubDir the subdirectory of the artifact to be loaded
     * @return an Artifact object initialized from the specified subdirectory
     * @throws IOException if an I/O error occurs while accessing the artifact
     * @throws TimerException if an error related to the timer functionality occurs
     */
    public static Artifact LoadArtifact(Config config, String givenArtifactSubDir) throws IOException, TimerException {
        return new Artifact(config, givenArtifactSubDir);
    }

    /**
     * Loads an artifact from the specified file path.
     *
     * @param config The configuration object used to initialize the artifact.
     * @param path The file path from which the artifact should be loaded.
     * @return The loaded {@code Artifact} instance.
     * @throws IOException If an I/O error occurs while accessing the specified path.
     * @throws TimerException If a timing-related error occurs during artifact processing.
     */
    public static Artifact LoadArtifactFromPath(Config config, Path path) throws IOException, TimerException {
        return new Artifact(config, path.toString());
    }

    /**
     * Saves the provided configuration by converting it to a YAML format and storing it
     * as a file named "config.yaml".
     *
     * @param config the configuration object to be serialized and saved
     * @throws IOException if an I/O error occurs during the saving process
     */
    public void saveConfig(Config config) throws IOException {
        addReport(config.toYaml().getBytes(StandardCharsets.UTF_8), "config.yaml");
    }

    private static String getLatestSubDir(Path pathStart) throws IOException {
        try (Stream<Path> listing = Files.list(pathStart)) {
            Optional<Path> latest = listing.map(Path::getFileName).max(Comparator.naturalOrder());

            if (latest.isEmpty()) {
                throw new RuntimeException("Unable to locate LATEST run for this project.");
            }

            return latest.get().toString();
        }
    }

    private static Path getPathStart(Config config) {
        return Path.of(
                config.artifactDirectory,
                "executions",
                config.project,
                config.subProject == null ? "" : config.subProject
        );
    }

    private Artifact(Config config, String artifactSubDir) throws IOException, TimerException {
        this.artifactSubDir = artifactSubDir;

        // set target path vars
        fullArtifactPath = getPathStart(config).resolve(artifactSubDir);
        targetPathTest = fullArtifactPath.resolve(Type.TESTS.name());
        targetPathRuntime = fullArtifactPath.resolve(Type.RUNTIME.name());
        targetPathReport = fullArtifactPath.resolve(Type.REPORTS.name());

        saveConfig(config);
    }

    /**
     * Initializes metadata for the specified property. If the property does not
     * already exist in the metadata, it is added with a default {@link MetaData.PropertyTest} instance.
     *
     * @param propertyName the name of the property for which metadata is being initialized
     */
    public void initMetaData(String propertyName) {
        metaData.propertyTest.putIfAbsent(propertyName, new MetaData.PropertyTest());
    }

    /**
     * Marks a property as completed by calculating and storing its total elapsed time.
     *
     * @param propertyName the name of the property whose elapsed time is to be calculated and stored
     */
    public void propertyDone(String propertyName) {
        MetaData.PropertyTest propertyTest = metaData.propertyTest.get(propertyName);
        propertyTest.propertyElapsedTimeInSeconds = propertyTest.propertyTimes.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private static String throwableToString(Throwable e) {
        // add more detail to error
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        Throwable cause = (e.getCause() == null) ? e : e.getCause();
        String msg = (e.getMessage() == null) ? "" : e.getMessage();
        String causeMsg = (cause.getMessage() == null) ? "" : cause.getMessage();

        return msg + ": "
                + e.getClass().getSimpleName() + ": "
                + causeMsg
                + "\n" + sw;
    }

    /**
     * Adds an error to the metadata by converting the given {@code Throwable}
     * object to a string representation and logging it.
     *
     * @param e the {@code Throwable} object representing the error to be added
     */
    public void addError(Throwable e) {
        this.metaData.addError(throwableToString(e));
    }

    /**
     * Adds an error message to the metadata for a specific property. The error
     * message is generated from the provided {@code Throwable} object and appended
     * to any existing error messages for the given property.
     *
     * @param property the name of the property for which the error is being logged
     * @param e the {@code Throwable} object representing the error to be added
     */
    public void addPropertyError(String property, Throwable e) {
        String currentError = this.metaData.propertyTest.get(property).errorMsg;
        String newError = throwableToString(e);
        String combined = currentError.isEmpty() ? newError : currentError + "\n==========\n" + newError;

        this.metaData.propertyTest.get(property).setError(true, combined);
    }

    /**
     * Adds the elapsed time for a specific property and timer instance to the metadata.
     * If the property already contains elapsed time data for the timer label, the new elapsed time
     * value is added to the existing value.
     *
     * @param property the name of the property for which the elapsed time is being recorded
     * @param timer a {@code RunTimer} instance containing the label and elapsed time to be added
     */
    public void addPropertyElapsedTime(String property, RunTimer timer) {
        this.metaData.propertyTest.get(property).propertyTimes.merge(timer.label, timer.elapsedInSeconds(), (k, v) -> v + timer.elapsedInSeconds());
    }

    /**
     * Adds the elapsed time for a specific property and label to the metadata.
     * If the property already contains elapsed time data for the label, the new
     * elapsed time value is added to the existing value.
     *
     * @param property the name of the property for which the elapsed time is being recorded
     * @param label the label associated with the elapsed time
     * @param elapsedTimeInSeconds the elapsed time in seconds to be added
     */
    public void addPropertyElapsedTime(String property, String label, double elapsedTimeInSeconds) {
        this.metaData.propertyTest.get(property).propertyTimes.merge(label, elapsedTimeInSeconds, (k, v) -> v + elapsedTimeInSeconds);
    }

    /**
     * Updates the trial metadata for a given property by setting the override trials,
     * expected trials, and actual trials run.
     *
     * @param property the name of the property for which the trials data is being updated
     * @param overrideTrials the custom number of trials to override the default value
     * @param trialsExpected the total number of trials expected for the property
     * @param trialsRan the number of trials that have been executed for the property
     */
    public void addPropertyTrials(String property, long overrideTrials, long trialsExpected, long trialsRan) {
        MetaData.PropertyTest p = this.metaData.propertyTest.get(property);
        p.overrideTrials = overrideTrials;
        p.trialsExpected = trialsExpected;
        p.trialsRan = trialsRan;
    }

    /**
     * Sets the coverage data for a specific entry point and coverage data type.
     * If the entry point already contains data for the given coverage data type,
     * an exception is thrown to ensure only one entry is allowed per type.
     *
     * @param propertyTest The test property containing the entry point information.
     * @param coverageDataType The type of coverage data being set.
     * @param coverageData The coverage data to be associated with the provided entry point and type.
     * @throws RuntimeException if an entry for the specified coverage data type already exists for the entry point.
     */
    public void setCoverageData(PropertyTest propertyTest, CoverageDataType coverageDataType, CoverageData coverageData) {
        String entryPoint = propertyTest.entryPoint.replace('/', '.');
        coverage.putIfAbsent(entryPoint, new HashMap<>());
        if (coverage.get(entryPoint).containsKey(coverageDataType)) {
            throw new RuntimeException("Entrypoint " + entryPoint + " should only have one entry for " + coverageDataType);
        }
        coverage.get(entryPoint).put(coverageDataType, coverageData);
    }

    /**
     * Adds the elapsed time from the given RunTimer instance to the current context.
     *
     * @param timer the RunTimer instance containing the label and elapsed time in seconds
     */
    public void addElapsedTime(RunTimer timer) {
        addElapsedTime(timer.label, timer.elapsedInSeconds());
    }

    /**
     * Adds the elapsed time associated with a specific label to the internal metadata tracking.
     * If the label already exists, the given elapsed time is added to the existing value.
     *
     * @param label the identifier for the time entry
     * @param elapsedTimeInSeconds the elapsed time in seconds to add for the specified label
     */
    public void addElapsedTime(String label, double elapsedTimeInSeconds) {
        this.metaData.timesCommon.merge(label, elapsedTimeInSeconds, (k, v) ->  v + elapsedTimeInSeconds);
    }

    /**
     * Adds runtime paths to the specified target directory for execution purposes.
     *
     * @param paths an array of Path objects representing the runtime directories or files to be added
     * @throws IOException if an I/O error occurs while copying the paths to the target runtime directory
     */
    public void addRuntimes(Path[] paths) throws IOException {
        _add(paths, targetPathRuntime);
    }

    /**
     * Retrieves an array of paths that represent directories within the target test path.
     *
     * @return an array of {@code Path} objects, each pointing to a directory within the target test path.
     * @throws IOException if an I/O error occurs while accessing the file system.
     */
    public Path[] getTestPaths() throws IOException {
        try (Stream<Path> s = Files.list(targetPathTest)) {
            return s.filter( (p) -> p.toFile().isDirectory() ).toArray(Path[]::new);
        }
    }

    /**
     * Adds test files or directories to the designated test target path.
     * The method uses the specified array of paths to copy their
     * contents into the test subfolder directory resolved within the target path.
     *
     * @param paths an array of {@code Path} objects representing the files or
     *              directories to be added as tests
     * @throws IOException if an I/O error occurs during the copy process, or
     *                     if the sub-folder is not set
     */
    public void addTests(Path[] paths) throws IOException {
        if (this.subFolder == null) throw new IOException("Sub-folder is required for tests.  Call setSubFolder before addTests to set the property test directory name.");
        _add(paths, targetPathTest.resolve(subFolder));
    }

    /**
     * Adds a test file to the designated test target path.
     * The method writes the provided byte data to a specified filename
     * within the resolved test subfolder path. If the target directories
     * do not already exist, they will be created.
     *
     * @param sourceBytes the byte array containing the content to be written to the test file
     * @param targetFilename the name of the file to be created under the test target path
     * @return the {@code Path} to the newly created test file
     * @throws IOException if an I/O error occurs during the process of file writing or directory creation
     */
    public Path addTest(byte[] sourceBytes, String targetFilename) throws IOException {
        return _add(sourceBytes, targetFilename, targetPathTest.resolve(subFolder));
    }

    /**
     * Adds a report to the designated target path by writing the provided source bytes
     * to the specified target filename.
     *
     * @param sourceBytes the byte array representing the content of the report to be added
     * @param targetFilename the name of the file to which the report content will be written
     * @throws IOException if an I/O error occurs while writing the report to the target path
     */
    public void addReport(byte[] sourceBytes, String targetFilename) throws IOException {
        _add(sourceBytes, targetFilename, targetPathReport);
    }

    /**
     * Sets the subfolder path for the artifact instance.
     *
     * @param subFolder the path to the subfolder that needs to be assigned
     */
    public void setSubFolder(Path subFolder) {
        this.subFolder = subFolder;
    }

    /**
     * Resets the subfolder reference by setting it to null.
     * This method is typically used to clear or unassign any currently associated subfolder.
     */
    public void unSetSubFolder() {
        subFolder = null;
    }

    private void _add(Path[] sourcePaths, Path targetPath) throws IOException {
        if (sourcePaths == null) return;
        if (targetPath == null) throw new IOException("targetPath must be provided.");

        for (Path sourcePath : sourcePaths) {
            Util.recursiveCopy(sourcePath, targetPath);
        }
    }

    private Path _add(byte[] sourceBytes, String targetFilename, Path targetPath) throws IOException {
        Files.createDirectories(targetPath);
        return Files.write(targetPath.resolve(targetFilename), sourceBytes); // Creates, Truncates, and Writes by default
    }

    /**
     * Sets the label for the associated metadata.
     *
     * @param label the label to be set for the metadata
     */
    public void setLabel(String label) {
        metaData.label = label;
    }

    /**
     * Prepares and adds coverage report data in two formats: serialized and JSON.
     * This method serializes the coverage object and adds it as a report,
     * and also converts the coverage object to a JSON string with indented formatting,
     * which is then added as another report.
     *
     * @throws IOException if an I/O error occurs during serialization or report addition.
     */
    public void beforeReports() throws IOException {
        addReport(Serialize.Write(coverage), "coverage_details.ser");

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(coverage);
        addReport(json.getBytes(), "coverage_details.json");
    }

    static class CoverageTotalCollector {
        final Map<CoverageDetail.SourceFileID, HashSet<Long>> mapOfLinesCovered = new HashMap<>();
        final Map<CoverageDetail.SourceFileID, HashSet<Long>> mapOfLinesMissed = new HashMap<>();
    }

    private void createCoverageTotal() {
        metaData.coverage.clear();

        // total line sets for missed and covered
        Map<CoverageDataType, CoverageTotalCollector> collectors = new HashMap<>();
        for (Map<CoverageDataType, CoverageData> value : coverage.values()) {
            value.forEach((coverageDataType, coverageData) -> {
                CoverageTotalCollector collector = collectors.getOrDefault(coverageDataType, new CoverageTotalCollector());

                for (CoverageDetail coverageDetail : coverageData.details.values()) {
                    coverageDetail.mapOfLinesCovered.forEach((sourceFileID, longs) -> {
                        HashSet<Long> toSet = collector.mapOfLinesCovered.getOrDefault(sourceFileID, new HashSet<>());
                        toSet.addAll(longs);
                        collector.mapOfLinesCovered.put(sourceFileID, toSet);
                    });

                    coverageDetail.mapOfLinesMissed.forEach((sourceFileID, longs) -> {
                        HashSet<Long> toSet = collector.mapOfLinesMissed.getOrDefault(sourceFileID, new HashSet<>());
                        toSet.addAll(longs);
                        collector.mapOfLinesMissed.put(sourceFileID, toSet);
                    });
                }

                collectors.put(coverageDataType, collector);
            });
        }

        // build to coverage detail map
        for (CoverageDataType coverageDataType : CoverageDataType.values()) {
            CoverageDetail.Builder builder = new CoverageDetail.Builder();
            CoverageTotalCollector collector = collectors.get(coverageDataType);

            int totalLinesMissed = collector.mapOfLinesMissed.values().stream().map(HashSet::size).reduce(0, Integer::sum);
            int totalLinesCovered = collector.mapOfLinesCovered.values().stream().map(HashSet::size).reduce(0, Integer::sum);

            collector.mapOfLinesMissed.forEach(builder::addMissedLineNumbers);
            collector.mapOfLinesCovered.forEach(builder::addCoveredLineNumbers);

            metaData.coverage.put(
                coverageDataType,
                builder
                    .addLinesMissed(totalLinesMissed)
                    .addLinesCovered(totalLinesCovered)
                    .build()
            );
        }
    }

    /**
     * Finalizes the processing of metadata, calculates coverage totals, and generates a
     * metadata report in JSON format. This method logs the elapsed processing time, creates
     * coverage totals, serializes metadata to JSON, and adds the JSON report.
     *
     * @throws IOException if there is an input/output error during JSON serialization or reports
     * addition.
     * @throws TimerException if there is an error calculating the elapsed time using the
     * metadata timer.
     */
    public void complete() throws IOException, TimerException {
        double elapsedTimeInSeconds = metaData.done();
        LOGGER.info("Artifact elapsed time: {}", elapsedTimeInSeconds);

        createCoverageTotal();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(metaData);

        addReport(json.getBytes(), "metadata.json");
    }
}