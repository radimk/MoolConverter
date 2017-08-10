package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.db.mvn.Dependency

case class GrDependency(configuration: String = "compile", dep: String) {
  def toScalaMultiVersion: GrDependency = {
    val newDep = dep
      .replace(":2.10.3", ":%scala-version%")
      .replace(":2.10.4", ":%scala-version%")
      .replace(":2.11.8", ":%scala-version%")
      .replaceFirst("_2.1[012]:", "_%%:")
    val newDep2 = if (newDep == dep) dep
    else newDep.replace('\'', '"')
    GrDependency(configuration = this.configuration, dep = newDep2)
  }

  def text: String = s"  $configuration $dep"
}

object GrDependency {
  def apply(dependency: Dependency): GrDependency = {
    val configuration = dependency.scope match {
      case "provided" => "compileOnly"
      case "test" => "testCompile"
      case _ => "compile"
    }
    new GrDependency(configuration = configuration,
      dep = s"${dependency.gradleDefinition}")
  }
}