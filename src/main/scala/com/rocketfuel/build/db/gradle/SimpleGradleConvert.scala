package com.rocketfuel.build.db.gradle

import java.nio.file._

import com.rocketfuel.build.Logger
import com.rocketfuel.build.db.gradle.GradleConvert.loadResource
import com.rocketfuel.build.db.mvn._
import com.rocketfuel.build.db.mool.Bld
import com.rocketfuel.sdbc.PostgreSql._

// Filter a very small subset of project to test IDE integration quickly
class SmallProjectFilter(modulePaths: Map[Int, String]) {

  private val quasarDeps =
    """3rd_party-java-com-google-protobuf-ZeroCopyByteString
      |3rd_party-java-com-googlecode-protobuf-pro-duplex-DuplexLogProto
      |3rd_party-java-com-googlecode-protobuf-pro-duplex-DuplexProtobufAll
      |3rd_party-java-com-twitter-elephantbird-hive-serde-Inspector
      |3rd_party-java-mvn-ch-qos-logback-LogbackAll
      |3rd_party-java-mvn-com-fasterxml-FasterxmlJacksonAll
      |3rd_party-java-mvn-com-google-guava-GuavaTestLibAll
      |3rd_party-java-mvn-org-apache-curator-CuratorAll
      |3rd_party-java-mvn-org-apache-curator-CuratorTestAll
      |3rd_party-java-mvn-org-apache-hadoop-HadoopDfsMiniClusterAll2
      |3rd_party-java-mvn-org-apache-hadoop-HadoopRunTimeDeps2
      |3rd_party-java-mvn-org-apache-httpcomponents-HttpAll
      |3rd_party-java-mvn-org-codehaus-jackson-JacksonAll
      |3rd_party-java-mvn-org-eclipse-jetty-JettyAll
      |3rd_party-java-mvn-org-json4s-Json4sAll
      |3rd_party-java-mvn-redis-clients-JedisAll
      |common-rpcutils-DuplexProtocolJavaProto
      |common-rpcutils-EmptyJavaProto
      |common-rpcutils-RpcTestJavaProto
      |dp-luke-LookupJavaProtos
      |dp-luke-PageJavaProtos
      |ei-common-ClassUtil
      |ei-common-ClassUtilTest
      |ei-common-Cache
      |ei-common-DummyRecord
      |ei-common-RpcClient
      |ei-common-RpcServer
      |ei-common-YamlUtil
      |ei-common-YamlUtilTest
      |ei-common-YamlUtilWithDeps
      |ei-common-resources-TestResources
      |grid-dmp-ssvadapter-utils-HdfsUtilsLib
      |3rd_party-java-mvn-org-json4s-Json4sAll2_11
      |grid-common-spark-SparkCatalyst2_0
      |grid-common-spark-SparkCore2_0
    """.stripMargin.split("\n").toSet

  def filterProject(path: String): Boolean = {
    if (path.startsWith("server") ||
      path.startsWith("camus") ||
      path.startsWith("common-message") ||
      path.startsWith("grid-common") ||
      path.startsWith("grid-externalreport") ||
      path.startsWith("grid-keychainsegjournal") ||
      path.startsWith("grid-lookup") ||
      path.startsWith("grid-luke") ||
      path.startsWith("grid-mestor") ||
      path.startsWith("grid-metricsdb") ||
      path.startsWith("grid-onlinestore") ||
      path.startsWith("grid-quasar") ||
      path.startsWith("grid-reportplus") ||
      path.startsWith("grid-retention") ||
      path.startsWith("grid-site_mv") ||
      path.startsWith("grid-scrubplus") ||
      path.startsWith("grid-viewability") ||
      path.startsWith("grid-common-spark-Spark") ||
      path.startsWith("mobile") ||
      path.startsWith("modeling-behavioral") ||
      path.startsWith("modeling-bt") ||
      path.startsWith("modeling-common") ||
      path.startsWith("modeling-dependency") ||
      path.startsWith("modeling-hiveaccess") ||
      path.startsWith("modeling-utils") ||

      path == "grid-scrubplus-logformat-generated-hive_proto-EvfColumnsProto" ||
      path == "3rd_party-java-mvn-org-apache-hadoop-HadoopAll2" ||
      quasarDeps.contains(path))
      true
    else
      false
  }

}

object SimpleGradleConvert extends Logger {

  def files(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val copies = GradleCopy.all.vector().map(GradleCopy.toCopy(_)).toSet
    Copy.copy(copies, moolRoot, destinationRoot)
  }

  def builds(moolRoot: Path, destinationRoot: Path)(implicit connection: Connection): Unit = {
    val projectsRoot = destinationRoot.resolve("projects")

    val identifiers = {
      for (i <- Identifier.list.iterator()) yield {
        i.bldId -> i
      }
    }.toMap

    val dependencies =
      com.rocketfuel.build.db.mvn.Dependency.list.vector().groupBy(_.sourceId)
    val publications = {
      for (i <- Publications.all.iterator()) yield {
        i.id -> i
      }
    }.toMap

    val localBlds = Bld.locals.vector()
    val modulePaths = localBlds.foldLeft(Map.empty[Int,String]) { case (m, bld) =>
        m + (bld.id -> Projects.pathToModulePath(bld.path))
    }
    val moduleBlds = localBlds.groupBy { bld => Projects.pathToModulePath(bld.path) }

    var includedBuilds = List[(String, Seq[String])]()
    val moduleOutputs = localBlds.foldLeft(Map.empty[String, Int]) { case (moduleOuts, bld) =>
      val identifier = identifiers(bld.id)
      val output = s"'${identifier.groupId}:${identifier.artifactId}:${identifier.version}'"
      val outputTest = s"'${identifier.groupId}:${identifier.artifactId}:${identifier.version}:tests'"
      moduleOuts + (output -> bld.id) + (outputTest -> bld.id)
    }
    val prjFilter = new SmallProjectFilter(modulePaths)
    val convertor = new GradleConvert(projectsRoot, modulePaths, publications, moduleOutputs)
    // for ((path, blds) <- moduleBlds) {
    for ((path, blds) <- moduleBlds.filter { case (path, bld) => prjFilter.filterProject(path) }) {
      val bldsWithDeps = blds
        .map { bld => (bld, dependencies.getOrElse(bld.id, Vector.empty))}
        .toMap

      includedBuilds = (path, blds.map(_.path.mkString("-"))) :: includedBuilds
      val modulePath = projectsRoot.resolve(path)
      val gradle = convertor.gradle(path, bldsWithDeps, modulePath)
      val gradlePath = modulePath.resolve("build.gradle")

      Files.createDirectories(modulePath)
      Files.write(gradlePath, gradle.getBytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    val settingsGradle = destinationRoot.resolve("settings.gradle")
    val settings = includedBuilds.sortBy {_._1}.foldLeft("") { (buffer, prjNames) =>
      val comment = if (prjNames._1 == prjNames._2) "" else s" // ${prjNames._2}"
      buffer + s"include ':${prjNames._1}'$comment\n"
    }

    Files.write(settingsGradle,
      (settings + loadResource("settings_end.gradle")).getBytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE)

  }
}
