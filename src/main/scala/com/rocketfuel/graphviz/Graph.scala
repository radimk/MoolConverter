package com.rocketfuel.graphviz

import java.nio.file.{Files, Path}
import java.util

/*
http://www.graphviz.org/content/dot-language
 */

/**
  *
  * @param nodes nodes that aren't connected to anything
  * @param clusters nodes that are within a group
  * @param edges nodes that are connected but not in a group
  */
case class Graph(
  nodes: Set[String],
  clusters: Set[Cluster],
  edges: Set[Edge]
) {
  def toDot: String = {
    val sb = new StringBuilder()

    sb.append("digraph mool {\n  node [fontname = \"Liberation Mono:style=Regular\"]\n")

    for (node <- nodes) {
      sb.appendIndented(1, node.dotQuote)
      sb.append('\n')
    }

    clusters.foreach(c => c.toDot(1, sb))

    edges.foreach(_.toDot(1, sb))

    sb.append('}')

    sb.toString
  }

  def writeDot(file: Path): Unit = {
    Option(file.getParent).foreach(Files.createDirectories(_))
    val out = Files.newBufferedWriter(file)
    try out.write(toDot)
    finally out.close()
  }
}

case class Cluster(
  namePostfix: String,
  edges: Set[Edge]
) {
  val name = "cluster_" + namePostfix

  def toDot(indent: Int, sb: StringBuilder): Unit = {
    sb.appendIndented(indent, "subgraph ")
    sb.append(name.dotQuote)
    sb.append(" {\n")
    edges.foreach(_.toDot(indent + 1, sb))
    sb.appendIndented(indent, "}\n")
  }
}

case class Edge(
  left: String,
  right: String,
  label: Option[String]
) {
  def toDot(indent: Int, sb: StringBuilder): Unit = {
    sb.appendIndented(indent, left.dotQuote)
    sb.append(" -> ")
    sb.append(right.dotQuote)
    for (l <- label) {
      sb.append(" [label = ")
      sb.append(l.dotQuote)
      sb.append(']')
    }
    sb.append("\n")
  }

  override def hashCode(): Int =
    util.Arrays.hashCode(Array[Object](left, right))

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: Edge =>
        left == other.left && right == other.right
      case _ =>
        false
    }
  }
}
