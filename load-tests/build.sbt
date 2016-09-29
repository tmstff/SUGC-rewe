enablePlugins(GatlingPlugin)

name := "gatling-shopping-list-load-tests"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.0" % "test"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "2.2.0" % "test"
libraryDependencies += "org.scalaj"           %% "scalaj-http"               % "1.1.4" % "test"
libraryDependencies += "io.spray"             %%  "spray-json"               % "1.3.2" % "test"
    