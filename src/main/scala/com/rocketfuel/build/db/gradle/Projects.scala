package com.rocketfuel.build.db.gradle

case class BldGrouping(sharedPrefix: String,
                       excludes: Set[String] = Set.empty,
                       gradleProjectName: Option[String] = None)

object Projects {

  private val bldGroupings = Seq(
    BldGrouping(sharedPrefix = "brand-veenome"),
    BldGrouping(sharedPrefix = "camus-api"),
    BldGrouping(sharedPrefix = "camus-coders"),
    BldGrouping(sharedPrefix = "camus-etl-mapred-support"),
    BldGrouping(sharedPrefix = "camus-etl"),
    BldGrouping(sharedPrefix = "camus-schemaregistry"),
//    BldGrouping(sharedPrefix = "camus"),
    // merge all common protobufs
    // temporarily split aerospike_data_message & page_context
    BldGrouping(sharedPrefix = "common-message", excludes = Set("common-message-protobuf-AerospikeDataMessageProto")),
    BldGrouping(sharedPrefix = "grid-scrubplus-logformat-generated-hive_proto-EvfColumnsProto",
      gradleProjectName = Some("common-message")),

    BldGrouping(sharedPrefix = "cc"),
    BldGrouping(sharedPrefix = "common-releaseutils"),
    BldGrouping(sharedPrefix = "common-repotools"),
    BldGrouping(sharedPrefix = "common-tools-datacomp"),
    BldGrouping(sharedPrefix = "ei-common-Cache"),
    BldGrouping(sharedPrefix = "ei-common-LruCacheTest", gradleProjectName = Some("ei-common-Cache")),
    BldGrouping(sharedPrefix = "ei-common-RpcClient"),
    BldGrouping(sharedPrefix = "ei-common-RpcServer"),
    BldGrouping(sharedPrefix = "ei-common-YamlUtil"), // to add ...WithDeps
    BldGrouping(sharedPrefix = "ei-datamon-alert"),

    BldGrouping(sharedPrefix = "grid-common-GridCommon"),
    BldGrouping(sharedPrefix = "grid-common-hive-bucketing"),
    // BldGrouping(sharedPrefix = "grid-common-hive-utils"),
    BldGrouping(sharedPrefix = "grid-common-hive"),
    BldGrouping(sharedPrefix = "grid-common-io"),
    BldGrouping(sharedPrefix = "grid-common-mapreduce-job-metrics-config-BulkloadJobConfig"),
    BldGrouping(sharedPrefix = "grid-common-mapreduce-job-metrics-JobMetricUtils", gradleProjectName = Some("grid-common-mapreduce-job-metrics")),
    BldGrouping(sharedPrefix = "grid-common-mapreduce-job-metrics-JobMetricsUtilsTest", gradleProjectName = Some("grid-common-mapreduce-job-metrics")),
    BldGrouping(sharedPrefix = "grid-common-mapreduce-job-metrics"),
    BldGrouping(sharedPrefix = "modeling-hiveaccess-OrcStructFieldRetriever", gradleProjectName = Some("grid-common-hive")),
    BldGrouping(sharedPrefix = "modeling-utils-hive-HiveUtils", gradleProjectName = Some("grid-common-hive")),
    BldGrouping(sharedPrefix = "grid-externalreport"),
    BldGrouping(sharedPrefix = "grid-keychainsegjournal"),
    BldGrouping(sharedPrefix = "grid-lookup-dim-config"),
    BldGrouping(sharedPrefix = "grid-lookup-dim-ds"),
    BldGrouping(sharedPrefix = "grid-lookup-dim-testconfig"),
    BldGrouping(sharedPrefix = "grid-lookup-dim-DimLib"),
    BldGrouping(sharedPrefix = "grid-lookup-dim"),
    BldGrouping(sharedPrefix = "grid-lookup-metrics"),
    BldGrouping(sharedPrefix = "grid-lookup-scripts"),
    BldGrouping(sharedPrefix = "grid-lookup-service-DimConstants"),
    BldGrouping(sharedPrefix = "grid-lookup-service"),
    BldGrouping(sharedPrefix = "grid-lookup-service-dim"),
    BldGrouping(sharedPrefix = "grid-lookup-support"),
    BldGrouping(sharedPrefix = "grid-lookup-GridLookupOld"),
    BldGrouping(sharedPrefix = "grid-lookup-GridLookup"),

    BldGrouping(sharedPrefix = "grid-luke-scripts"), // TODO probably need to sort out manually
    BldGrouping(sharedPrefix = "grid-luke-service-api"),
    BldGrouping(sharedPrefix = "grid-luke-service-cli"),
    BldGrouping(sharedPrefix = "grid-luke-service-client"),
    BldGrouping(sharedPrefix = "grid-luke-service-core-common-ByteSerDe", gradleProjectName = Some("grid-luke-service-core-common-simple")),
    BldGrouping(sharedPrefix = "grid-luke-service-core-common-ControlValue", gradleProjectName = Some("grid-luke-service-core-common-simple")),
    BldGrouping(sharedPrefix = "grid-luke-service-core-common-SizeCalculator", gradleProjectName = Some("grid-luke-service-core-common-simple")),
    BldGrouping(sharedPrefix = "grid-luke-service-core-common-Stringifier", gradleProjectName = Some("grid-luke-service-core-common-simple")),
    BldGrouping(sharedPrefix = "grid-luke-service-core-common"),// TODO cut {prod,user}_conf
    BldGrouping(sharedPrefix = "grid-luke-service-metrics"),
    BldGrouping(sharedPrefix = "grid-luke-service-discovery", gradleProjectName = Some("grid-luke-service-core-common")),
    BldGrouping(sharedPrefix = "grid-luke-squeeze-common"),
    BldGrouping(sharedPrefix = "grid-luke-squeeze-handler"),
    BldGrouping(sharedPrefix = "grid-luke-squeeze-javabeanserde", gradleProjectName = Some("grid-luke-squeeze-common")),
    BldGrouping(sharedPrefix = "grid-luke-squeeze-squeezer", gradleProjectName = Some("grid-luke-squeeze-handler")),
    BldGrouping(sharedPrefix = "grid-luke-utils", excludes = Set("grid-luke-utils-EmbeddedRedis")),

    BldGrouping(sharedPrefix = "grid-mestor"),
    BldGrouping(sharedPrefix = "grid-metricsdb"),
    BldGrouping(sharedPrefix = "grid-onlinestore-utils-cleanup"),
    BldGrouping(sharedPrefix = "grid-onlinestore-utils-norm"),
    BldGrouping(sharedPrefix = "grid-onlinestore-model-dmp"),
    BldGrouping(sharedPrefix = "grid-onlinestore-model-protobuf-TestProtobuf", gradleProjectName = Some("grid-onlinestore-model-dmp")),
    BldGrouping(sharedPrefix = "grid-onlinestore", excludes = Set(
      "grid-onlinestore-OnlineStore",
      "grid-onlinestore-OnlineJobs",
      "grid-onlinestore-OfflineJobs"
    )),
    BldGrouping(sharedPrefix = "grid-onlinestore", gradleProjectName = Some("grid-onlinestore-all")),

    BldGrouping(sharedPrefix = "grid-quasar"),
    BldGrouping(sharedPrefix = "grid-reportplus-writers"),
    BldGrouping(sharedPrefix = "grid-reportplus"),
    BldGrouping(sharedPrefix = "grid-retention"),
    BldGrouping(sharedPrefix = "grid-site_mv"),
    BldGrouping(sharedPrefix = "grid-scrubplus-logformat-generated-pojo-GeneratedPojoLib"),
    BldGrouping(sharedPrefix = "grid-scrubplus-logformat-generated-proto_pojo-GeneratedProtoPojoLib"),
    BldGrouping(sharedPrefix = "grid-scrubplus"),
    BldGrouping(sharedPrefix = "grid-viewability"),

    BldGrouping(sharedPrefix = "mobile-common"),
    BldGrouping(sharedPrefix = "mobile-geo"),
    BldGrouping(sharedPrefix = "mobile-pipelines"),// probably deserves split
    BldGrouping(sharedPrefix = "mobile-tools"),
    BldGrouping(sharedPrefix = "modeling-common-Dependency", gradleProjectName = Some("modeling-dependency")),
    // these are Java tests built & run against Scala 2.10 and 2.11,
    // can be merged into modeling-common-Scala but then only one of Java and Scala tests are run
    BldGrouping(sharedPrefix = "modeling-common-JavaTest", gradleProjectName = Some("modeling-common-JavaTest")),
    BldGrouping(sharedPrefix = "modeling-common-Scala"),
    BldGrouping(sharedPrefix = "modeling-common-ModelingCommon"),
    BldGrouping(sharedPrefix = "modeling-common"),
    BldGrouping(sharedPrefix = "modeling-behavioral", gradleProjectName = Some("modeling-common")),
    BldGrouping(sharedPrefix = "modeling-bt"),
    BldGrouping(sharedPrefix = "modeling-dependency"),
    BldGrouping(sharedPrefix = "modeling-utils"), // modeling-utils-hive is moved to grid-common-hive

    // create one project for server.util, the only external dependency is server.geoip.TimeZone
    BldGrouping(sharedPrefix = "server-util"),
    BldGrouping(sharedPrefix = "server-geoip-TimeZone", gradleProjectName = Some("server-util")),

    BldGrouping(sharedPrefix = "server-geoip"),
    BldGrouping(sharedPrefix = "server-rfi"),
    // merge these python packages together
    BldGrouping(sharedPrefix = "server-tools-load_anon_inventory"),
    BldGrouping(sharedPrefix = "server-tools-load_bid_notification")

    // BldGrouping(sharedPrefix = "server", gradleProjectName = Some("server-ServerProtoAll")) // too many duplicates in protobufs
  )

