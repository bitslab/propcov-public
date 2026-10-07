package edu.uic.bitslab.propcov.extensions.analysisframework.opal

import com.typesafe.config.{Config, ConfigRenderOptions, ConfigValueFactory}
import edu.uic.bitslab.propcov.core.config.AnalysisConfig.AnalysisType
import org.opalj.br.BaseConfig
import org.opalj.br.analyses.cg.{InitialEntryPointsKey, InitialInstantiatedTypesKey}

import java.util
import scala.annotation.unused
import scala.jdk.CollectionConverters._


object ConfigOps {

  def createConfigChange(declaringClass: String, methodName: String, paramTypes: util.List[String],
                         algo: AnalysisType, cfaOpt: Boolean, lamdaFlag: Boolean): Config = {
    val configRule = List(Map("declaringClass" -> declaringClass, "name" -> methodName).asJava).asJava
    val invokeDyn = if (lamdaFlag) false else BaseConfig.getBoolean("org.opalj.br.reader.ClassFileReader.Invokedynamic.rewrite")
    val modules = BaseConfig.getStringList("org.opalj.tac.cg.PointsTo.modules")

    algo match {
      case AnalysisType.CFA0 | AnalysisType.CFA10 if cfaOpt =>
        modules.add("org.opalj.tac.fpcf.analyses.pointsto.TypeBasedLibraryPointsToAnalysisScheduler")
      case AnalysisType.CFA1 | AnalysisType.CFA11 if cfaOpt =>
        modules.add("org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedLibraryPointsToAnalysisScheduler")
      case _ =>
    }
    BaseConfig
      .withValue("org.opalj.br.reader.ClassFileReader.Invokedynamic.rewrite", ConfigValueFactory.fromAnyRef(invokeDyn))
      .withValue("org.opalj.tac.cg.PointsTo.modules", ConfigValueFactory.fromIterable(modules))
      .withValue(InitialEntryPointsKey.ConfigKey, ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ConfigurationEntryPointsFinder"))
      .withValue(InitialEntryPointsKey.ConfigKeyPrefix + "entryPoints", ConfigValueFactory.fromIterable(configRule))
      .withValue(InitialInstantiatedTypesKey.ConfigKeyPrefix + "instantiatedTypes", ConfigValueFactory.fromIterable(paramTypes))
      .withFallback(BaseConfig).resolve()
  }

  /**
   * function to print to console a configuration given.
   *
   * @param conf typesafe config object that we want to print out
   */
  @unused
  private def printPrettyConfig(conf: Config): Unit = {
    val renderOptions = ConfigRenderOptions
      .defaults()
      .setOriginComments(false)
      .setComments(false)
      .setFormatted(true)

    println(conf.root().render(renderOptions))
  }

}
