package edu.uic.bitslab.propcov.extensions.analysisframework.opal.Optimizations

import edu.uic.bitslab.propcov.core.analyze.Artifact
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.OpalUtil.{methodToJavaJVM, optimizationReportWriter}
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.ParsedCallgraph.{LOGGER, StringGraph}
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.listData
import org.jgrapht.alg.shortestpath.AllDirectedPaths
import org.jgrapht.graph.{DefaultEdge, SimpleDirectedGraph}
import org.opalj.br.Method
import org.opalj.tac._
import org.opalj.value.ValueInformation

import scala.jdk.CollectionConverters._

object ThrowOptimization {
  private type IntGraph = SimpleDirectedGraph[Int, DefaultEdge]

  def throwOptimization(g: StringGraph, data: listData, entry: String, artifact: Artifact): Unit = {
    LOGGER.info("Starting Throw Optimization")
    val tac = data.fp.get(ComputeTACAIKey)
    val check = data.allThrowMethods.filter(m => g.containsVertex(methodToJavaJVM(m)))

    val badEdges = check
      .map(m => (methodToJavaJVM(m), getUnwantedEdges(m, tac)))
      .toMap

    val removedEdges = badEdges
      .filter(x => x._2.nonEmpty)
      .map(x => (x._1, x._2.filter(y => g.containsVertex(y))))

    val removedEdgesString:Set[String] = removedEdges.flatMap{
      case (src, dests) =>
        dests.map(dest => s"$src -> $dest")
    }.toSet

    removedEdges.foreach(x => x._2.foreach(d => g.removeEdge(x._1, d)))

    optimizationReportWriter(entry, "ThrowOptimization", removedEdgesString, artifact)
    LOGGER.info("Completed Throw Optimization")
  }

  private def findAllMethodCalls(expr: Expr[DUVar[ValueInformation]], pc: Integer): Set[(Call[DUVar[ValueInformation]], Integer)] = {
    expr match {
      case InvokedynamicFunctionCall(_, _, _, _, params) =>
        val args = params
          .flatMap(findAllMethodCalls(_, pc))
        Set.from(args)
      case PrimitiveTypecastExpr(_, _, operand) => findAllMethodCalls(operand, pc)
      case BinaryExpr(_, _, _, left, right) => findAllMethodCalls(left, pc).union(findAllMethodCalls(right, pc))
      case Compare(_, left, _, right) => findAllMethodCalls(left, pc).union(findAllMethodCalls(right, pc))
      case PrefixExpr(_, _, _, operand) => findAllMethodCalls(operand, pc)
      case InstanceOf(_, value, _) => findAllMethodCalls(value, pc)
      case _: ValueExpr[_] => Set.empty
      case New(_, _) => Set.empty
      case _: ArrayExpr[_] => Set.empty
      case _: FieldRead[_] => Set.empty
      case call: FunctionCall[_] =>
        val args = call
          .allParams
          .flatMap(findAllMethodCalls(_, pc))
        Set.from(args) + ((call, pc))
      case _ => throw new Error()
    }
  }

  private def findAllMethodCalls(stmt: Stmt[DUVar[ValueInformation]]): Set[(Call[DUVar[ValueInformation]], Integer)] = {
    stmt match {
      case Goto(_, _) => Set.empty
      case CaughtException(_, _, _) => Set.empty
      case ReturnValue(_, expr) => findAllMethodCalls(expr, stmt.pc)
      case If(_, left, _, right, _) => findAllMethodCalls(left, stmt.pc).union(findAllMethodCalls(right, stmt.pc))
      case JSR(_, _) => Set.empty
      case InvokedynamicMethodCall(_, _, _, _, _) => Set.empty
      case call: MethodCall[_] =>
        val args = call
          .allParams
          .flatMap(findAllMethodCalls(_, stmt.pc))
          .toSet
        Set.from(args) + ((call, stmt.pc))
      case Switch(_, _, index, _) => findAllMethodCalls(index, stmt.pc)
      case stmt: SynchronizationStmt[_] => findAllMethodCalls(stmt.objRef, stmt.pc)
      case stmt: AssignmentLikeStmt[_] => findAllMethodCalls(stmt.expr, stmt.pc)
      case Checkcast(_, value, _) => findAllMethodCalls(value, stmt.pc)
      case _: SimpleStmt => Set.empty
      case Ret(_, _) => Set.empty
      case _: VariableFreeStmt => Set.empty
      case stmt: FieldWriteAccessStmt[_] => findAllMethodCalls(stmt.value, stmt.pc)
      case ArrayStore(_, arrayRef, _, value) => findAllMethodCalls(arrayRef, stmt.pc).union(findAllMethodCalls(value, stmt.pc))
      case Throw(_, exception) => findAllMethodCalls(exception, stmt.pc)
      case _ => throw new Error()
    }
  }

  private def findAllMethodCalls(stmts: Array[Stmt[DUVar[ValueInformation]]]): Set[(Call[DUVar[ValueInformation]], Integer)] = {
    Set.from(stmts.flatMap(s => findAllMethodCalls(s)))
  }

  private def retrievePaths(g: IntGraph, entryIdx: Int, query: Set[Int], pathFinder: AllDirectedPaths[Int, DefaultEdge]): Set[Int] = {
    if (g.containsVertex(entryIdx))
      pathFinder
        .getAllPaths(Set(entryIdx).asJava, query.filter(g.containsVertex).asJava, true, null) // null here might cause slower performance
        .asScala.flatMap(_.getVertexList.asScala).toSet
    else
      Set.empty[Int]
  }

  private def getUnwantedEdgesHelper(taCode: AITACode[TACMethodParameter, ValueInformation], returns: Set[Int], throws: Set[Int]): Set[String] = {
    val allCalls = findAllMethodCalls(taCode.instructions)
    val iCfg = new IntGraph(classOf[DefaultEdge])

    taCode.stmts.indices.foreach(iCfg.addVertex)
    taCode.stmts.indices.foreach { i =>
      taCode.cfg.successors(i).foreach { s =>
        iCfg.addEdge(i, s)
      }
    }

    val paths = new AllDirectedPaths(iCfg)
    val entryIdx = taCode.pcToIndex(taCode.stmts.head.pc)

    val entryToReturn = retrievePaths(iCfg, entryIdx, returns, paths)
    val entryToThrow = retrievePaths(iCfg, entryIdx, throws, paths)

    val pathsToRemove = (entryToThrow -- entryToReturn).map(taCode.stmts(_).pc)

    allCalls
      .filter({ case (_, pc) => pathsToRemove.contains(pc) })
      .map({ case (c, _) => methodToJavaJVM(c) })
  }

  private def getUnwantedEdges(m: Method, tac: Method => AITACode[TACMethodParameter, ValueInformation]): Set[String] = {
    val taCode = tac(m)
    val statements = taCode.stmts

    val throws = taCode.instructions
      .filter(_.isThrow)
      .map(x => taCode.pcToIndex(x.pc))
      .toSet

    val returns = statements
      .filter(x => x.astID == Return.ASTID || x.astID == ReturnValue.ASTID || x.astID == Ret.ASTID)
      .map(x => taCode.pcToIndex(x.pc))
      .toSet

    if (throws.isEmpty) Set.empty
    else getUnwantedEdgesHelper(taCode, returns, throws)

  }
}
