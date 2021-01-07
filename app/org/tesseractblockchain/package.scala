package org

import java.time.Clock

import core.interops.persistence.services.PersistenceService

package object tesseractblockchain {
  trait BlockchainEnvironment {
    val clockEnv: Clock
    val persistenceEnv: PersistenceService
    val dependencyEnv: DependencyManager
  }
}
