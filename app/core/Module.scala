package core

import com.google.inject.AbstractModule
import core.interops.persistence.services.PersistenceService

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[PersistenceService]).asEagerSingleton()
  }
}
