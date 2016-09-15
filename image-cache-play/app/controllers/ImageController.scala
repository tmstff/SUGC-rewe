package controllers

import javax.inject.Inject

import persistence.CouchDbImageCache
import play.api.mvc._

import scala.concurrent.ExecutionContext

class ImageController @Inject()(couchDbImageCache: CouchDbImageCache)(implicit ec: ExecutionContext) extends Controller {

  def get(url: String) = Action.async {
    couchDbImageCache.get(url).map {
      Ok.sendEntity
    }
  }

}
