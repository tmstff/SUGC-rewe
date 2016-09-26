package service.images

import javax.inject.{Inject, Named}

import akka.http.scaladsl.server.Directives._
import persistence.CouchDbImageCache

@Named
class ImageService @Inject() (couchDbImageCache: CouchDbImageCache)  {

  val imageRoutes =
    path("images") {
      get {
        parameters("url") {
          url =>
            complete( couchDbImageCache.get(url) )
        }
      }
      path("metadata") {
        get {
          parameters("url") {
            url =>
              complete( couchDbImageCache.getMetadata(url) )
          }
        }
    }


}
