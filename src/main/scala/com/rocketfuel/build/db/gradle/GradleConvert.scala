package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.mool.Bld
import com.rocketfuel.build.db.mvn.{Dependency => MvnDependency}
import com.rocketfuel.build.db.mvn._
import com.rocketfuel.sdbc.PostgreSql._

case class BuildGradleParts(compileDeps: Set[GrDependency] = Set.empty,
                            plugins: Set[String] = Set("plugin: 'java'"),
                            snippets: Set[String] = Set.empty,
                            scalaVersions: Set[String] = Set.empty)

class GradleConvert(projectRoot: Path, modulePaths: Map[Int, String], moduleOutputs: Map[String, Int]) extends Logger {
  // not s""": leave interpolation to Groovy/Gradle
  private val protoConfigSnippet =
    """
      |protobuf {
      |  protoc {
      |    path = "${System.env.HOME}/.mooltool/packages/protobuf/bin/protoc"
      |  }
      |}
      |task sourcesJar(type: Jar, dependsOn: classes) {
      |    classifier = 'sources'
      |    from sourceSets.main.allSource
      |    from sourceSets.main.proto
      |    from "${protobuf.generatedFilesBaseDir}/main/java"
      |}
      |idea {
      |    module {
      |        sourceDirs += file("${protobuf.generatedFilesBaseDir}/main/java");
      |    }
      |}
      |""".stripMargin
  private val thriftConfigSnippet =
    """
      |compileThrift {
      |  thriftExecutable "${System.env.HOME}/.mooltool/packages/thrift-0.9.1/bin/thrift"
      |}
      |""".stripMargin
  private val testNGConfigSnippetWithGroupsPre =
    """
      |test {
      |  useTestNG() {
      |    environment 'BUILD_ROOT', "${rootProject.projectDir}/projects/testdata"
      |    includeGroups """.stripMargin
  private val testNGConfigSnippetWithGroupsPost =
    """
      |  }
      |}
      |""".stripMargin

  def testNGConfig(testGroups: Option[String]): String = {
    testNGConfigSnippetWithGroupsPre +
      testGroups.getOrElse("unit").split(",").map {
        "'" + _ + "'"
      }.mkString(", ") +
      testNGConfigSnippetWithGroupsPost
  }

  private val shadowJarSnippet =
    """shadowJar {
      |  manifest {
      |    attributes 'Main-Class': '__MAIN_CLASS__'
      |  }
      |}
      |""".stripMargin

  def shadowJarConfig(mainClass: Option[String]): Option[String] =
    mainClass.map(mClz => shadowJarSnippet.replace("__MAIN_CLASS__", mClz))

  private val scala210Libs = List(GrDependency(dep = "'org.scala-lang:scala-library:2.10.4'"),
    GrDependency(dep = "'org.scala-lang:scala-actors:2.10.4'")
  )
  private val scala211Libs = List(GrDependency(dep = "'org.scala-lang:scala-library:2.11.8'"))
  // use 1.4.2 with scalatest 3, now stick to 1.1
  private val scalatestLibs = GrDependency(configuration = "testRuntime", dep = "'org.pegdown:pegdown:1.1.0'")
  private val scalatestSnippet =
    """
      |test {
      |    maxParallelForks = 1
      |    environment 'BUILD_ROOT', "${rootProject.projectDir}/projects/testdata"
      |}
    """.stripMargin
  private val scala210Tasks = "rootProject.tasks.build210.dependsOn tasks.build\n"
  private val scala211Tasks = "rootProject.tasks.build211.dependsOn tasks.build\n"

  private val protoLib = GrDependency(dep = "files(\"${System.env.HOME}/.mooltool/packages/protobuf/java/target/protobuf-2.5.0.jar\")")
  private val thriftLib = GrDependency(dep = "'org.apache.thrift:libthrift:0.9.1'")

  def sourceCompatibility(javaVersion: Option[String]): String =
    "sourceCompatibility = " + javaVersion.getOrElse("1.7")

