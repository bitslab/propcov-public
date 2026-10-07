package edu.uic.bitslab.propcov.extensions.coverage;

import edu.uic.bitslab.propcov.core.Util;
import edu.uic.bitslab.propcov.core.config.Config;
import edu.uic.bitslab.propcov.core.config.PropertyTest;
import edu.uic.bitslab.propcov.core.config.YAMLConfig;
import edu.uic.bitslab.propcov.core.coverage.AbstractCoverage;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail;
import edu.uic.bitslab.propcov.core.coverage.CoverageDetail.SourceFileID;
import edu.uic.bitslab.propcov.core.coverage.CoverageException;
import edu.uic.bitslab.propcov.core.report.LOCTracker;
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.OpalUtil;
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.ParsedCallgraph;
import edu.uic.bitslab.propcov.extensions.util.LoadXML;
import jakarta.xml.bind.JAXBException;
import jaxb.generated.jacoco.Report;
import jaxb.generated.jacoco.Sourcefile;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Implements JaCoCo Coverage
 */
public class JaCoCo extends AbstractCoverage {
    private static final String METHOD_TYPE = "METHOD";
    private static final String LINE_TYPE = "LINE";
    private static final String BRANCH_TYPE = "BRANCH";
    private static final Logger LOGGER = Util.getLogger(JaCoCo.class);

    private Map<String, CoverageDetail> coverageDataJaCoCo;
    private Map<String, CoverageDetail> coverageDataPropCov;
    private Set<String> missingNodesPropCov;

    /**
     * Initializes a new instance of the JaCoCo coverage tool, using the provided
     * extension configuration to locate and define specific properties.
     *
     * @param extension The extension configuration object containing properties
     *                  and environment settings required for this JaCoCo instance.
     */
    public JaCoCo(YAMLConfig.Extension extension) {
        super(extension, "PropCov.Coverage.JaCoCo.");
    }

    private Path getXMLCoverageReport(Config config, PropertyTest propertyTest) {
        Path pathXML = Path.of(extension.properties.get("XMLCoverageReport"));
        if (pathXML.isAbsolute()) return pathXML;
        if (propertyTest.subProject == null) return config.buildSystem.getFullTargetPath(propertyTest).resolve(pathXML);
        return config.buildSystem.getFullTargetPath(new PropertyTest(propertyTest.name, propertyTest.entryPoint, "propcovrun")).resolve(pathXML);
    }

    private String buildQualifiedName(jaxb.generated.jacoco.Class clazz, jaxb.generated.jacoco.Method method) {
        // qualified name for the key
        return OpalUtil.methodToJavaJVM(
                clazz.getName().replaceAll("/", "."),
                method.getName(),
                method.getDesc()
        );
    }

    /**
     * Build JaCoCo and PropCov
     * @param g Previously generated call graph
     * @param config Complete Project Config
     * @param xmlCoverageReport Coverage report path
     * @throws JAXBException JaCoCo XML Parsing Error
     * @throws IOException JaCoCo XML Unable to read
     * @throws ParserConfigurationException JaCoCo XML configuration is invalid
     * @throws SAXException JaCoCo XML Building error
     */
    public void process(Graph<String, DefaultEdge> g, Config config, Path xmlCoverageReport) throws IOException, SAXException, JAXBException, ParserConfigurationException {
        Map<String, String> inherit = new HashMap<>();
        LOCTracker locTracker = new LOCTracker(config.coverage);

        Map<String, LOCTracker.LOCDetail> locsFromSource = new HashMap<>();
        for (Path jar : config.mainJars) {
            locTracker.run(jar, locsFromSource, inherit);
        }

        Map<String, LOCTracker.LOCDetail> testsFromSource = new HashMap<>();
        for (Path jar : config.testJars) {
            locTracker.run(jar, testsFromSource, inherit);
        }

        processJaCoCo(locsFromSource, xmlCoverageReport);
        processPropCov(g, locsFromSource, testsFromSource, inherit);
    }

