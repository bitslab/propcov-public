package edu.uic.bitslab.propcov.extensions.analysisframework.opal

import edu.uic.bitslab.propcov.extensions.analysisframework.opal.OpalUtil.methodToJavaJVM
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.ParsedCallgraph.{PARAM_NOT_FOUND, StringGraph}
import org.jgrapht.alg.shortestpath.DijkstraManyToManyShortestPaths
import org.jgrapht.graph.DefaultEdge
import org.opalj.br.fpcf.properties.Context

import java.util.regex.Matcher
import scala.annotation.tailrec
import scala.collection.immutable.{HashSet, ListSet}
import scala.jdk.CollectionConverters._


object PathParsing {
  private type StringMultiDijkstra = DijkstraManyToManyShortestPaths[String, DefaultEdge]

  def getParsedPaths(fullGraph: StringGraph, data: listData): Set[ListSet[String]] = {
    // Generate paths between nodes without filtering
    val pathULists = generatePaths(fullGraph, data.filteredReachableMethods)

    // parse the paths
    parsePaths(pathULists, data)

  }

  /**
   * helper function that recursively checks the incoming parameter string against our set of packages to find the
   * correct object in the case of an inner object.
   */
  @tailrec
  private def parseLambdaParamHelper(temp: Option[String], allPackages: Set[String]): Option[String] = {
    temp match {
      case Some(value) =>
        val last = value.lastIndexOf(".")
        if (last < 0) return None
        val pack = value.substring(0, last)
        if (allPackages.contains(pack)) {
          Some(pack)
        } else {
          if (pack.isEmpty)
            None
          else
            parseLambdaParamHelper(Some(pack), allPackages)
        }
      case None => None
    }
  }

  @tailrec
  private def parseLambdaClassMethodHelper(temp: Option[String], allTypes: Set[String]): Option[String] = {
    temp match {
      case Some(value) =>
        val last = value.lastIndexOf("/")
        if (last < 0) return None
        val methodClass = value.substring(0, last)
        if (allTypes.contains(methodClass))
          return Some(methodClass)
        val methodClassWInner = methodClass.replaceAll("""/(?!.*/)""", Matcher.quoteReplacement("$"))
        // what if multiple inner classes
        if (allTypes.contains(methodClassWInner))
          return Some(methodClassWInner)
        if (methodClass.isEmpty)
          None
        else
          parseLambdaClassMethodHelper(Some(methodClass), allTypes)
      case None => None
    }
  }

  /**
   * Method to parse the parameters inside a java string initially parsed by OPAL, into a more reasonable
   * list of parameters matching Java standards
   *
   * @param param       the string of the parameter to parse
   * @param allPackages a set of all packages available to the full project
   * @return the string of a parameter that has been parsed for Java Standards to match with Jacoco.
   */
  private def parseLambdaParamRet(param: String, allPackages: Set[String]): String = {
    val temp =
      if (param.contains('$'))
        Some(param.replaceAll("\\$", "."))
      else
        None
    val found = parseLambdaParamHelper(temp, allPackages)
    val ret = found match {
      case Some(paramS) =>
        val rest = param.substring(paramS.length + 1).dropWhile(_ == '$')
        val ret = s"$paramS.$rest;"
        ret.replaceAll("\\.", "/")
      case None => PARAM_NOT_FOUND
    }
    ret
  }

  private def parseLambdaClassMethod(clazzMethod: String, allTypes: Set[String]): String = {
    val temp =
      if (clazzMethod.contains('$'))
        Some(clazzMethod.replace('$', '.').replace('.', '/'))
      else
        None
    val found = parseLambdaClassMethodHelper(temp, allTypes)
    val ret = found match {
      case Some(value) =>
        val rest = clazzMethod.substring(value.length + 1)
        s"$value.$rest".replaceAll("/", ".")
      case None => "ClassNotFound"
    }
    ret
  }

