package edu.uic.bitslab.propcov.extensions.analysisframework.opal

import com.typesafe.config.Config
import edu.uic.bitslab.propcov.core.analyze.Artifact
import edu.uic.bitslab.propcov.core.config.AnalysisConfig
import edu.uic.bitslab.propcov.core.config.AnalysisConfig.AnalysisType
import edu.uic.bitslab.propcov.extensions.analysisframework.Opal.{OPALLoggerType, OpalAnalysisConfig}
import edu.uic.bitslab.propcov.core.util.NoGraphException
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.Optimizations.{CFAOptimization, IteratorOptimization, ThrowOptimization}
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.OpalUtil.methodToJavaJVM
import org.jgrapht.alg.shortestpath.BFSShortestPath
import org.jgrapht.graph.{DefaultEdge, SimpleDirectedGraph}
import org.opalj.br.analyses.cg.ConfigurationEntryPointsFinder
import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.bytecode.JavaBase
import org.opalj.log.{ConsoleOPALLogger, GlobalLogContext, OPALLogger}
import org.opalj.tac.cg._
import org.slf4j.{Logger, LoggerFactory}

import java.net.{URL, URLClassLoader}
import java.nio.file.Path
import scala.jdk.CollectionConverters._

class ParsedCallgraph


object ParsedCallgraph {
  final val PARAM_NOT_FOUND = "ParamNotFound"
  type StringGraph = SimpleDirectedGraph[String, DefaultEdge]
  val LOGGER: Logger = LoggerFactory.getLogger(classOf[ParsedCallgraph])

  /**
   * Use Opal's configuration that should already be set up in the project to grab the correct entry points and
   * return them as a set
   *
   * @param project the set-up project object that has a configuration set up to use a manual, configuration based
   *                entry point
   * @return a set of method objects that are the entry methods for the given project.
   */
  private def generateConfigEntryMethods(project: Project[URL]) = {
    ConfigurationEntryPointsFinder
      .collectEntryPoints(project)
      .toSet
  }

  /**
   * Generates a tuple of project objects, the project that only contains packages associated with the SUT, and the
   * full project that is all packages associated with the SUT and packages required by the Java run time.
   *
   * @param conf       The config file for the current analysis
   * @param projectURL the set of strings that are the URL locations of the jar files for the SUT.
   * @return Tuple (project object with only SUT package, project object with SUT package and Java RT)
   */
  private def createProjectWithConfig(conf: Config, projectURL: Set[Path], depsURL: Set[Path]): (Project[URL], Project[URL]) = {
    val p = buildProjectObj(conf, projectURL)
    val fp = extendProjectWithConfig(p, conf, depsURL)
    (p, fp)
  }

  def extendProjectWithConfig(p: Project[URL], conf: Config, depsURL: Set[Path]): Project[URL] = {
    val fp = p.extend(Project(JavaBase, GlobalLogContext, conf))
    val finalP = if (depsURL.nonEmpty) {
      fp.extend(buildProjectObj(conf, depsURL))
    } else fp
    finalP
  }

  /**
   * Builds Project object for getting project urls.
   *
   * @param conf       The config file for the current analysis
   * @param projectURL the set of strings that are the URL locations of the jar files for the SUT.
   * @return Project[URL]
   */
  private def buildProjectObj(conf: Config, projectURL: Set[Path]): Project[URL] = {
    projectURL.map(x => x.toFile).map(x => Project(x, GlobalLogContext, conf)).reduce((a, b) => a.extend(b))
  }

  /**
   * helper function that given a graph and string will first check that the string does not exist in the graph
   * as a node, then if it does not exist will then add the string as a vertex.
   */
  private def safeAddVertexToGraph(g: StringGraph, s: String): Unit = {
    if (!g.containsVertex(s)) g.addVertex(s)
  }

