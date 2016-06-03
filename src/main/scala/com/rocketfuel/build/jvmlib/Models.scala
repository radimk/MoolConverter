package com.rocketfuel.build.jvmlib

import com.rocketfuel.build.mool
import java.nio.file.Path

case class Models(
  models: Map[mool.MoolPath, Model],
  moolModel: mool.Model,
  moolRoot: Path
) {

  def copies(destinationRoot: Path): Map[Path, Path] = {
    for {
      (relCfgPath, model) <- models
      relCfg = moolModel.relCfgs(relCfgPath)
      bldPath <- relCfg.`jar-with-dependencies`.toTraversable.map(_.targetPath)
      bld = moolModel.blds(bldPath)
      srcPath = destinationRoot.resolve(relCfgPath.last).resolve("src")
      (configurationName, configuration) <- model.configurations
      file <- configuration.files
    } yield {

      val relative = moolRoot.relativize(file)
      val relativeWithoutJava = relative.subpath(1, relative.getNameCount)

      val srcLanguage =
        bld.rule_type match {
          case "file_coll" =>
            "resources"
          case "java_proto_lib" =>
            "proto"
          case "java_lib" =>
            "java"
          case "scala_lib" =>
            "scala"
        }

      val destinationFile =
        srcPath.resolve(configurationName).
          resolve(srcLanguage).
          resolve(relativeWithoutJava)

      (file, destinationFile)
    }
  }
}

object Models {
  def ofMoolRepository(moolRoot: Path): Models = {
    val moolModel = mool.Model.ofRepository(moolRoot)
    val models = Model.ofMoolRelCfgs(moolModel)
    Models(
      models = models,
      moolModel = moolModel,
      moolRoot = moolRoot
    )
  }
}