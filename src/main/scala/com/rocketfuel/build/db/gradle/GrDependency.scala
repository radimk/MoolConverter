package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.db.mvn.Dependency

case class GrDependency(configuration: String = "compile", dep: String) {

  def text: String = s"  $configuration $dep"
}

object GrDependency {
  def apply(dependency: Dependency): GrDependency = {
    val configuration = dependency.scope match {
      case "provided" => "compileOnly"
      case "test" => "testCompile"
      case _ => "compile"
    }
    val classifier = dependency.`type` match {
      case Some("test-jar") => ":tests"
      case _ => ""
    }
    new GrDependency(configuration = configuration,
      dep = s"'${dependency.gradleDefinition}${classifier}'")
  }
}