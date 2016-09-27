package com.example

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.util.Future

class PingController extends Controller {

  get("/images") { request: Request =>
    val url = request.getParam("url")
  	info(url)
    Future {url}
  }
}
