package org.tesseractblockchain

import com.google.inject.AbstractModule
import org.tesseractblockchain.logic.{Blockchain, PendingTransactions}
import org.tesseractblockchain.services.{BlockService, DispatcherService, TransactionService}

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Blockchain]).asEagerSingleton()

    bind(classOf[DependencyManager]).asEagerSingleton()
    bind(classOf[BlockService]).asEagerSingleton()
    bind(classOf[Application]).asEagerSingleton()

    bind(classOf[DispatcherService]).asEagerSingleton()
    bind(classOf[TransactionService]).asEagerSingleton()
    bind(classOf[PendingTransactions]).asEagerSingleton()
  }

}
