package com.rocketfuel.build.db.gradle

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class Publications(id: Int,
                        path: Seq[String],
                        groupId: String,
                        artifactId: String,
                        baseVersion: String)

object Publications extends Deployable {
  val all =
    Select[Publications]("SELECT id, path, group_id AS groupId, artifact_id AS artifactId, base_version AS baseVersion FROM gradle.publications")

  val deployQuery = Ignore.readClassResource(Publications.getClass, "publications.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS gradle.publications CASCADE")

}