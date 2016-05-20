package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import com.rocketfuel.mool.RelCfg
import java.nio.file.Path

case class Model(
  groupId: String,
  artifactId: String,
  version: String,
  repository: Option[String],
  scalaVersion: Option[String],
  dependencies: Vector[Model.Dependency],
  files: Set[Path]
)

object Model {

  implicit class PathParts(pathString: String) {
    private lazy val splitPathString =
      pathString.split('.').toVector

    def pathParts: Vector[String] =
      splitPathString.drop(1).dropRight(1)

    def pathName: String =
      splitPathString.last
  }

  sealed trait Dependency

  object Dependency {
    case class Local(
      path: Vector[String],
      name: String
    ) extends Dependency

    case class Remote(
      groupId: String,
      artifactId: String,
      version: String
    ) extends Dependency
  }

  def ofMoolBld(
    moolModel: mool.Model
  )(path: Vector[String],
    name: String,
    bld: mool.Bld
  ): Option[Model] = {
    val dependencies =
      dependenciesOfBld(moolModel)(path, bld)

    val sourcePaths =
      sourcePathsOfBld(moolModel)(path, bld)

    for {
      pathRelCfgs <- moolModel.relCfgs.get(path)
      relCfg <- pathRelCfgs.get(name)
    } yield {
      Model(
        groupId = relCfg.group_id,
        artifactId = relCfg.artifact_id,
        version = relCfg.base_version,
        scalaVersion = bld.scala_version,
        dependencies = dependencies,
        repository = bld.maven_specs.map(_.repo_url),
        files = sourcePaths
      )
    }
  }

  /**
    * Create a Model for each BLD.
    *
    * @param model
    * @return
    */
  def ofMoolBlds(model: mool.Model): Iterable[Model] = {
    for {
      (path, blds) <- model.blds
      (name, bld) <- blds
      model <- ofMoolBld(model)(path, name, bld)
    } yield model
  }

  def ofMoolRelCfg(
    moolModel: mool.Model
  )(path: Vector[String],
    name: String,
    relCfg: RelCfg
  ): Option[Model] = {
    for {
      withDeps <- relCfg.`jar-with-dependencies`
      targetBldParts = withDeps.target.split('.').toVector
      if targetBldParts.startsWith(Vector("mool", "java"))
    } yield {

      val targetBldPath = targetBldParts.drop(1).dropRight(1)
      val withDepsName = targetBldParts.last
      val blds = moolModel.blds(targetBldPath)
      val bld = blds(withDepsName)

      val dependencies =
        dependenciesOfBld(moolModel)(path, bld)

      val sourcePaths =
        sourcePathsOfBld(moolModel)(path, bld)

      Model(
        groupId = relCfg.group_id,
        artifactId = relCfg.artifact_id,
        version = relCfg.base_version,
        scalaVersion = bld.scala_version,
        dependencies = dependencies,
        repository = bld.maven_specs.map(_.repo_url),
        files = sourcePaths
      )
    }
  }

  /**
    * Create a Model for each RelCfg.
    *
    * @param model
    * @return
    */
  def ofMoolRelCfgs(model: mool.Model): Iterable[Model] = {
    for {
      (path, relCfgs) <- model.relCfgs
      (name, relCfg) <- relCfgs
      model <- ofMoolRelCfg(model)(path, name, relCfg).toVector
    } yield model
  }

  def dependenciesOfBld(moolModel: mool.Model)(path: Vector[String], bld: mool.Bld): Vector[Dependency] = {
    for {
      deps <- bld.deps.toVector
      dep <- deps
    } yield {
      val depParts =
        if (dep.startsWith(".")) {
          //A dependency starting with '.' is relative.
          //Drop the leading '.' and append the name to the path.
          path :+ dep.drop(1)
        }
        else {
          //The dependency path is absolute.
          //drop the leading "mool"
          dep.split('.').drop(1).toVector
        }
      val depPath = depParts.dropRight(1)
      val depName = depParts.last
      val depPathBlds = moolModel.blds(depPath)
      val depBld = depPathBlds(depName)


      depBld.maven_specs match {
        case Some(mavenSpecs) =>
          Dependency.Remote(
            groupId = mavenSpecs.group_id,
            artifactId = mavenSpecs.artifact_id,
            version = mavenSpecs.version
          )
        case None =>
          Dependency.Local(
            path = depPath,
            name = depName
          )
      }
    }
  }

  def sourcePathsOfBld(moolModel: mool.Model)(path: Vector[String], bld: mool.Bld): Set[Path] = {
    val paths =
      for {
        sources <- bld.srcs.toVector
        source <- sources
      } yield {
        val sourceRelativePath = path :+ source
        moolModel.root.resolve(sourceRelativePath.mkString("/"))
      }

    paths.toSet
  }

  /**
    * Get the source dependencies of a relcfg. Look in all its dependencies transitively, up to the point where a
    * dependency is required by a different relcfg.
    *
    * @param moolModel
    * @param path
    * @param bld
    * @return
    */
  def transitiveSourcePathsOfBld(moolModel: mool.Model)(path: Vector[String], name: String, bld: mool.Bld): Set[Path] = {
    for {
      dependency <- bld.deps.toVector.flatten
    } yield {
      val dependencyPath = dependency.pathParts
      val dependencyName = dependency.pathName
      if (moolModel.relCfgs.contains(dependencyPath) && moolModel.relCfgs(dependencyPath).contains(dependencyName)) Set.empty
      else
    }
  }

}