  private def gradleForBld(path: String, prjBld: Bld, dependencies: Vector[MvnDependency], moduleRoot: Path): BuildGradleParts = {
    def dependencyList(isTest: Boolean) = dependencies.foldLeft(List[GrDependency]()) { case (depList, dep) =>
      moduleOutputs.get(dep.gradleDefinition).flatMap(modulePaths.get(_)) match {
        case Some(depPath) =>
          val configuration = dep.scope match {
            case "provided" => "compileOnly"
            case "test" => "testCompile"
            case _ => if (isTest) "testCompile" else "compile"
          }
          val projectOutputs = dep.`type` match {
            case Some("test-jar") => List(
              s"project(':${depPath}')",
              s"project(path: ':${depPath}', configuration: 'tests')")
            case _ => List(
              s"project(':${depPath}')")
          }
          depList ++ projectOutputs.map(output => GrDependency(configuration = configuration, dep = s"${output}"))
        case _ =>
          depList ++ List(GrDependency(dep)).map { d =>
            val newConfiguration = if (isTest && d.configuration == "compile") "testCompile"
            else d.configuration
            GrDependency(configuration = newConfiguration, dep = d.dep)
          }
      }
    }.filterNot { testDep =>
      if (path == "grid-onlinestore" && testDep.dep.contains("grid-onlinestore-model-dmp")) {
        logger.info(s"unwanted dependency ${prjBld}, ${testDep}")
      }
      if (testDep.dep.contains("':" + path + "'")) {
        logger.trace(s"eliminate test dependency on main source in ${path}")
      }
      testDep.dep.contains("':" + path + "'")
    }.toSet

    val buildGradleParts = {
      prjBld.ruleType match {
        case "java_proto_lib" =>
          BuildGradleParts(compileDeps = Set(protoLib) ++ dependencyList(false),
            plugins = Set("plugin: 'java'", "plugin: 'com.google.protobuf'"),
            snippets = Set(protoConfigSnippet))
        case "java_lib" | "file_coll" =>
          BuildGradleParts(compileDeps = dependencyList(false),
            plugins = Set("plugin: 'java'"),
            snippets = Set(sourceCompatibility(prjBld.javaVersion)))
        case "java_bin" =>
          BuildGradleParts(compileDeps = dependencyList(false),
            plugins = Set("plugin: 'java'", "plugin: 'com.github.johnrengelman.shadow'"),
            snippets = shadowJarConfig(prjBld.mainClass).toSet + sourceCompatibility(prjBld.javaVersion))
        case "java_test" =>
          // 'from: "${' will be interpolated by Gradle
          BuildGradleParts(plugins = Set("plugin: 'java'", "from: \"${rootProject.projectDir}/gradle/tests.gradle\""),
            snippets = Set(testNGConfig(prjBld.testGroups)) + sourceCompatibility(prjBld.javaVersion),
            compileDeps = dependencyList(true))
        case "java_thrift_lib" =>
          BuildGradleParts(plugins = Set("plugin: 'java'", "plugin: 'org.jruyi.thrift'"),
            snippets = Set(thriftConfigSnippet) + sourceCompatibility(prjBld.javaVersion),
            compileDeps = Set(thriftLib) ++ dependencyList(false))
        case "scala_lib" =>
          val compileDeps = prjBld.scalaVersion match {
            case Some("2.10") => scala210Libs.toSet ++ dependencyList(false)
            case Some("2.11") => scala211Libs.toSet ++ dependencyList(false)
            case Some("2.12") => dependencyList(false) // TODO should have 2.12 libs
            case _ =>
              logger.warn(s"scala_lib with unknown version ${prjBld}")
              dependencyList(false)
          }
          BuildGradleParts(compileDeps = compileDeps,
            plugins = Set("plugin: 'scala'"),
            snippets = Set(sourceCompatibility(prjBld.javaVersion)),
            scalaVersions = prjBld.scalaVersion.toSet)
        case "scala_test" =>
          val compileDeps = prjBld.scalaVersion match {
            case Some("2.10") => scala210Libs.toSet ++ dependencyList(true) + scalatestLibs
            case Some("2.11") => scala211Libs.toSet ++ dependencyList(true) + scalatestLibs
            case Some("2.12") => dependencyList(true) + scalatestLibs // TODO should have 2.12 libs
            case _ =>
              logger.warn(s"scala_test with unknown version ${prjBld}")
              dependencyList(true).toSet + scalatestLibs
          }
          BuildGradleParts(compileDeps = compileDeps,
            plugins = Set("plugin: 'scala'", "plugin: 'com.github.maiflai.scalatest'"),
            snippets = Set(sourceCompatibility(prjBld.javaVersion), scalatestSnippet),
            scalaVersions = prjBld.scalaVersion.toSet)
        case "scala_bin" =>
          val compileDeps = prjBld.scalaVersion match {
            case Some("2.10") => scala210Libs.toSet ++ dependencyList(false)
            case Some("2.11") => scala211Libs.toSet ++ dependencyList(false)
            case Some("2.12") => dependencyList(false) // TODO should have 2.12 libs
            case _ =>
              logger.warn(s"scala_lib with unknown version ${prjBld}")
              dependencyList(false)
          }
          BuildGradleParts(compileDeps = compileDeps,
            plugins = Set("plugin: 'scala'", "plugin: 'com.github.johnrengelman.shadow'"),
            snippets = shadowJarConfig(prjBld.mainClass).toSet + sourceCompatibility(prjBld.javaVersion),
            scalaVersions = prjBld.scalaVersion.toSet)
        case _ =>
          BuildGradleParts(plugins = Set("plugin: 'base'"))
      }
    }
    buildGradleParts
  }

