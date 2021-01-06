package core

import com.google.inject.AbstractModule
import core.interops.persistence.services.PersistenceService
import core.persistence.services.{FileReadChainService, FileWriteChainService}

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[FileReadChainService]).asEagerSingleton()
    bind(classOf[FileWriteChainService]).asEagerSingleton()
    bind(classOf[PersistenceService]).asEagerSingleton()
  }
}
