//package edu.uic.bitslab.propcov.extensions.coverage;
//
//import edu.uic.bitslab.propcov.core.Util;
//import edu.uic.bitslab.propcov.extensions.util.LoadXML;
//import jakarta.xml.bind.JAXBElement;
//import jakarta.xml.bind.JAXBException;
//import jaxb.generated.jacoco.Counter;
//import jaxb.generated.jacoco.Report;
//import org.apache.commons.collections4.CollectionUtils;
//import org.slf4j.Logger;
//import org.xml.sax.SAXException;
//
//import javax.xml.parsers.ParserConfigurationException;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * Implements JaCoCo Coverage
// */
//public class JaCoCoCompare {
//    private static final String METHOD_TYPE = "METHOD";
//    private static final String LINE_TYPE = "LINE";
//    private static final String BRANCH_TYPE = "BRANCH";
//    private static final Logger LOGGER = Util.getLogger(JaCoCoCompare.class);
//
//    public static void main(String[] args) throws JAXBException, IOException, ParserConfigurationException, SAXException {
//        Path left = Path.of(args[0]);
//        Path right = Path.of(args[1]);
//        String project = args[2];
//
//        compare(left, right, project);
//    }
//
//    public static void compare(Path left, Path right, String project) throws JAXBException, IOException, ParserConfigurationException, SAXException {
//        try {
//            CoverageDetail cdLeft = getCoverage(left);
//            CoverageDetail cdRight = getCoverage(right);
//
//            //System.out.println("project,leftCovered,rightCovered,leftMissed,rightMissed,leftOnlyCovered,rightOnlyCovered,bothCovered,noneCovered");
//            System.out.printf("%s,%d,%d,%d,%d,%d,%d,%d,%d\n",
//                    project,
//                    CollectionUtils.union(cdLeft.coveredLinesSet, cdRight.coveredLinesSet).size(), // totalCovered
//                    CollectionUtils.union(cdLeft.missedLinesSet, cdRight.missedLinesSet).size(), // totalMissed
//                    cdLeft.coveredLinesSet.size(), //leftCovered
//                    cdRight.coveredLinesSet.size(), //rightCovered
//                    CollectionUtils.subtract(cdLeft.coveredLinesSet, cdRight.coveredLinesSet).size(), //leftOnlyCovered
//                    CollectionUtils.subtract(cdRight.coveredLinesSet, cdLeft.coveredLinesSet).size(), //rightOnlyCovered
//                    CollectionUtils.subtract(CollectionUtils.union(cdLeft.coveredLinesSet, cdRight.coveredLinesSet), CollectionUtils.union(cdLeft.missedLinesSet, cdRight.missedLinesSet)).size(), //bothCovered
//                    CollectionUtils.subtract(CollectionUtils.union(cdLeft.missedLinesSet, cdRight.missedLinesSet), CollectionUtils.union(cdLeft.coveredLinesSet, cdRight.coveredLinesSet)).size() //noneCovered
//            );
//        } catch (Exception e) {
//            System.out.printf("%s,NA,NA,NA,NA,NA,NA,NA,NA\n", project);
//        }
//    }
//
//    private static class CoverageDetail {
//        public long missedLines = 0;
//        public long coveredLines = 0;
//        public long totalLines = 0;
//        public Set<String> coveredLinesSet = new HashSet<>();
//        public Set<String> missedLinesSet = new HashSet<>();;
//    }
//
//    private static CoverageDetail getCoverage(Path xmlCoverageReport)
//            throws IOException, SAXException, JAXBException, ParserConfigurationException {
//
//        /* Convert the jacoco.xml file into a Report object */
//        Report report = new LoadXML<>(Report.class).load(xmlCoverageReport);
//
//        CoverageDetail coverageDetail = new CoverageDetail();
//
//        // set high-level coverage counters
//        for (Counter counter : report.getCounter()) {
//            switch (counter.getType()) {
//                case JaCoCoCompare.LINE_TYPE: {
//                    coverageDetail.missedLines += Long.parseLong(counter.getMissed());
//                    coverageDetail.coveredLines += Long.parseLong(counter.getCovered());
//                    coverageDetail.totalLines += Long.parseLong(counter.getCovered() + counter.getMissed());
//                    break;
//                }
//
//                default:
//            }
//        }
//
//        // line counters
//        /* Iterate over all packages in the report */
//        for (Package pkg : report.getPackage()) {
//
//            /* Iterate over all classes in a package */
//            pkg.getClazz().forEach( clazz -> {
//                // source filename lines
//                Report.Package.Sourcefile sourceFile = pkg.getSourcefile().stream().filter(s -> s.getName().equals(clazz.getSourcefilename())).findFirst().orElseThrow();
//                List<Report.Package.Sourcefile.Line> sourceFileLines = sourceFile.getContent().stream()
//                        .filter(s -> s instanceof JAXBElement)
//                        .map(s -> (JAXBElement<?>) s)
//                        .filter(s -> s.getValue() instanceof Report.Package.Sourcefile.Line)
//                        .map(s -> (Report.Package.Sourcefile.Line) s.getValue())
//                        .collect(Collectors.toList());
//
//                sourceFileLines.forEach(s -> {
//                    if  (s.getMi() < 0 || s.getCi() < 0 || (s.getMi() == 0 && s.getCi() == 0)) {
//                        throw new RuntimeException("Unexpected value since line has <= 0 for missed and covered instructions. Expected >= 0 for each value and both can not be 0.");
//                    }
//
//                    if (s.getMi() == 0 && s.getCi() > 0) {
//                        // covered
//                        coverageDetail.coveredLinesSet.add(clazz.getSourcefilename() + ":" + s.getNr());
//                    } else {
//                        // missed
//                        coverageDetail.missedLinesSet.add(clazz.getSourcefilename() + ":" + s.getNr());
//                    }
//                });
//            });
//        }
//
//        return coverageDetail;
//    }
//}