package com.rocketfuel.build.mool

import java.nio.file.Paths
import org.scalatest.FunSuite

class ModelSpec extends FunSuite {

  val bld00StringPath = "mool.java.0.0"
  val bld01StringPath = "mool.java.0.1"
  val bld02StringPath = "mool.java.0.2"
  val bld10StringPath = "mool.java.1.0"
  val bldOrphanStringPath = "mool.java.orphan"
  val bldConflictStringPath = "mool.java.conflict"

  val bld00Path = Bld.absolutePath(bld00StringPath)
  val bld01Path = Bld.absolutePath(bld01StringPath)
  val bld02Path = Bld.absolutePath(bld02StringPath)
  val bld10Path = Bld.absolutePath(bld10StringPath)
  val bldOrphanPath = Bld.absolutePath(bldOrphanStringPath)
  val bldConflictPath = Bld.absolutePath(bldConflictStringPath)

  val relCfg0Path = RelCfg.path("0")
  val relCfg1Path = RelCfg.path("1")

  val bld00 = Bld(rule_type = "java_lib", srcs = Some(Vector("0.src")), deps = Some(Vector(bld01StringPath, bldConflictStringPath)))
  val bld01 = Bld(rule_type = "java_lib", deps = Some(Vector(bld02StringPath)))
  val bld02 = Bld(rule_type = "java_lib")
  val bld10 = Bld(rule_type = "java_lib", deps = Some(Vector(bldConflictStringPath)))
  val bldOrphan = bld02
  val bldConflict = bldOrphan

  val relCfg0Blds =
    Map(
      bld00Path -> bld00,
      bld01Path -> bld01,
      bld02Path -> bld02,
      bldConflictPath -> bldConflict
    )

  val relCfg1Blds =
    Map(
      bld10Path -> bld10,
      bldConflictPath -> bldConflict
    )

  val blds =
    relCfg0Blds ++ relCfg1Blds + (bldOrphanPath -> bldOrphan)

  val relCfg0 =
    RelCfg("g", "a", "0.0", `jar-with-dependencies` = Some(RelCfg.Artifact(target = bld00StringPath, artifact_path = "a")))

  val relCfg1 =
    relCfg0.copy(`jar-with-dependencies` = Some(RelCfg.Artifact(target = bld10StringPath, artifact_path = "a")))

  private val relCfgs =
    Map(
      relCfg0Path -> relCfg0,
      relCfg1Path -> relCfg1
    )

  val model =
    Model(
      root = Paths.get("."),
      blds = blds,
      relCfgs = relCfgs,
      versions = Map.empty
    )

  test("relCfgsToBld") {
    val expected =
      Map(
        relCfg0Path -> Some(bld00Path),
        relCfg1Path -> Some(bld10Path)
      )
    assertResult(expected)(model.relCfgsToBld)
  }

  test("bldsToBlds") {
    val expected =
      Map(
        bld00Path -> Set(bld01Path, bldConflictPath),
        bld01Path -> Set(bld02Path),
        bld02Path -> Set(),
        bld10Path -> Set(bldConflictPath),
        bldConflictPath -> Set(),
        bldOrphanPath -> Set()
      )
    assertResult(expected)(model.bldsToBlds)
  }

  test("bldToBldsTransitive") {
    val expected =
      Map(
        bld00Path -> Set(bld01Path, bld02Path, bldConflictPath),
        bld01Path -> Set(bld02Path),
        bld02Path -> Set(),
        bld10Path -> Set(bldConflictPath),
        bldConflictPath -> Set(),
        bldOrphanPath -> Set()
      )
    assertResult(expected)(model.bldsToBldsTransitive)
  }

  test("relCfgsToBldsTransitive") {
    val expected =
      Map(
        relCfg0Path -> Set(bld00Path, bld01Path, bld02Path, bldConflictPath),
        relCfg1Path -> Set(bld10Path, bldConflictPath)
      )
    assertResult(expected)(model.relCfgsToBldsTransitive)
  }

  test("bldsToRelCfgs") {
    val expected =
      Map(
        bld00Path -> Set(relCfg0Path),
        bld01Path -> Set(),
        bld02Path -> Set(),
        bld10Path -> Set(relCfg1Path),
        bldConflictPath -> Set(),
        bldOrphanPath -> Set()
      )
    assertResult(expected)(model.bldsToRelCfgs)
  }

  test("bldsToRelCfgsTransitive") {
    val expected =
      Map(
        bld00Path -> Set(relCfg0Path),
        bld01Path -> Set(relCfg0Path),
        bld02Path -> Set(relCfg0Path),
        bld10Path -> Set(relCfg1Path),
        bldConflictPath -> Set(relCfg0Path, relCfg1Path),
        bldOrphanPath -> Set()
      )
    assertResult(expected)(model.bldsToRelCfgsTransitive)
  }

  test("01.src") {
    val expected =
      Vector(model.root.resolve("java").resolve("0").resolve("0").resolve("0.src"))
    val actual = bld00.srcPaths(model, bld00Path)
    assertResult(expected)(actual)
  }

  test("circular dependencies") {
    val circularBlds =
      blds + (bld02Path -> bld02.copy(deps = Some(Vector(bld00StringPath))))

    val circularModel =
      model.copy(
        blds = circularBlds
      )

    val expected =
      Map(
        relCfg0Path -> Set(bld00Path, bld01Path, bld02Path, bldConflictPath),
        relCfg1Path -> Set(bld10Path, bldConflictPath)
      )

    val actual =
      circularModel.relCfgsToBldsTransitive

    assertResult(expected)(actual)
  }

  test("bldConflicts") {
    val expectedConflicts =
      Map(
        bldConflictPath -> Set(relCfg0Path, relCfg1Path)
      )

    assertResult(expectedConflicts)(model.bldIndirectConflicts)
  }

  test("bldOrphans") {
    val expectedOrphans = Set(bldOrphanPath)

    assertResult(expectedOrphans)(model.bldOrphans)
  }

}
