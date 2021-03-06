package com.reroute.backend.logic.revstationroute

import com.reroute.backend.model.location.{Station, StationWithRoute}
import com.reroute.backend.model.routing.Route
import com.reroute.backend.model.time.{TimeDelta, TimePoint}
import com.reroute.backend.stations.{ArrivableRequest, WalkableRequest}

case class RevStationQueryBuilder(
                                   genWalkableQuery: (Route, Int) => WalkableRequest,
                                   genDepartableQuery: (Route, StationWithRoute, Int) => ArrivableRequest
                                 )

object RevStationQueryBuilder {

  private case class StandardGeneratorBase[T](item: T)(implicit converter: RevStandardQuerying[T]) {
    def limit: Int = converter.limit(item)

    def effectiveWalkLeft(route: Route): TimeDelta = converter.effectiveWalkLeft(item, route)

    def effectiveWaitLeft(route: Route): TimeDelta = converter.effectiveWaitLeft(item, route)

    def starttime: TimePoint = converter.starttime(item)

    def totaltime: TimeDelta = converter.totaltime(item)
  }

  def standardBuilder[T: RevStandardQuerying](base: T,
                                              walk_modifier: Double = 1.0,
                                              paths_modifier: Double = 1.0,
                                              arrivable_modifier: Double = 1.0
                                             ): RevStationQueryBuilder = {
    val request = StandardGeneratorBase(base)
    RevStationQueryBuilder(
      genWalkableQuery = (rt, curlayer) => WalkableRequest(
        rt.currentEnd.asInstanceOf[Station],
        request.effectiveWalkLeft(rt).avgWalkDist,
        rt.endTime,
        request.effectiveWaitLeft(rt),
        (request.limit / curlayer * walk_modifier).toInt
      ),
      genDepartableQuery = (rt, data, curlayer) => ArrivableRequest(
        data,
        data.prevArrival(rt.endTime),
        request.totaltime + data.prevArrival(rt.endTime).timeUntil(rt.endTime),
        (request.limit / curlayer * arrivable_modifier).toInt
      )
    )
  }

  def simpleBuilder(maxdelta: TimeDelta, limit: Int): RevStationQueryBuilder = {
    RevStationQueryBuilder(
      genWalkableQuery = (rt, curlayer) => WalkableRequest(
        rt.currentEnd,
        (maxdelta - rt.totalTime).abs.avgWalkDist,
        rt.endTime,
        (maxdelta.abs - rt.totalTime.abs) * -1,
        (10.0 / curlayer * limit).toInt
      ),
      genDepartableQuery = (rt, data, curlayer) => ArrivableRequest(
        data,
        data.prevArrival(rt.endTime),
        (maxdelta.abs - data.prevArrival(rt.endTime).timeUntil(rt.endTime).abs) * -1,
        (10.0 / curlayer * limit).toInt
      )
    )
  }
}

trait RevStandardQuerying[T] {
  def limit(request: T): Int

  def effectiveWalkLeft(request: T, route: Route): TimeDelta

  def effectiveWaitLeft(request: T, route: Route): TimeDelta

  def starttime(request: T): TimePoint

  def totaltime(request: T): TimeDelta
}
