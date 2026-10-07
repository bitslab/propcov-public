package edu.uic.bitslab.propcov.extensions.analysisframework.opal.Optimizations

import com.typesafe.config.Config
import de.siegmar.fastcsv.writer.CsvWriter
import edu.uic.bitslab.propcov.extensions.analysisframework.opal.ParsedCallgraph.{LOGGER, extendProjectWithConfig}
import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.bi.{ACC_FINAL, ACC_PRIVATE, ACC_PUBLIC}
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.{GETFIELD, PUTFIELD}
import org.opalj.br.{ClassFile, Field, FieldTemplates, MethodTemplates}
import org.opalj.log.GlobalLogContext

import java.io.{File, IOException}
import java.net.URL
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters._

object CFAOptimization {


  private def newFieldNameGen(classTypeName: String, fieldName: String): String = {
    s"_PROPCOV_${classTypeName.replaceAll("/", "_")}_$fieldName"
  }

  private def privateRewrite(cf: ClassFile, fields: ArraySeq[Field]): MethodTemplates = {
    val fieldNames = fields.map(x => x.name -> x).toMap
    val clsType = cf.thisType
    cf.methods // what happens if there is a class with the same name that extends outside the package.
      .filter(x => x.body.isDefined)
      .map(x => x.copy(body = x.body.map { code =>
        val updatedInst = code.instructions.map {
          case GETFIELD(declType, name, fieldType)
            if fieldNames.keySet.contains(name) &&
              declType == clsType => GETFIELD(declType, newFieldNameGen(clsType.fqn, name), fieldType)
          case PUTFIELD(declType, name, fieldType)
            if fieldNames.keySet.contains(name) &&
              declType == clsType => PUTFIELD(declType, newFieldNameGen(clsType.fqn, name), fieldType)
          case other => other
        }
        code.copy(instructions = updatedInst)
      }))
  }

  private def cleanOutputDir(outputDir: Path): Path = {
    Files.walkFileTree(outputDir, new SimpleFileVisitor[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        if (dir != dir.getRoot) Files.delete(dir)
        FileVisitResult.CONTINUE
      }
    })
  }

  private def buildTempClassFiles(outputDir: Path, updatedClassFiles: Iterable[(ClassFile, URL)]): Seq[Path] = {
    LOGGER.info("Generating Temp Class Files")
    val mappingFile = outputDir.resolve(s"ClassNameMapping.csv")
    Files.createDirectories(mappingFile.getParent)
    val header = List("ClassName", "FileName")
    val csv = CsvWriter.builder().build(mappingFile)
    csv.writeRecord(header.asJava)

    val ret = updatedClassFiles.map { case (cf, _) =>
        val className = s"${cf.thisType.fqn}"
        val finalClassName =
          if (className.length > 200)
            s"${className.substring(0, 200)}_${className.substring(200).hashCode.toString}.class"
          else
            s"$className.class" // This might have issues if the entry method is caught here.
        csv.writeRecord(List(className, finalClassName).asJava)
        val path = outputDir.resolve(finalClassName)
        Files.createDirectories(path.getParent)
        val bytecode = Assembler(toDA(cf))
        (path, bytecode)
      }
      .toList
      .map { case (path, bytes) => Files.write(path, bytes).toAbsolutePath }
    csv.close()
    ret
  }


  def fixCFAFinalPrivateFields(p: Project[URL], targetTypes: java.util.List[String],
                               config: Config, depsURL: Set[Path]): (Project[URL], Project[URL]) = {
    LOGGER.info("Starting CFA Optimization")
    val allCwS: Map[ClassFile, URL] = p.classFilesWithSources.toMap

    val rawTargetTypes = targetTypes.asScala.toSet
    val baseTargetTypes = rawTargetTypes.map(_.stripSuffix("+"))
    val wildcardTargetTypes = rawTargetTypes.collect { case t if t.endsWith("+") => t.stripSuffix("+") }

    val matchingBaseClasses = allCwS.filter { case (cf, _) =>
      wildcardTargetTypes.exists(prefix => cf.fqn.startsWith(prefix))
    }

    val subclassTypes = matchingBaseClasses
      .flatMap { case (cf, _) => p.classHierarchy.allSubclassTypes(cf.thisType, reflexive = false) }
      .map(_.fqn)
      .toSet

    val allTargetTypes = baseTargetTypes ++ subclassTypes

    val removeFinal: Int => Int = flags => flags & ~ACC_FINAL.mask
    val makePublic: Int => Int = flags => (flags & ~ACC_PRIVATE.mask) | ACC_PUBLIC.mask

    val updatedClassFiles: Iterable[(ClassFile, URL)] = allCwS.map { case (cf, url) =>
      if (!allTargetTypes.exists(prefix => cf.fqn.startsWith(prefix))) {
        (cf.copy(), url)
      } else {
        val updatedFields: FieldTemplates = cf.fields.map { field: Field =>
          val newAccessFlags: Int = List(
            Option.when(field.isFinal)(removeFinal),
            Option.when(field.isPrivate)(makePublic)
          ).flatten.foldLeft(field.accessFlags)((flags, fn) => fn(flags))
          val newName = if (field.isPrivate) newFieldNameGen(cf.fqn, field.name) else field.name
          field.copy(accessFlags = newAccessFlags, name = newName)
        }

        val modifiedFields = cf.fields.zip(updatedFields).exists {
          case (oldF, newF) => oldF.name != newF.name
        }

        val updatedMethods: MethodTemplates =
          if (modifiedFields) privateRewrite(cf, cf.fields)
          else cf.methods.map(_.copy())
        (cf.copy(fields = updatedFields, methods = updatedMethods), url)
      }
    }

    val outputDir = Paths.get("artifacts").resolve("opal-temp")
    if (!Files.exists(outputDir)) {
      Files.createDirectories(outputDir)
    }
    cleanOutputDir(outputDir)
    val files = buildTempClassFiles(outputDir, updatedClassFiles)
    val modifiedP: Project[URL] = Project(
      projectFiles = files.map(x => x.toFile).toArray,
      libraryFiles = p.libraryClassFilesWithSources.map(x => new File(x._2.getFile)).toArray,
      logContext = GlobalLogContext,
      config = config
    )

    val modifiedFP: Project[URL] = extendProjectWithConfig(modifiedP, config, depsURL)
    LOGGER.info("Completed CFA Optimization")

    (modifiedP, modifiedFP)
  }
}
