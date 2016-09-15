package persistence

import java.util.Base64
import javax.inject.Inject

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import org.asynchttpclient.{AsyncCompletionHandler, AsyncHttpClient, Response}
import org.asynchttpclient.request.body.generator.ReactiveStreamsBodyGenerator
import play.api.http.HttpEntity
import play.api.http.HttpEntity.Streamed
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{StreamedResponse, WSClient, WSResponseHeaders}
import play.api.{Configuration, Logger}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

class CouchDbImageCache @Inject()(configuration: Configuration, wsClient: WSClient)(implicit ec: ExecutionContext, mat: Materializer)  {

  val couchDbUrl = configuration.getString("couchDbUrl").getOrElse(
    throw new RuntimeException("You need to configure \"couchDbUrl\"!"))

  val imageDatabaseUrl = s"$couchDbUrl/images"

  makeSureDbExists(imageDatabaseUrl)

  def get(imageUrl: String): Future[Streamed] = {
    val id = toBase64(imageUrl)
    val documentUrlInDb = s"$imageDatabaseUrl/$id"
    val attachmentUrlInDb = s"$documentUrlInDb/attachment"

    for (
      document <- getOrCreateDocument(imageUrl, documentUrlInDb);
      _ <-
        if ( attachmentExists(document) ) doNothing
        else streamImageToDb( imageUrl, attachmentUrlInDb, revisionOf( document ) );
      imageFromDb <- streamImageFromDb( attachmentUrlInDb )
    ) yield (imageFromDb)

  }

  private def doNothing: Future[Unit] = {
    Future(Unit)
  }

  private def makeSureDbExists(dbUrl: String) =
    wsClient.url( imageDatabaseUrl ).execute( "PUT" ).map(
      response =>
        response.status match {
          case 201 =>
            Logger.info("Database was created")
          case 412 =>
            Logger.info("Database already existed")
          case status =>
            Logger.warn(s"Database creation replied with status $status and body '${response.body}'")
        }
    )

  private def getOrCreateDocument(imageUrl: String, documentUrlInDb: String): Future[ JsValue ] = {
    for (
      putResponse <-
        wsClient.url(documentUrlInDb)
          .put( Json.obj( "url" -> imageUrl ) );
      documentResponse <-
        if (putResponse.status == 201) Future { putResponse }
        else wsClient.url( documentUrlInDb ).get()
    ) yield ( documentResponse.json )
  }

  private def attachmentExists(document: JsValue): Boolean =
    (document \ "_attachments" \ "attachment").toOption.isDefined

  private def revisionOf(document: JsValue): String =
    (document \ "rev").as[String]

  private def streamImageToDb( imageUrl: String, attachmentUrlInDb: String, documentRevision: String ): Future[Unit] = {
    wsClient.url(imageUrl).withMethod("GET").stream().flatMap {
      case StreamedResponse(response, body) =>

        if (response.status != 200) {
          throw new RuntimeException(s"status code for GET to $imageUrl was ${response.status}!")
        }

        val contentType = contentTypeOf(response)
        val contentLength = contentLengthOf(response)

        val putResponse = Promise[Response]()
        wsClient.underlying[AsyncHttpClient]
          .preparePut( attachmentUrlInDb )
          .setQueryParams( Map( "rev" -> List( documentRevision ).asJava ).asJava )
          .setBody( sourceToBodyGenerator(body, contentLength) )
          .setHeader( "Content-Type", contentType )
          .execute( asyncCompletionHandler( putResponse ) )
        putResponse.future.map {
          r =>
            if (r.getStatusCode != 201) {
              Logger.warn( s"status code for PUT to $attachmentUrlInDb was ${r.getStatusCode}!" )
            }
        }
    }
  }

  def sourceToBodyGenerator(body: Source[ByteString, _], contentLength: Long): ReactiveStreamsBodyGenerator = {
    val toByteBuffer = Flow[ByteString].map( _.toByteBuffer )
    val publisher = body.via( toByteBuffer ).runWith( Sink.asPublisher(true) )
    new ReactiveStreamsBodyGenerator( publisher, contentLength )
  }

  private def asyncCompletionHandler(promise: Promise[Response]): AsyncCompletionHandler[Response] =
    new AsyncCompletionHandler[Response]() {
      override def onCompleted(response: Response) = {
        promise.success(response)
        response
      }

      override def onThrowable(t: Throwable) =
        promise.failure(t)
    }

  private def streamImageFromDb(attachmentUrlInDb: String): Future[Streamed] = {
    wsClient.url(attachmentUrlInDb).withMethod("GET").stream().map {
      case StreamedResponse(response, body) =>

        if (response.status != 200) {
          throw new RuntimeException(s"status code for GET to $attachmentUrlInDb was ${response.status}!")
        }

        val contentType = contentTypeOf(response)

        // we expect the response from couchDb not to be chunked
        response.headers.get("Content-Length") match {
          case Some(Seq(length)) =>
            HttpEntity.Streamed(body, Some(length.toLong), Some(contentType))
          case contentLength =>
            throw new RuntimeException(s"ContentLength did not contain a single entry ($contentLength)")
        }
    }
  }

  private def contentTypeOf(response: WSResponseHeaders): String =
    response.headers.get("Content-Type").flatMap(_.headOption)
      .getOrElse("application/octet-stream")

  private def contentLengthOf(response: WSResponseHeaders): Long =
    response.headers.get("Content-Length") match {
      case Some(Seq(length)) =>
        length.toLong
      case _ =>
        -1L
    }

  private def toBase64(string: String): String =
    new String( Base64.getEncoder().encode( string.getBytes("UTF-8") ), "UTF-8" )
}

