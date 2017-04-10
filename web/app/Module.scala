import com.google.inject.AbstractModule
import java.time.Clock

import net.codingwell.scalaguice.ScalaModule

import services.MetricsRepoService

class Module extends AbstractModule with ScalaModule {
  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind[Clock].toInstance(Clock.systemDefaultZone)

    bind[MetricsRepoService].asEagerSingleton()
  }
}
