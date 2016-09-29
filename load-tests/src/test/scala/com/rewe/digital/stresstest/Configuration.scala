package com.rewe.digital.stresstest

import java.util.logging.Logger

import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.reflect.runtime.universe._

object Configuration {
  val LOGGER: Logger = Logger.getLogger(Configuration.getClass.getName())

  // user count config
  val DEFAULT_USERS_PER_SEC = 10
  val USERS_PER_SEC = getEnvironmentVar("users", DEFAULT_USERS_PER_SEC)

  // duration config
  val DEFAULT_DURATION = 15
  val DEFAULT_RAMP_UP_DURATION = 15
  val CONSTANT_DURATION = getEnvironmentVar("duration", DEFAULT_DURATION)
  val RAMP_UP_DURATION = getEnvironmentVar("ramp_up_duration", DEFAULT_RAMP_UP_DURATION)

  // req rate config
  val DEFAULT_MAX_REQ_RATE = 200
  val MAX_REQ_RATE = getEnvironmentVar("request_rate", DEFAULT_MAX_REQ_RATE)

  // environment config
  val DEFAULT_ENVIRONMENT = Environment.PLAY
  val ENVIRONMENT = getEnvironmentVar("environment", DEFAULT_ENVIRONMENT)

  // Target values to determine wether test was a success
  val DEFAULT_75TH_PERCENTILE = 60
  val DEFAULT_95TH_PERCENTILE = 220
  val DEFAULT_SUCCESSFUL_REQUEST_RATE = 99
  val DEFAULT_MEAN_REQUESTS_RATE = 125
  val TARGET_75TH_PERCENTILE = getEnvironmentVar("target_75th_percentile", DEFAULT_75TH_PERCENTILE)
  val TARGET_95TH_PERCENTILE = getEnvironmentVar("target_95th_percentile", DEFAULT_95TH_PERCENTILE)
  val TARGET_SUCCESSFUL_REQUEST_RATE = getEnvironmentVar("target_successful_request_rate", DEFAULT_SUCCESSFUL_REQUEST_RATE)
  val TARGET_MEAN_REQUESTS_RATE = getEnvironmentVar("target_mean_requests_rate", DEFAULT_MEAN_REQUESTS_RATE)

  def parseParameter[T: TypeTag](value: String) = typeOf[T] match {
    case t if t =:= typeOf[String] => value
    case t if t =:= typeOf[Int] => Integer.parseInt(value)
  }

  def getEnvironmentVar[T: TypeTag](envName: String, defaultValue: T): T = {
    val envVar = System.getProperty(envName)
    val result =
      if (envVar != null) {
        val value = typeOf[T] match {
          case t if t =:= typeOf[String] => envVar
          case t if t =:= typeOf[Int] => Integer.parseInt(envVar)
          case t if t =:= typeOf[Environment] => Environment.valueOf(envVar)
          case t if t =:= typeOf[Boolean] => envVar.toBoolean
        }
        value.asInstanceOf[T]
      } else {
        defaultValue
      }
    LOGGER.info("Config parameter '%s' = \"%s\"".format(envName, result))
    result
  }

}
