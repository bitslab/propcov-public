package edu.uic.bitslab.propcov.extensions.analysisframework.opal.Optimizations

import edu.uic.bitslab.propcov.core.analyze.Artifact
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.OpalUtil._
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.ParsedCallgraph.{LOGGER, StringGraph}
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.listData

import java.net.URLClassLoader
import scala.jdk.CollectionConverters._


object IteratorOptimization {

  def iteratorOptimization(parsedGraph: StringGraph, data: listData, urlCL: URLClassLoader, entry: String, artifact: Artifact): Unit = {
    LOGGER.info("Starting Iterator Optimization")
    val removedNodes: Set[String] = parsedGraph
      .vertexSet()
      .asScala.toSet.filter(x => {
        val parsedMethod = x.split("[(]")
        val splitIdx = parsedMethod.apply(0).lastIndexOf(".")
        val entryPair = parsedMethod.apply(0).splitAt(splitIdx)

        // this might have problems if we run into unavailable classes
        val ret = try {
          val meth = data.filteredReachableMethods.filter(m => methodToJavaJVM(m.method).equals(x))
          if (meth.nonEmpty) {
            val methstr = meth.head.method.declaringClassType.fqn.replaceAll("/", ".")
            val methcls = urlCL.loadClass(methstr) // what if we can't load the class?
            val iterSet1 = Set("hasNext", "next")
            val iterSet2 = Set("iterator")

            // this will capture any string with substrings that have the key strings
            (classOf[java.util.Iterator[_]].isAssignableFrom(methcls) &&
              iterSet1.exists(entryPair._2.substring(1).contains)) ||
              (classOf[java.lang.Iterable[_]].isAssignableFrom(methcls) &&
                iterSet2.exists(entryPair._2.substring(1).contains))
          } else {
            false
          }

        } catch {
          case e@(_: NoClassDefFoundError | _: ClassNotFoundException) =>
            LOGGER.error(s"Could not load class from $x\n\t${e.toString} in iterator optimization")
            false
        }
        ret
      })

    optimizationReportWriter(entry, "IteratorOptimization", removedNodes, artifact)

    removedNodes.foreach(v => parsedGraph.removeVertex(v))
    LOGGER.info(s"Iterator Optimization removed ${removedNodes.size} nodes")
    LOGGER.info("Completed Iterator Optimization")
  }
}
