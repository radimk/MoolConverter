package com.rocketfuel.build.db.gradle

/**
  * Enum for applied scripts / plugins
  */
object Applies {

  sealed abstract class Plugin(val order: Int, val kind: String,
                               val name: String) extends Ordered[Plugin] {

    def compare(that: Plugin) = this.order - that.order

    override def toString = if (kind == "plugin") s"apply plugin: '${name}'"
    else "apply from: \"${rootProject.projectDir}/gradle/" + name + ".gradle\""
  }

  case object BASE extends Plugin(1, "plugin", "base")
  case object JAVA extends Plugin(2, "plugin", "java")
  case object SCALA extends Plugin(3, "plugin", "scala")

  case object PROTOBUF extends Plugin(11, "plugin", "com.google.protobuf")
  case object THRIFT extends Plugin(12, "plugin", "org.jruyi.thrift")
  case object TESTS extends Plugin(13, "script", "tests")
  case object SCALA_MULTIVERSION extends Plugin(14, "plugin", "com.adtran.scala-multiversion-plugin")
  case object SCALATEST extends Plugin(15, "plugin", "com.github.maiflai.scalatest")

  case object CHECKS extends Plugin(21, "script", "checks")
  case object SCALACHECKS extends Plugin(22, "script", "scalachecks")

  case object DEPENDENCIES extends Plugin(31, "script", "dependencies")
  case object SHADOW extends Plugin(32, "plugin", "com.github.johnrengelman.shadow")
  case object MAVEN_PUBLISH extends Plugin(33, "plugin", "maven-publish")
  case object RELEASE extends Plugin(34, "script", "release")
}
