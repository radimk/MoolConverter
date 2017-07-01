package com.rocketfuel.build.db.mvn

import com.rocketfuel.build.db.Deployable
import com.rocketfuel.sdbc.PostgreSql._

case class Copy(
  source: String,
  packagePath: String,
  destination: String
)

object Copy extends Deployable {
  val all =
    Select[Copy]("SELECT source, package_path AS packagePath, destination FROM mvn.copies")

  val deployQuery = Ignore.readClassResource(classOf[Identifier], "copies.sql")

  override def deploy()(implicit connection: Connection): Unit =
    deployQuery.ignore()

  override def undeploy()(implicit connection: Connection): Unit =
    Ignore.ignore("DROP VIEW IF EXISTS mvn.copies CASCADE")

}