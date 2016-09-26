package persistence

import java.util.Base64
import javax.inject.{Inject, Named}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import settings.SettingsImpl
import spray.json._
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.HttpMethods.PUT
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.Created
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

@Named
class CouchDbImageCache @Inject() (settings: SettingsImpl) (implicit actorSystem: ActorSystem, materializer: Materializer, ec: ExecutionContext) extends BasicFormats {

  val imageDatabaseUrl = s"${settings.couchDbUrl}/images"

  makeSureDbExists( imageDatabaseUrl )

  def get(imageUrl: String): Future[HttpResponse] = {
    val documentUrlInDb = documentUrlInDbOf( imageUrl )
    val attachmentUrlInDb = s"$documentUrlInDb/attachment"

    for (
      document <- getOrCreateDocument(imageUrl, documentUrlInDb);
      _ <-
        if ( attachmentExists( document ) ) doNothing
        else streamImageToDb( imageUrl, attachmentUrlInDb, revisionOf( document ) );
      imageFromDb <- streamImageFromDb( attachmentUrlInDb )
    ) yield ( imageFromDb )
  }

  def getMetadata(imageUrl: String): Future[HttpResponse] = {
    val documentUrlInDb = documentUrlInDbOf( imageUrl )
    Http().singleRequest( HttpRequest( uri = documentUrlInDb ) )
  }

  private def makeSureDbExists(dbUrl: String) =
    Http().singleRequest( HttpRequest( uri = imageDatabaseUrl, method = PUT)).map(
      response =>
        response.status.intValue() match {
          case 201 =>
            println("Database was created")
          case 412 =>
            println("Database already existed")
          case status =>
            println(s"Database creation replied with status $status and body '${stringEntityOf(response.entity)}'")
        }
    )

  private def documentUrlInDbOf(imageUrl: String): String = {
    val id = toBase64(imageUrl)
    s"$imageDatabaseUrl/$id"
  }

  private def toBase64(string: String): String =
    new String( Base64.getEncoder().encode( string.getBytes("UTF-8") ), "UTF-8" )


  private def getOrCreateDocument(imageUrl: String, documentUrlInDb: String): Future[ JsValue ] = {
    val documentBody = httpEntityOf(JsObject("url" -> JsString(imageUrl)))
    val documentResponse = for (
      putResponse <-
      Http().singleRequest( HttpRequest(uri = documentUrlInDb, entity = documentBody, method = PUT) );
      documentResponse <-
        if ( putResponse.status == Created ) Future ( putResponse )
        else Http().singleRequest( HttpRequest( uri = documentUrlInDb ) )
    ) yield ( documentResponse )
    documentResponse.flatMap( dr => parseJson(dr.entity) )
  }

  private def httpEntityOf(jsObject: JsObject): Strict =
    HttpEntity(contentType = `application/json`, data = ByteString(jsObject.prettyPrint))

  private def parseJson(entity: ResponseEntity): Future[JsValue] =
    stringEntityOf(entity).map( _.parseJson )

  private def streamImageFromDb( attachmentUrlInDb: String ): Future[HttpResponse] =
    Http().singleRequest( HttpRequest( uri = attachmentUrlInDb ) )

  private def attachmentExists( document: JsValue ): Boolean =
    document.asJsObject.fields.get( "_attachments" ) // schöner, bitte! :-)
      .flatMap( _.asJsObject.fields.get( "attachment" ) )
      .isDefined

  private def doNothing: Future[Unit] =
    Future(Unit)

  private def revisionOf( document: JsValue ): String =
    document.asJsObject.fields("_rev").convertTo[String]

  private def streamImageToDb( imageUrl: String, attachmentUrlInDb: String, revision: String ): Future[HttpResponse] = {
    println(s"streamImageToDb: imageUrl=$imageUrl attachmentUrlInDb=$attachmentUrlInDb revision=$revision")
    val streamImageToDbResponse = for (
      getImageResponse <- Http().singleRequest(HttpRequest(uri = imageUrl));
      streamImageToDbResponse <-
      Http().singleRequest(HttpRequest(
        uri = Uri(attachmentUrlInDb).withQuery(Uri.Query(("rev" -> revision))),
        entity = getEntityFrom(getImageResponse),
        method = PUT))
    ) yield ( streamImageToDbResponse )
    streamImageToDbResponse.map(
       r => r match  {
         case HttpResponse( Created, _, _, _ ) => r
         case HttpResponse( status, _, entity, _) =>
           println (s"PUT for $attachmentUrlInDb not successfull - status=$status entity=${ stringEntityOf( entity ) }")
           r
       }
    )
  }

  private def getEntityFrom(getImageResponse: HttpResponse): RequestEntity =
    HttpEntity(getImageResponse.entity.contentType, getImageResponse.entity.contentLengthOption.get, getImageResponse.entity.dataBytes)

  private def stringEntityOf(entity: ResponseEntity): Future[String] =
    entity.dataBytes.runFold( ByteString("") )( _ ++ _ ).map( _.decodeString("UTF-8") ) // schöner wär gut :-)

}
