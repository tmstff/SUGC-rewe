package imagecache

import javax.inject.Inject

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.util.Future
import imagecache.persistence.CouchDbImageCache

class ImageController @Inject() (couchDbImageCache: CouchDbImageCache) extends Controller {

  get("/images") { request: Request =>
    val url = request.getParam("url")
  	info(url)
    couchDbImageCache.get(url)
  }


  get("/images/metadata") { request: Request =>
    val url = request.getParam("url")
    info(url)
    couchDbImageCache.getMetadata(url)
  }
}
