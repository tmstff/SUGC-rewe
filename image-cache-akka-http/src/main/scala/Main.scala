import akka.actor.ActorSystem
import akka.event.Logging
import akka.event.Logging.InfoLevel
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.google.inject.{Binder, Guice, Module}
import service.health._
import service.images.ImageService
import service.users.UserService
import settings.{ServiceModule, Settings}

import scala.concurrent.ExecutionContext

object Main extends App with HealthRoutes {
  
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher 

  val settings = Settings(system)

  val logger = Logging(system, getClass)

  /** Use Guice for Dependency Injection. Remove if not required */
  private val injector = Guice.createInjector(new ServiceModule(settings), new Module {
    override def configure(binder: Binder): Unit = {
      binder.bind(classOf[ActorSystem]).toInstance(system)
      binder.bind(classOf[Materializer]).toInstance(materializer)
      binder.bind(classOf[ExecutionContext]).toInstance(ec)
    }
  })
  private val userService = injector.getInstance(classOf[UserService])
  private val imageService = injector.getInstance(classOf[ImageService])
  
  val routes = logRequestResult("", InfoLevel)(userService.userRoutes ~ healthRoutes ~ imageService.imageRoutes)

  Http().bindAndHandle(routes, settings.Http.interface, settings.Http.port) map { binding =>
    logger.info(s"Server started on port {}", binding.localAddress.getPort)
  } recoverWith { case _ => system.terminate() }
}