    /**
     * Builds a map of lines of code (LOC) details for the methods in the provided class byte array.
     * This method analyzes the provided class bytecode to extract coverage data, including covered
     * and missed line numbers for each method. It also updates inheritance relationships among classes.
     *
     * @param clazzBytes The byte array representation of the class file to be analyzed.
     * @param locs A map where the keys are method signatures (class name, method name, and description)
     *             and the values are LOC details, including line coverage information.
     * @param inherit A map that holds class inheritance relationships, mapping class names to their
     *                respective superclasses.
     */
    public void buildLOC(byte[] clazzBytes, Map<String, LOCTracker.LOCDetail> locs, Map<String, String> inherit) {

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final ExecutionDataStore executionDataStore = new ExecutionDataStore();
        final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
        try {
            analyzer.analyzeClass(clazzBytes, "foo/bar.class");

            for (IClassCoverage c : coverageBuilder.getClasses()) {
                String className = c.getName().replace('/', '.');
                String superName = c.getSuperName();

                if (superName != null) inherit.put(className, superName.replace('/', '.'));

                for (IMethodCoverage mc : c.getMethods()) {
                    String methodName = mc.getName();
                    String methodDescription = mc.getDesc();
                    String signature = String.format("%s.%s%s", className, methodName, methodDescription);

                    if (locs.containsKey(signature)) {
                        continue;
                    }

                    // line numbers
                    LOCTracker.LOCDetail.Builder locDetail = new LOCTracker.LOCDetail.Builder();
                    for (int l = mc.getFirstLine(); l <= mc.getLastLine(); l++) {
                        ILine line = mc.getLine(l);

                        switch (line.getStatus()) {
                            case ICounter.NOT_COVERED, ICounter.PARTLY_COVERED -> locDetail.addMissed(l);
                            case ICounter.FULLY_COVERED -> locDetail.addCovered(l);
                            case ICounter.EMPTY -> {}
                            default -> throw new AssertionError("Invalid status " + line.getStatus() + " on line " + line);
                        }
                    }

                    // add line numbers for this method
                    locs.put(signature, locDetail.build());
                }
            }



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkSum(Map<String, LOCTracker.LOCDetail> locsFromSource) throws InputMismatchException {
        // look up all coverageDataJaCoCo keys using locsFromSource (provided by ASM)
        long sumLinesTotalFromASM = coverageDataJaCoCo.keySet().stream()
                .map(locsFromSource::get)
                .mapToLong(LOCTracker.LOCDetail::linesTotal)
                .sum();

        // get the totals from the JaCoCo report
        long sumLinesTotalFromJaCoCo = coverageDataJaCoCo.values().stream().mapToLong( v -> v.linesTotal ).sum();

        // if equal, then both JaCoCo and ASM found the same total for JaCoCo known codes
        if (sumLinesTotalFromASM == sumLinesTotalFromJaCoCo) return;

        throw new InputMismatchException("The line totals from JaCoCo report ("+sumLinesTotalFromJaCoCo+") and line count ("+sumLinesTotalFromASM+") must be equal.");
    }

    private void processPropCov(Graph<String, DefaultEdge> g, Map<String, LOCTracker.LOCDetail> locsFromSource, Map<String, LOCTracker.LOCDetail> testsFromSource, Map<String, String> inherit) {
        if (this.coverageDataJaCoCo == null || this.coverageDataJaCoCo.isEmpty()) {
            throw new IllegalStateException("No JaCoCo coverage data found");
        }

        if (g.vertexSet().isEmpty()) {
            throw new IllegalStateException("No coverage graph found");
        }

        // *** SANITY Check ***
        try {
            checkSum(locsFromSource);
        } catch (InputMismatchException e) {
            LOGGER.warn(e.getMessage());
        }

        Map<String, CoverageDetail> pCD = new HashMap<>();

        // build coverage for each node based on what appears in the coverage graph
        for (String s : g.vertexSet()) {
            if (s.startsWith("ClassNotFound(")) {
                LOGGER.warn("Ignoring unknown class {}", s);
                continue;
            }

            Matcher m = Util.labelToParts.matcher(s);
            if (!m.matches()) throw new IllegalStateException("Invalid label: " + s);
            String clazz = Util.fullClass(m);
            String pkg = clazz.lastIndexOf(".") == -1 ? null : clazz.substring(0, clazz.lastIndexOf("."));
            String file = clazz.lastIndexOf(".") == -1 ? null : clazz.substring(clazz.lastIndexOf(".") + 1);
            SourceFileID sourceFileID = SourceFileID.get(pkg, file);

            // is "test" method, so skip it
            if (testsFromSource.containsKey(s)) continue;
            
            // marked as not found in "ParsedCallgraph"
            if (s.contains(ParsedCallgraph.PARAM_NOT_FOUND())) continue;

            // try to get locs from source
            LOCTracker.LOCDetail fromSource = null;
            String tryMethod = s;
            int maxTries = 100;
            for (int i = 0 ;;) {
                if (locsFromSource.containsKey(tryMethod)) {
                    fromSource = locsFromSource.get(tryMethod);
                    break;
                }

                Matcher tryMatcher = Util.labelToParts.matcher(tryMethod);
                if (!tryMatcher.matches()) break; // didn't match, so can't resolve

                String fullClass = Util.fullClass(tryMatcher);
                String tryClass = inherit.get(fullClass);
                if (tryClass == null) break; // no more to try

                String prevTryMethod = tryMethod;
                tryMethod = tryMethod.replaceFirst(fullClass, tryClass);
                if (prevTryMethod.equals(tryMethod)) break; // same method as we already tried, so done

                if (++i >= maxTries) {
                    LOGGER.warn("Resolving classes results in more than 100 attempts for {}.", s);
                    break;
                }
            }

            // does the source have it?
            if (fromSource == null) {
                // not found
                LOGGER.warn("No loc data found for {}", s);

                CoverageDetail cD = this.coverageDataJaCoCo.getOrDefault(
                    s,
                    new CoverageDetail.Builder()
                        .addOtherUnknownMethod(true)
                        .build());
                pCD.put(s, cD.clone());
                continue;
            }

            CoverageDetail cD = this.coverageDataJaCoCo.getOrDefault(
                    // get: from JaCoCo
                    s,

                    // default: not in JaCoCo, so get from source
                    new CoverageDetail.Builder()
                            .addOtherUnknownMethod(true)
                            .addMissedLineNumbers(sourceFileID, new HashSet<>(fromSource.linesMissed))
                            .addLinesMissed(fromSource.linesMissed.size())
                            .addMethodsMissed(1)
                            .build()
            );

            pCD.put(s, cD.clone());
        }

        // get a list of elements in jacoco with coverage that are not in the propcov coverage graph
        this.missingNodesPropCov = coverageDataJaCoCo.entrySet().stream()
            .filter( s -> s.getValue().linesCovered > 0 )
            .map(Map.Entry::getKey)
            .filter( s -> !g.vertexSet().contains(s) )
            .collect(Collectors.toUnmodifiableSet());

        this.coverageDataPropCov = Collections.unmodifiableMap(pCD);
    }

    private List<jaxb.generated.jacoco.Package> collapseGroups(Report report) {
        List<jaxb.generated.jacoco.Package> pkgs = new ArrayList<>();
        collapseGroups(report.getGroupOrPackage(), pkgs);
        return pkgs;
    }

    private void collapseGroups(Object obj, List<jaxb.generated.jacoco.Package> pkgs) {
        if (obj instanceof jaxb.generated.jacoco.Package) {
            pkgs.add((jaxb.generated.jacoco.Package) obj);
            return;
        }

        if (obj instanceof jaxb.generated.jacoco.Group) {
            collapseGroups(((jaxb.generated.jacoco.Group) obj).getGroupOrPackage(), pkgs);
            return;
        }

        if (obj instanceof List<?>) {
            ((List<?>) obj).forEach(o -> collapseGroups(o, pkgs));
            return;
        }

        throw new IllegalStateException("Unexpected type: " + obj.getClass().getName());
    }

    private List<jaxb.generated.jacoco.Class> getClassElements(jaxb.generated.jacoco.Package pkg) {
        return pkg.getClazzOrSourcefile().stream()
                .filter(s -> s instanceof jaxb.generated.jacoco.Class)
                .map(s -> (jaxb.generated.jacoco.Class) s)
                .collect(Collectors.toList());
    }

    private List<Sourcefile> getSourceFileElements(jaxb.generated.jacoco.Package pkg) {
        return pkg.getClazzOrSourcefile().stream()
                .filter(s -> s instanceof jaxb.generated.jacoco.Sourcefile)
                .map(s -> (Sourcefile) s)
                .collect(Collectors.toList());
    }

    private void processJaCoCo(Map<String, LOCTracker.LOCDetail> locsFromSource, Path xmlCoverageReport)
            throws IOException, SAXException, JAXBException, ParserConfigurationException {

        Map<String, CoverageDetail> jCD = new HashMap<>();

        /* Convert the jacoco.xml file into a Report object */
        Report report = new LoadXML<>(Report.class).load(xmlCoverageReport);

        /* we do not care about groups because all should have different package
         * names. So, flatten group (if present to a list of packages)
         **/
        List<jaxb.generated.jacoco.Package> pkgs = collapseGroups(report);

        /* Iterate over all packages in the report */
        for (jaxb.generated.jacoco.Package pkg : pkgs) {

            /* Iterate over all classes in a package */
            getClassElements(pkg).forEach( clazz -> {
                // source filename lines
                jaxb.generated.jacoco.Sourcefile sourceFile = getSourceFileElements(pkg).stream().filter(s -> s.getName().equals(clazz.getSourcefilename())).findFirst().orElseThrow();
                List<jaxb.generated.jacoco.Line> sourceFileLines = sourceFile.getLine();

                /* Collect method info */
                List<jaxb.generated.jacoco.Method> methods = clazz.getMethod().stream()
                        .sorted(Comparator.comparing(jaxb.generated.jacoco.Method::getLine))
                        .collect(Collectors.toList());

                for (jaxb.generated.jacoco.Method method : methods) {
                    String qualifiedName = buildQualifiedName(clazz, method);

                    long firstLineFromSource = locsFromSource.get(qualifiedName).firstLine();
                    if (Long.parseLong(method.getLine()) != firstLineFromSource) {
                        LOGGER.warn("Line {} differs from {}", method.getLine(), firstLineFromSource);
                    }
                }

                // build coverage data
                for (jaxb.generated.jacoco.Method method : methods) {
                    String qualifiedName = buildQualifiedName(clazz, method);

                    if (jCD.containsKey(qualifiedName)) {
                        throw new RuntimeException("Duplicate method found: " + qualifiedName);
                    }

                    // coverage datum
                    CoverageDetail.Builder coverageDatum = new CoverageDetail.Builder();

                    // set coverage counters
                    for (jaxb.generated.jacoco.Counter counter : method.getCounter()) {
                        switch (counter.getType()) {
                            case JaCoCo.METHOD_TYPE: {
                                coverageDatum.addMethodsMissed(Long.parseLong(counter.getMissed()));
                                coverageDatum.addMethodsCovered(Long.parseLong(counter.getCovered()));
                                break;
                            }

                            case JaCoCo.LINE_TYPE: {
                                coverageDatum.addLinesMissed(Long.parseLong(counter.getMissed()));
                                coverageDatum.addLinesCovered(Long.parseLong(counter.getCovered()));
                                break;
                            }

                            case JaCoCo.BRANCH_TYPE: {
                                coverageDatum.addBranchesMissed(Long.parseLong(counter.getMissed()));
                                coverageDatum.addBranchesCovered(Long.parseLong(counter.getCovered()));
                                break;
                            }

                            default:
                        }
                    }

                    // get missed / coverage line numbers
                    SourceFileID sourceFileID = SourceFileID.get(pkg.getName(), sourceFile.getName());
                    LOCTracker.LOCDetail locsForQualifiedName = locsFromSource.get(qualifiedName);

                    sourceFileLines.stream()
                        .filter(s ->  locsForQualifiedName.contains(Long.parseLong(s.getNr())))
                        .forEach(s -> {
                                if (Long.parseLong(s.getMi()) < 0L || Long.parseLong(s.getCi()) < 0L || (Long.parseLong(s.getMi()) == 0L && Long.parseLong(s.getCi()) == 0L)) {
                                    throw new RuntimeException("Unexpected value since line has <= 0 for missed and covered instructions. Expected >= 0 for each value and both can not be 0.");
                                }

                                if (Long.parseLong(s.getMi()) == 0 && Long.parseLong(s.getCi()) > 0) {
                                    // covered
                                    coverageDatum.addCoveredLineNumber(sourceFileID, Long.parseLong(s.getNr()));
                                } else {
                                    // missed
                                    coverageDatum.addMissedLineNumber(sourceFileID, Long.parseLong(s.getNr()));
                                }
                            });

                    jCD.put(qualifiedName, coverageDatum.build());
                }
            });
        }

        this.coverageDataJaCoCo = Collections.unmodifiableMap(jCD);
    }

    public Map<String, CoverageDetail> getOtherStatistics() {
        return coverageDataJaCoCo;
    }

    public Map<String, CoverageDetail> getPropCovStatistics() {
        return coverageDataPropCov;
    }

    public Set<String> getMissingNodesPropCov() {
        return missingNodesPropCov;
    }

    public CoverageDetail getOtherStatistics(String qualifiedName) {
        return coverageDataJaCoCo.get(qualifiedName);
    }

    public CoverageDetail getPropCovStatistics(String qualifiedName) {
        return coverageDataPropCov.get(qualifiedName);
    }

    @Override
    public void reset(Path target) throws IOException {
        for (Path coverageResultPath : getResultPaths(target)) {
            Util.recursiveRemove(coverageResultPath);
        }
    }

    @Override
    public Path[] getResultPaths(Path start) {
        return new Path[]{
                start.resolve("jacoco.exec"),
                start.resolve("site"),
                start.resolve("surefire-reports")
        };
    }

    public void apply(Graph<String, DefaultEdge> g, Config config, PropertyTest property) throws CoverageException {
        Path xmlCoverageReport = getXMLCoverageReport(config, property);

        // Load coverage
        LOGGER.info("Reading JaCoCo coverage file {}", xmlCoverageReport);

        try {
            process(g, config, xmlCoverageReport);
        } catch (IOException | ParserConfigurationException | JAXBException | SAXException e) {
            LOGGER.error("Could not read JaCoCo coverage file", e);
            throw new CoverageException("Could not read JaCoCo coverage file", e);
        }
    }
}