  private def removeDisconnectedNodes(g: StringGraph, entry: Set[Method]): Unit = {
    val finalBFS = new BFSShortestPath[String, DefaultEdge](g)
    val reachableNoEntries: Set[String] = entry
      .flatMap(e => {
        val source = methodToJavaJVM(e)
        if (!g.containsVertex(source)) Set.empty
        else {
          g.vertexSet().asScala
            .filterNot(_ == source)
            .flatMap { target =>
              Option(finalBFS.getPath(source, target))
                .map(_.getVertexList.asScala)
                .getOrElse(Seq.empty)
            }
        }
      })
    val entries = entry.map(methodToJavaJVM).filter(g.containsVertex)
    val reachable = reachableNoEntries ++ entries
    val unreachable = g.vertexSet().asScala.toSet -- reachable
    unreachable.foreach(g.removeVertex)
  }

  /**
   *
   * Generate a full graph from the call graph analysis data by iterating through each reachable method and adding
   * the name as a vertex followed by adding each of its callers as nodes and adding the edge between them.
   *
   * @param callGraph the call graph data from the full project including the java rt.
   * @return a graph object with all reachable nodes.
   */
  private def generateFullGraph(implicit callGraph: CallGraph): StringGraph = {
    val fullGraph = new StringGraph(classOf[DefaultEdge])
    callGraph.reachableMethods().foreach(c => {
      val source = methodToJavaJVM(c.method)
      safeAddVertexToGraph(fullGraph, source)
      callGraph.callersOf(c.method).iterator.foreach(caller => {
        val dest = methodToJavaJVM(caller._1)
        if (!source.equals(dest)) {
          safeAddVertexToGraph(fullGraph, dest)
          fullGraph.addEdge(dest, source)
        }
      })
    })
    fullGraph
  }

  private def generateParsedGraph(fullGraph: StringGraph, data: listData): StringGraph = {

    val parsedPaths = PathParsing.getParsedPaths(fullGraph, data)

    val pGraph = new StringGraph(classOf[DefaultEdge])
    parsedPaths.foreach(p => {
      p
        .sliding(2)
        .map(x => (x.head, x.last))
        .foreach(newEdge => {
          safeAddVertexToGraph(pGraph, newEdge._1)
          safeAddVertexToGraph(pGraph, newEdge._2)
          if (newEdge._1 != newEdge._2)
            pGraph.addEdge(newEdge._1, newEdge._2)
        })
    })
    pGraph
  }

  /**
   * Primary runner method to generate the graph.
   *
   * @param fp           the full project object that includes all incoming project jars as well as the java RT.
   * @param p            the project object that only includes incoming project jars
   * @param entryMethods a set of entry methods that will be used to run analyses
   * @param level        the call graph generation type requested.
   * @return a parsed call graph.
   */
  @throws[OutOfMemoryError]("Ran out of memory generating graph")
  @throws[NoGraphException]("if graph empty or missing")
  private def generateGraph(fp: Project[URL], p: Project[URL], entryMethods: Set[Method], level: AnalysisType,
                            throwOpt: Boolean, iterOpt: Boolean, classLoader: URLClassLoader, entry: String,
                            artifact: Artifact): StringGraph = {
    // Generate the call-graph data
    val callGraph = level match {
      case AnalysisType.CHA => fp.get(CHACallGraphKey)
      case AnalysisType.RTA => fp.get(RTACallGraphKey)
      case AnalysisType.CTA => fp.get(CTACallGraphKey)
      case AnalysisType.FTA => fp.get(FTACallGraphKey)
      case AnalysisType.MTA => fp.get(MTACallGraphKey)
      case AnalysisType.XTA => fp.get(XTACallGraphKey)
      case AnalysisType.CFA0 => fp.get(TypeBasedPointsToCallGraphKey)
      case AnalysisType.CFA1 => fp.get(AllocationSiteBasedPointsToCallGraphKey)
      case AnalysisType.CFA10 => fp.get(CFA_1_0_CallGraphKey)
      case AnalysisType.CFA11 => fp.get(CFA_1_1_CallGraphKey)
    }

    LOGGER.info(s"call graph data generated")
    // generate full graph
    val fullGraph = generateFullGraph(callGraph)
    // createDotFile(fullGraph, "test")
    LOGGER.info(s"full graph generated")
    if (fullGraph.vertexSet().isEmpty) {
      throw new NoGraphException(s"Full Graph for ${entryMethods.head} is empty")
    }

    val filteringData = new listData(p, fp, callGraph)

    val parsedGraph = generateParsedGraph(fullGraph, filteringData)
    LOGGER.info(s"first pass graph generated")
    if (parsedGraph.vertexSet().isEmpty || !parsedGraph.containsVertex(methodToJavaJVM(entryMethods.head))) {
      throw new NoGraphException(s"Parsed Graph for ${entryMethods.head} is empty")
    }
    removeDisconnectedNodes(parsedGraph, entryMethods)
    // remove all bad edges and nodes that come from throws
    if (throwOpt) {
      ThrowOptimization.throwOptimization(parsedGraph, filteringData, entry, artifact)
      removeDisconnectedNodes(parsedGraph, entryMethods)
    }
    if (iterOpt) {
      IteratorOptimization.iteratorOptimization(parsedGraph, filteringData, classLoader, entry, artifact )
      removeDisconnectedNodes(parsedGraph, entryMethods)
    }
    parsedGraph

  }

