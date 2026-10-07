package edu.uic.bitslab.propcov.extensions.analysisframework.opal

import edu.uic.bitslab.propcov.extensions.analysisframework.opal.ParsedCallgraph.LOGGER

import java.lang.reflect.{Field, Type, TypeVariable}
import java.net.URLClassLoader
import java.nio.file.Path
import java.util
import java.util.jar.JarFile
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

object InitialAnalysis {

  def parseEntryPointTypes(entry: String, jars: Set[Path], cl: URLClassLoader): util.List[String] = {
    def logError(cls: Class[_], e: Throwable) = {
      LOGGER.warn(s"${cls.toString} threw ${e.toString}")
      Set[java.lang.reflect.Method]()
    }

    val packages = getPackagesFromJars(jars)

    val parsedEntry = entry.split("[(]").apply(0)
    val splitIdx = parsedEntry.lastIndexOf(".")
    val entryPair = parsedEntry.splitAt(splitIdx)
    val cls = cl.loadClass(entryPair._1)
    val testClsTypes = getTestClassTypes(cls, packages, Set(cls.getTypeName.replaceAll("[.]", "/")), cl).getOrElse(Set[String]())
    val testClassFields = getAllGlobals(cls, cl, packages, Set[String]())
    val methods = try {
      cls.getDeclaredMethods.toSet
    } catch {
      case e: NoClassDefFoundError => logError(cls, e)
      case e: ClassNotFoundException => logError(cls, e)
    }
    val meth = methods.filter(m => m.getName.equals(entryPair._2.substring(1)))
    if (meth.size > 1)
      throw new Error("implement overloaded entry point")
    val ret = meth.headOption match {
      case Some(m) =>
        m.getGenericParameterTypes // This will tell us if we need to bring in all children types // TYPE VARIABLE
          .map(tunnelIntoArrays)
          .filterNot(checkPrimitive)
          .flatMap(getNames)
          //.map(x => x.getTypeName)
          .flatMap(s => tunnelTypes(s, cl, Set(s + "+"), packages)).toSet
          .map((s: String) => s.replaceAll("[.]", "/"))
      case None =>
        LOGGER.warn("initial types cannot find entry point")
        Set[String]()
    }
    val test = ret ++ testClassFields ++ testClsTypes
    test.toList.asJava
  }

  private def getNames(typ: Type):Set[String] = {
    typ match {
      case value: TypeVariable[?] =>
        value.getBounds.map(x => x.getTypeName.split('<').head).toSet
      case _ =>
        Set(typ.getTypeName.split('<').head)
    }
  }

  @tailrec
  private def getAllDeclaredFields(curr: Class[?], packages: Set[String], acc: Set[Field]): Set[Field] = {
    if (curr == null || !packages.contains(curr.getPackageName)) {
      acc
    } else {
      getAllDeclaredFields(curr.getSuperclass, packages, acc ++ curr.getDeclaredFields)
    }
  }

  @tailrec
  private def tunnelIntoArrays(typ: Type): Type = {
    typ match {
      case t: Class[_] if t.isArray => tunnelIntoArrays(t.getComponentType)
      case _ => typ
    }
  }

  private def checkPrimitive(typ: Type): Boolean = {
    typ match {
      case t: Class[_] => t.isPrimitive
      case _ => false
    }
  }

  private def tunnelTypes(entry: String, urlCL: URLClassLoader, accum: Set[String], packages: Set[String]): Set[String] = {
    @tailrec
    def loop(todo: List[String], seen: Set[String]): Set[String] = todo match {
      case ::(head, next) =>
        val sanitized = head.stripSuffix("+").takeWhile(_ != '<')
        val clazz = urlCL.loadClass(sanitized)
        val fieldTypes = getAllDeclaredFields(clazz, packages, Set.empty)
          .filter((f: Field) => packages.contains(f.getType.getPackageName) && !f.getType.isArray)
          .map(f => f.getType.getTypeName + "+").diff(seen)
        loop(fieldTypes.toList ++ next, seen ++ fieldTypes)
      case Nil => seen
    }

    loop(List(entry), accum)
  }

  private def getPackagesFromJars(jars: Set[Path]): Set[String] = {
    jars.map(x => new JarFile(x.toAbsolutePath.normalize.toFile, true))
      .map(j => j.entries())
      .map(e => {
        e.asIterator()
          .asScala.toSet
          .filter(se => se.getName.endsWith(".class"))
      })
      .flatMap(s => s
        .map(je => je.getName.replace('/', '.').stripSuffix(".class"))
        .map(cn => {
          cn.lastIndexOf('.') match {
            case -1 => None
            case idx => Some(cn.substring(0, idx))
          }
        })
      )
      .flatten
  }

  @tailrec
  private def getAllGlobals(cls: Class[_], cl: URLClassLoader, packages: Set[String], accum: Set[String]): Set[String] = {
    getGlobals(cls) match {
      case Some(value) =>
        val superName = cls.getGenericSuperclass.getTypeName.split('<').head
        val superCls = cl.loadClass(superName)
        if (packages.contains(superCls.getPackageName)) {
          getAllGlobals(superCls, cl, packages, accum ++ value)
        } else {
          accum ++ value
        }
      case None =>
        accum
    }
  }

  @tailrec
  private def getTestClassTypes(cls: Class[_], packages: Set[String], accum: Set[String], cl: URLClassLoader): Option[Set[String]] = {
    val superName = cls.getGenericSuperclass.getTypeName.split('<').head
    val superCls = cl.loadClass(superName)
    if (packages.contains(superCls.getPackageName)) {
      getTestClassTypes(superCls, packages, accum ++ Set(superName.replaceAll("[.]", "/")), cl)
    } else {
      Some(accum)
    }
  }

  private def getGlobals(cls: Class[_]): Option[Set[String]] = {
    try {
      val res = cls
        .getDeclaredFields
        .toSet
        .map((x: Field) => x.getType)
        .map(x => tunnelIntoArrays(x))
        .filterNot(checkPrimitive)
        .map(x => x.getTypeName)
        .map((s: String) => s.replaceAll("[.]", "/"))
      Some(res)
    } catch {
      case e: NoClassDefFoundError =>
        LOGGER.warn(s"${cls.toString} threw ${e.toString}")
        None
      case e: ClassNotFoundException =>
        LOGGER.warn(s"${cls.toString} threw ${e.toString}")
        None
    }
  }


}
