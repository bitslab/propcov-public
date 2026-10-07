package edu.uic.bitslab.propcov.extensions.analysisframework.opal

import edu.uic.bitslab.propcov.core.analyze.Artifact
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.ParsedCallgraph.StringGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.nio.dot.DOTExporter
import org.jgrapht.nio.{Attribute, DefaultAttribute}
import org.opalj.br.{DeclaredMethod, Method}
import org.opalj.tac.{Call, DUVar}
import org.opalj.value.ValueInformation

import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.util
import scala.annotation.unused

object OpalUtil {
  def methodToJavaJVM(m: Method): String = {
    val dct = m.classFile.thisType.toJava
    val desc = m.descriptor.toJVMDescriptor
    s"$dct.${m.name}$desc"
  }

  def optimizationReportWriter(entry: String, fileNamePrefix: String, results: Set[String], artifact: Artifact): Unit = {

    val numNodes: Int = results.size
    val nodeString: String = if (numNodes==0) "[]" else results.map(x=> s"\"$x\"").mkString("[",",\n","]")

    val reportMap = Array(
      s"\"Property\": \"$entry\"" ,
      s"\"Count\": \"$numNodes\"",
      s"\"Nodes/Edges\": $nodeString"
    ).mkString("{\n",",\n","\n}")

    artifact.addReport(reportMap.getBytes(StandardCharsets.UTF_8), s"$fileNamePrefix-${entry.replace("/", ".")}.json")
  }

  def methodToJavaJVM(className: String, methodName: String, methodDescriptor: String): String = {
    s"$className.$methodName$methodDescriptor"
  }

  def methodToJavaJVM[T <: Call[DUVar[ValueInformation]]](m: T): String = {
    val dct = m.declaringClass.toJava
    val desc = m.descriptor.toJVMDescriptor
    s"$dct.${m.name}$desc"
  }

  def methodToJavaJVM(m: DeclaredMethod): String = {
    val dct = m.declaringClassType.toJava
    val desc = m.descriptor.toJVMDescriptor
    s"$dct.${m.name}$desc"
  }

  @unused
  def createDotFile(graph: StringGraph, title: String): Unit = {
    println("====Creating Dot File...")

    val out = new FileWriter(s"$title.dot")

    val dotExporter = new DOTExporter[String, DefaultEdge]()
    dotExporter.setVertexAttributeProvider(v => {
      val map = new util.LinkedHashMap[String, Attribute]
      map.put("label", DefaultAttribute.createAttribute(v))
      map
    })
    dotExporter.setGraphAttributeProvider(() => {
      val attribute = new util.LinkedHashMap[String, Attribute]()
      attribute.put(" rankdir ", DefaultAttribute.createAttribute(" LR "))
      attribute
    })
    dotExporter.exportGraph(graph, out)
  }
}