  /**
   * Parse the entry arguments coming from a call from propcov
   *
   * @param entry properly formed string of the entry point method
   * @return a tuple of strings of the declaring class and method name of the entry point
   */
  private def parseEntryArg(entry: String): (String, String) = {
    val parsedEntry = entry.split("[(]").apply(0).replaceAll("[.]", "/")
    val splitIdx = parsedEntry.lastIndexOf("/")
    val entryPair = parsedEntry.splitAt(splitIdx)
    (entryPair._1, entryPair._2.substring(1))
  }

  /**
   * entry point for analyzing a project, will generate and return a graph to the caller
   *
   * @param entry          well-formed entry point method string
   * @param jars           set of string url locations of relevant project jars, specifically the test jar and jar without
   *                       dependencies.
   * @param analysisConfig config object for altering opal parameters (e.g. logger level)
   * @param classLoader    the URLClassLoader associated with the sut
   * @return a properly parsed call graph of the input project.
   */
  @throws[OutOfMemoryError]("Ran out of memory generating graph")
  @throws[NoGraphException]("if graph empty or missing")
  def build(entry: String, jars: Set[Path], deps: Set[Path], analysisConfig: OpalAnalysisConfig, classLoader: URLClassLoader, artifact: Artifact): StringGraph = {
    LOGGER.info(s"Starting Analysis for entrypoint: $entry")

    val analysisType = analysisConfig.analysisType
    val loggerLevel = analysisConfig.opalLoggerType
    val iterOpt = analysisConfig.analysisFlags.contains(AnalysisConfig.AnalysisFlag.ITERATOR)
    val throwOpt = analysisConfig.analysisFlags.contains(AnalysisConfig.AnalysisFlag.EXCEPTION)
    val cfaOpt = analysisConfig.analysisFlags.contains(AnalysisConfig.AnalysisFlag.CFA)
    val invokeFlag = analysisConfig.analysisFlags.contains(AnalysisConfig.AnalysisFlag.NO_LAMBDA)
    val paramTypes = InitialAnalysis.parseEntryPointTypes(entry, jars, classLoader)
    val configStrings = parseEntryArg(entry)
    LOGGER.info(s"Initial Types: $paramTypes")
    val newConf = ConfigOps.createConfigChange(configStrings._1, configStrings._2, paramTypes, analysisType, cfaOpt = cfaOpt, invokeFlag)
    val (project, fullProject) = createProjectWithConfig(newConf, jars, deps)
    val entries = generateConfigEntryMethods(project)
    val (projectM, fullProjectM) = analysisType match {
      case AnalysisType.CFA0 | AnalysisType.CFA1 if cfaOpt => //| AnalysisType.CFA10 | AnalysisType.CFA11 =>
        CFAOptimization.fixCFAFinalPrivateFields(project, paramTypes, newConf, deps)
      case _ => (project, fullProject)
    }
    val logLevel = loggerLevel match {
      case OPALLoggerType.warn => org.opalj.log.Warn
      case OPALLoggerType.info => org.opalj.log.Info
      case OPALLoggerType.error => org.opalj.log.Error
      case OPALLoggerType.fatal => org.opalj.log.Fatal
    }
    OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, logLevel))

    generateGraph(fullProjectM, projectM, entries, analysisType, throwOpt, iterOpt, classLoader, entry, artifact)
  }
}

