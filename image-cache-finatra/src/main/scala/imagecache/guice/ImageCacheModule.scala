package imagecache.guice

import com.google.inject.Provides
import com.twitter.finatra.httpclient.{HttpClient, RichHttpClient}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.TwitterModule
import com.typesafe.config.{Config, ConfigFactory}
import imagecache.CouchDbHttpClient
import play.api.libs.ws.WSClient
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext

object ImageCacheModule extends TwitterModule {

  val conf = ConfigFactory.load();

  val couchDbUrl = conf.getString("couchDbHostAndPort")

  val wsClient = NingWSClient()

  @Provides
  def providesConfig(): Config = conf

    @Provides
  @CouchDbHttpClient
  def providesCouchDbHttpClient( mapper: FinatraObjectMapper ): HttpClient = {
    val clientService = RichHttpClient.newClientService( couchDbUrl )
    new HttpClient( hostname = couchDbUrl, httpService = clientService, mapper = mapper )
  }

  @Provides
  def providesExecutionContext(): ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  @Provides
  def providesWsClient(): WSClient = wsClient

}
