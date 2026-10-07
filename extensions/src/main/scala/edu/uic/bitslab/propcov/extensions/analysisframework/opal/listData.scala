package edu.uic.bitslab.propcov.extensions.analysisframework.opal

import edu.uic.bitslab.propcov.extensions.analysisframework.opal.OpalUtil.methodToJavaJVM
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.cg.CallGraph

import java.net.URL
import scala.annotation.unused

class listData(val p: Project[URL], val fp: Project[URL], callGraph: CallGraph) {
  lazy val packageComparisonStringSet: Set[String] = packageSet.map(p => p.replaceAll("/", "."))
  lazy val allPackageStringSet: Set[String] = allPackageSet.map(p => p.replaceAll("/", "."))
  // filter the reachable methods to only include methods in the packages we care about
  lazy val filteredReachableMethods: Set[Context] = callGraph
    .reachableMethods()
    .filter(context => packageSet.contains(context.method.declaringClassType.packageName))
    .toSet
  lazy val allClassTypes: Set[String] = filteredReachableMethods
    .map(c => c.method.declaringClassType.toJVMTypeName.dropRight(1).substring(1))
  lazy val bridgeMethodStrings: Set[String] = p.allMethods.toSet.filter(m => m.isBridge).map(methodToJavaJVM)
  lazy val syntheticMethodStrings: Set[String] = p.allMethods.toSet.filter(m => m.isSynthetic).map(methodToJavaJVM)
  @unused
  lazy val abstractMethodStrings: Set[String] = abstractMethods.map(methodToJavaJVM)
  lazy val allThrowMethods: Set[Method] = p.allMethodsWithBody.toSet
    .filter(m => m.body.exists(c => c.exists(i => i.instruction.isAthrow)))
  private lazy val packageSet: Set[String] = p.packages.toSet
  // grab the set of all available packages in a string format
  private lazy val allPackageSet: Set[String] = fp.packages.toSet
  private lazy val abstractMethods: Set[Method] = p.allMethods.toSet.filter(m => m.isAbstract)
}
