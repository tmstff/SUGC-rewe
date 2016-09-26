package settings

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config._

class SettingsImpl(config: Config) extends Extension {

  lazy val couchDbUrl = config.getString("couchDbUrl")

  object Http {
    lazy val interface = config.getString("http.interface")
    lazy val port = config.getInt("http.port")
  }
}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new SettingsImpl(system.settings.config)
}