  def pathToModulePath(path: Seq[String]): String = {
    val patchedPath = if (path.head == "grid") "grid2" +: path.drop(1)
    else if (path.take(3) == Seq("java", "com", "rocketfuel")) path.drop(3)
    else if (path.take(3) == Seq("clojure", "com", "rocketfuel")) "clojure" +: path.drop(3)
    else if (path.head == "java") "3rd_party" +: path
    else path

    val defaultPath = patchedPath.mkString("-")
    val remappedPath = bldGroupings.find { bldGroup =>
      defaultPath.startsWith(bldGroup.sharedPrefix) && !bldGroup.excludes.contains(defaultPath)
    }.map { bldGroup => bldGroup.gradleProjectName.getOrElse(bldGroup.sharedPrefix)
    }.getOrElse(defaultPath)

    stripSuffices(remappedPath)
  }

  private def stripSuffices(s: String) : String = {
    val short = s.stripSuffix("EvaluatorTest")
      .stripSuffix("Test")
      .stripSuffix("Tests")
      .stripSuffix("Pkg")
      .stripSuffix("NoDeps")
      .stripSuffix("NoConf")
      // .stripSuffix("Lib")
      .stripSuffix("_bin")
      .stripSuffix("_test")
      .stripSuffix("_lib")
    if (s.endsWith("-ModulesTest") || // circular dep m/a/core/modules/Modules m/a/core/workflow/WorkFlow
      short.isEmpty) s
    else short
  }
}
