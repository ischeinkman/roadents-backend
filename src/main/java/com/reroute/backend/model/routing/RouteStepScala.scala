package com.reroute.backend.model.routing

import com.reroute.backend.model.distance.DistanceScala
import com.reroute.backend.model.location._
import com.reroute.backend.model.time.TimeDeltaScala

trait RouteStepScala {
  type startType <: LocationPointScala
  type endType <: LocationPointScala
  val startpt: startType
  val endpt: endType
  val steptype: String
  val totaltime: TimeDeltaScala
}

trait WalkStepScala extends RouteStepScala {
  val walkdistance: DistanceScala = startpt.distanceTo(endpt)
}

case class StartWalkStep(
                          override val startpt: StartScala,
                          override val endpt: StationScala,
                          override val totaltime: TimeDeltaScala
                        ) extends WalkStepScala {
  override type startType = StartScala
  override type endType = StationScala
  override val steptype = "start_walk"
}

case class TransferWalkStep(override val startpt: StationScala, override val endpt: StationScala, override val totaltime: TimeDeltaScala) extends WalkStepScala {
  override type startType = StationScala
  override type endType = StationScala
  override val steptype = "transfer_walk"
}

case class DestinationWalkStep(override val startpt: StationScala, override val endpt: DestinationScala, override val totaltime: TimeDeltaScala) extends WalkStepScala {
  override type startType = StationScala
  override type endType = DestinationScala
  override val steptype = "destination_walk"
}

case class FullRouteWalkStep(override val startpt: StartScala, override val endpt: DestinationScala, override val totaltime: TimeDeltaScala) extends WalkStepScala {
  override type startType = StartScala
  override type endType = DestinationScala
  override val steptype = "full_route_walk"
}

trait TransitStepScala extends RouteStepScala {
  override type startType = StationScala
  override type endType = StationScala
  val transitpath: TransitPathScala
  val waittime: TimeDeltaScala
  val traveltime: TimeDeltaScala
  val stops: Int
}

case class GeneralTransitStep(override val startpt: StationScala,
                              override val endpt: StationScala,
                              override val steptype: String = "misc_transit",
                              override val transitpath: TransitPathScala,
                              override val waittime: TimeDeltaScala,
                              override val traveltime: TimeDeltaScala,
                              override val stops: Int
                             ) extends TransitStepScala {
  override val totaltime: TimeDeltaScala = waittime + traveltime
}
