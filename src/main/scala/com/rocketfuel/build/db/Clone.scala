package com.rocketfuel.build.db

import java.nio.file.{Files, Path}
import scala.sys.process.Process

object Clone {

  private def clone(url: String, change: String, destination: Path): Unit = {
    val absoluteDestination = destination.toAbsolutePath.toString
    val destinationFile = destination.toFile

    if (Files.exists(destination)) {
      if (destination.getParent == null)
        throw new RuntimeException("do not deploy to root")

      //There doesn't seem to be an easy way to do this from JVM.
      Process(s"rm -rf $absoluteDestination").!
    }

    Process("git", Seq("clone", url, absoluteDestination)).!

    Process(Seq("git", "fetch", url, change), destinationFile).!

    Process(Seq("git", "checkout", "FETCH_HEAD"), destinationFile).!
  }

  def gridModeling(destinationRoot: Path): Unit = {
    clone("ssh://git.rfiserve.net:29418/grid/modeling", "refs/changes/70/112770/10", destinationRoot)
  }

  def vostok(destinationRoot: Path): Unit = {
    clone("ssh://git.rfiserve.net:29418/data/vostok", "refs/changes/15/115415/5", destinationRoot)
  }

}