  def gradle(path: String, bldWithDeps: Map[Bld, Vector[MvnDependency]],
             moduleRoot: Path) = {
    val buildGradleParts = bldWithDeps.map { case (bld, deps) => gradleForBld(path, bld, deps, moduleRoot) }
      .reduceLeft { (buildParts1, buildParts2) =>
        val mergedDeps = buildParts1.compileDeps ++ buildParts2.compileDeps
        val mergedScalaVersions = buildParts1.scalaVersions ++ buildParts2.scalaVersions ++
          mergedDeps.map { d =>
            if (d.dep.contains("org.scala-lang:scala-library:2.10")) Some("2.10")
            else if (d.dep.contains("org.scala-lang:scala-library:2.11")) Some("2.11")
            else None
          }.flatten ++
          // TODO following is wrong
          (if (path == "grid-reportplus-writers") Set("2.10", "2.11") else Set.empty)
        val mergedDeps2 = mergedDeps.map { d =>
          if (mergedScalaVersions.size > 1) d.toScalaMultiVersion
          else d
        }
        val multiVersionPlugin =
          if (mergedScalaVersions.size > 1) Some("plugin: 'com.adtran.scala-multiversion-plugin'")
          else None
        BuildGradleParts(
          compileDeps = mergedDeps2,
          snippets = buildParts1.snippets ++ buildParts2.snippets,
          plugins = buildParts1.plugins ++ buildParts2.plugins ++ multiVersionPlugin,
          scalaVersions = mergedScalaVersions
        )
    }

    val buildGradleText =
      buildGradleParts.plugins.map(p => s"apply ${p}").mkString("\n") + "\n\n" +
        buildGradleParts.snippets.mkString("\n") +
        """
          |
          |dependencies {
          |""".stripMargin +
        buildGradleParts.compileDeps.map(_.text).toSeq.sorted.mkString("\n") +
        "\n}\n"
    buildGradleText
  }

}

object GradleConvert extends Logger {

  def loadResource(path: String): String = {
    val source = io.Source.fromInputStream(getClass.getResourceAsStream(path))
    try source.mkString
    finally source.close()
  }
}
