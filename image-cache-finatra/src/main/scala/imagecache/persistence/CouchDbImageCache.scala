package imagecache.persistence

import java.util.Base64
import javax.inject.Inject

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.twitter.finagle.http._
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging
import com.twitter.util.Future
import com.typesafe.config.Config
import imagecache.CouchDbHttpClient
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext


class CouchDbImageCache @Inject() (@CouchDbHttpClient couchDbHttpClient: HttpClient, wSClient: WSClient, config: Config, mapper: FinatraObjectMapper)
    (implicit executionContext: ExecutionContext) extends Logging {

  // for wsClient
  val couchDbUrl = config.getString("couchDbUrl")

  makeSureDbExists(dbPath)

  def get(imageUrl: String): Future[Response] = {
    for (
      document <- getOrCreateDocument(imageUrl, dbDocumentPath(imageUrl));
      _ <-
        if ( attachmentExists(document) ) doNothing
        else streamImageToDb( imageUrl, dbAttachmentPath(imageUrl) , revisionOf( document ) );
      imageFromDb <- couchDbHttpClient.execute( RequestBuilder.get( dbAttachmentPath( imageUrl ) ).request )
    ) yield (imageFromDb)
  }

  def getMetadata(imageUrl: String): Future[Response] =
    couchDbHttpClient.execute( RequestBuilder.get( dbDocumentPath( imageUrl ) ).request )

  private def makeSureDbExists(path: String) =
    couchDbHttpClient.execute( RequestBuilder.put(path) ).map(
      response =>
        response.status.code match {
          case 201 =>
            info("Database was created")
          case 412 =>
            info("Database already existed")
          case status =>
            warn(s"Database creation replied with status $status and body '${response.contentString}'")
        }
    )

  private def getOrCreateDocument(imageUrl: String, documentUrlInDb: String): Future[ JsonNode ] = {
    for (
      _ <- couchDbHttpClient.execute( RequestBuilder.put( documentUrlInDb ).body( toJson("url" -> imageUrl) , Message.ContentTypeJson).request );
      documentResponse <- couchDbHttpClient.executeJson[ JsonNode ]( RequestBuilder.get( documentUrlInDb ).request )
    ) yield (documentResponse)
  }

  private def toJson( param: (String, String) ): String =
    mapper.writePrettyString( new JsonNodeFactory(false).objectNode().put(param._1, param._2) )

  private def attachmentExists(document: JsonNode): Boolean =
    document.hasNonNull("_attachments") &&
    document.get("_attachments").hasNonNull("attachment")

  private def doNothing: Future[Unit] =
    Future(Unit)

  private def streamImageToDb(imageUrl: String, attachmentUrlInDb: String, revision: String): Future[WSResponse] = {
    import imagecache.util.TwitterConverters._

    scalaToTwitterFuture(
      wSClient.url( imageUrl ).get().map{ r => debug( s"get image from internet status code ${r.status} and Content-Type ${r.header("Content-Type")}" ); r}
        .flatMap {
          response =>
            wSClient.url( couchDbUrl + "/" + attachmentUrlInDb )
              .withQueryString("rev" -> revision)
              .withHeaders( "Content-Type" -> response.header("Content-Type").getOrElse("application/octet-stream") )
              .put( response.bodyAsBytes )
              .map{ r => info("streamImageToDb status: " + r.status); r }
        }
    )

    /* oh crap, this can't be true ...
    val uri = new URI(imageUrl)
    val client = finagle.Http.newService(uri.getHost + (if (uri.getPort >= 0) ":" + uri.getPort else ":80"))

    val request = Request(Method.Get, uri.getPath)

    client(request)
      .map{r => info("streamImageToDb Status: " + r.status + " body:" + r.contentString); r}
      */
  }

  private def revisionOf(document: JsonNode): String = document.get("_rev").asText()

  private def dbAttachmentPath(imageUrl: String) =
    dbDocumentPath(imageUrl) + "/attachment"

  private def dbDocumentPath(imageUrl: String) =
    dbPath + toBase64(imageUrl)

  private def dbPath: String = "/images/"

  private def toBase64(string: String): String =
    new String( Base64.getEncoder().encode( string.getBytes("UTF-8") ), "UTF-8" )

}
