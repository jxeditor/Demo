package com.test.flink.stream.graph.template

import org.apache.flink.streaming.api.scala.DataStream

import scala.collection.mutable.Map

object Graph {
  def draw[T](dataStream: DataStream[T]): Graph[T] = {
    new Graph[T](dataStream)
  }
}

class Graph[T](dataStream: DataStream[T]) {
  private val pointMap = Map[String, Point]()
  private val edgeMap = Map[String, Map[String, AnyRef => Boolean]]()

  def addPoint(name: String, point: Point) = {
    pointMap(name) = point
  }

  def addEdge(from: String, to: String, filter: AnyRef => Boolean): Unit = {
    val filterMap = edgeMap.get(from)
    if (filterMap != None) {
      val map = filterMap.get
      map += (to -> filter)
    }
    else {
      edgeMap += (from -> Map(to -> filter))
    }
  }

  def finish(): Unit = {
    val outSize = edgeMap.map(x => (x._1, x._2.size))
    val inSize = edgeMap.map(x => x._2.toList).reduce((a, b) => a ::: b).groupBy(x => x._1).map(x => (x._1, x._2.size))
    val rootPoint = pointMap.filter(x => outSize.getOrElse(x._1, 0) != 0 && inSize.getOrElse(x._1, 0) == 0).toList

    rootPoint.foreach(x => linkPoint(x._1, x._2, dataStream))
  }

  private def linkPoint[U](name: String, point: Point, dataStream: DataStream[U]): Unit = {
    if (point.isInstanceOf[ExecPoint[U, _]]) {
      val afDataStream = point.asInstanceOf[ExecPoint[U, _]].process(dataStream)
      val toMap = edgeMap.get(name).get
      toMap.foreach(x => {
        val toName = x._1
        val func = x._2
        val toPoint = pointMap.get(toName).get
        val toDataStream = afDataStream.filter(x => func(x.asInstanceOf[AnyRef]))
        linkPoint(toName, toPoint, toDataStream)
      })
    }
    else {
      point.asInstanceOf[EndPoint[U]].process(dataStream)
    }
  }
}
