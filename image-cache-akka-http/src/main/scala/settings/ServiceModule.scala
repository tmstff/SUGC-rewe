package settings

import com.google.inject.AbstractModule
import service.users.{UserRepository, UserRepositoryImpl}

/**
  * Example showing use of Guice for DI. Remove if not required
  */
class ServiceModule(settings: SettingsImpl) extends AbstractModule {
  override def configure() {
    bind(classOf[UserRepository]).to(classOf[UserRepositoryImpl])
    bind(classOf[SettingsImpl]).toInstance(settings)
  }
}
