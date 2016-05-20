package com.rocketfuel.jvmlib

import com.rocketfuel.mool
import java.nio.file.Paths

object MainServerUtil extends App {

  val path = "java.com.rocketfuel.server.util".split('.').toVector
  val name = "server.util"

  val moolModel = mool.Model.ofRepository(Paths.get("/Users/jshaw/git/data/vostok"))
  val relCfg = moolModel.relCfgs(path)(name)

  val model = Model.ofMoolRelCfg(moolModel)(path, name, relCfg)

  println(model)

}
