package com.rewe.digital.stresstest.service

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scalaj.http.Http
import spray.json._
import DefaultJsonProtocol._

object ImageServiceClient {

  def getImage = exec(http("get anImage")
    .get("/images?url=https%3A//raw.githubusercontent.com/docker-library/docs/2307451281c6b47b85abb3af9f0097e51c70a5be/couchdb/logo.png")
    //.get("/images?url=https%3A//upload.wikimedia.org/wikipedia/commons/d/d5/West_Tian_Shan_mountains.jpg")
    .check(
      status is 200
    )
  )

}