  /**
   * function to parse lambda method strings from the full string given from OPAL and breaking down to the same
   * string that would be used by jacoco
   *
   * @param ms          incoming string from a java lambda method that has been parsed initially by opal
   * @param allPackages a list of all available packages from the full project
   * @return a string that should match JaCoCo's / Java's naming convention for java methods
   */
  private def parseLambdaString(ms: String, allPackages: Set[String], allTypes: Set[String]): String = {
    val regEx = """^(?<clazz>[^()$]+)\$(?<method>[^()]+)\((?<params>.*)\)(?<descriptor>[^()]+):+.*$""".r
    ms.replace("::", ":") match {
      case regEx(clazz, method, params, desc) =>
        val clazzmethod = s"$clazz$$$method"
        val test = parseLambdaClassMethod(clazzmethod, allTypes)
        val typs: HashSet[Char] =
          HashSet('B', 'C', 'D', '[', 'F', 'I', 'J', 'S', 'Z') // What about mixed with Objects
        val paramString =
          if (!params.forall(x => typs.contains(x)) && params.length > 1) {
            params.split(':').map(p => {
              val rest = parseLambdaParamRet(p.substring(1), allPackages)
              s"${p.charAt(0)}$rest"
            }).mkString("(", "", ")")
          } else if (params.isEmpty) {
            s"($params)"
          } else {
            s"($params);"
          }
        val returnTyp = if (desc.length > 1) {
          val rest = parseLambdaParamRet(desc.substring(1), allPackages)
          s"${desc.charAt(0)}$rest"
        } else s"$desc;"
        s"$test$paramString$returnTyp"
      case _ => s"$ms#NOTPARSED"
    }
  }

  private def checkDest(packageSet: Set[String], methodString: String): Boolean = {
    val containsPackage = packageSet.exists(p => methodString.contains(p))
    val containsClinit = methodString.contains("clinit")
    containsPackage && !containsClinit
  }

  private def parseStandardPaths(key: String, paths: Set[ListSet[String]], data: listData): Set[ListSet[String]] = {
    paths
      .map(u => u
        .takeWhile(checkDest(data.packageComparisonStringSet, _))
        .diff(data.bridgeMethodStrings)
        .diff(data.syntheticMethodStrings))
      .filter(u => !u.exists(_.contains(key)))
      .filter(e => e.size > 1)
  }

  private def parseLambdaPaths(key: String, paths: Set[ListSet[String]], data: listData): Set[ListSet[String]] = {
    paths
      .filter(l => l.exists(_.contains(key)))
      .map(l => l.filter(!_.contains("<init>")))
      .map(l => l.filter(!_.contains("$newInstance")))
      .map(u => u
        .takeWhile(checkDest(data.packageComparisonStringSet, _))
        .diff(data.bridgeMethodStrings)
        .diff(data.syntheticMethodStrings))
      .filter(_.size > 1)
  }

  private def cleanLambdas(key: String, paths: Set[ListSet[String]], data: listData): Set[ListSet[String]] = {
    paths.map(f => {
      f
        .map(ms => {
          if (ms.contains(key)) {
            parseLambdaString(ms, data.allPackageStringSet, data.allClassTypes)
          } else {
            ms
          }
        })
    })
  }

  private def parsePaths(pathULists: Set[ListSet[String]], data: listData): Set[ListSet[String]] = {
    val lambdaSubstringKey = "$Lambda"
    val standardParsedPaths = parseStandardPaths(lambdaSubstringKey, pathULists, data)
    val lambdaParsedPaths = parseLambdaPaths(lambdaSubstringKey, pathULists, data)
    val parsedPaths = standardParsedPaths ++ lambdaParsedPaths
    cleanLambdas(lambdaSubstringKey, parsedPaths, data)
  }

  @throws[OutOfMemoryError]("Ran out of memory generating graph")
  private def generatePaths(fullGraph: StringGraph, filteredMethods: Set[Context]): Set[ListSet[String]] = {
    val methodStringSet = filteredMethods.map(c => methodToJavaJVM(c.method))
    val searcher = new StringMultiDijkstra(fullGraph)
    val paths = searcher.getManyToManyPaths(methodStringSet.asJava, methodStringSet.asJava)
    for {
      src <- methodStringSet
      dest <- methodStringSet
      if src != dest
      path <- Option(paths.getPath(src, dest))
    } yield path.getVertexList.asScala.to(ListSet)
  }
}
