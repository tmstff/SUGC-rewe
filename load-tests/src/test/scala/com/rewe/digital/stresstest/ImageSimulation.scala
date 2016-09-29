package com.rewe.digital.stresstest

import com.rewe.digital.stresstest.service.ImageServiceClient._
import com.rewe.digital.stresstest.service.{OAuthServiceClient, ImageServiceClient}

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.core.feeder.{Feeder, _}
import io.gatling.http.Predef._

class ImageSimulation extends Simulation {

  val environment = Configuration.ENVIRONMENT
  val httpConf = http.baseURL(environment.url())


  val scn = scenario("shoppingList")
    .exec(getImage).exitHereIfFailed


  setUp(
    scn.inject(
      rampUsersPerSec(2) to (Configuration.USERS_PER_SEC) during(Configuration.RAMP_UP_DURATION seconds),
      constantUsersPerSec(Configuration.USERS_PER_SEC).during(Configuration.CONSTANT_DURATION seconds)
    )
  ).protocols(httpConf)
  .throttle(
    jumpToRps(50),
    reachRps(Configuration.MAX_REQ_RATE) in (Configuration.RAMP_UP_DURATION seconds),
    holdFor(Configuration.CONSTANT_DURATION seconds)
  )
  .assertions(
    global.successfulRequests.percent.greaterThan(Configuration.TARGET_SUCCESSFUL_REQUEST_RATE)
    //global.responseTime.percentile2.lessThan(Configuration.TARGET_75TH_PERCENTILE),
    //global.responseTime.percentile3.lessThan(Configuration.TARGET_95TH_PERCENTILE),
    //global.requestsPerSec.greaterThan(Configuration.TARGET_MEAN_REQUESTS_RATE)
  )

  def modifyFeedBuilder(feederBuilder: RecordSeqFeederBuilder[String], feedModification: Record[String] => Record[String]) = {
    new RecordSeqFeederBuilder[String](records = feederBuilder.records.map(feedModification), strategy = Circular)
  }

}
