/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.github.microburn

import java.io.File

import com.typesafe.config.ConfigFactory
import org.github.microburn.domain.actors.ProjectActor
import org.github.microburn.integration.Integration
import org.github.microburn.integration.support.kanban.AutomaticScrumManagerActor
import org.github.microburn.service.{ProjectUpdaterActor, SprintColumnsHistoryProvider}
import org.joda.time.Days

import scala.concurrent.duration.FiniteDuration

class ApplicationContext private(val projectActor: ProjectActor,
                                 val updater: ProjectUpdaterActor,
                                 val integration: Integration,
                                 val columnsHistoryProvider: SprintColumnsHistoryProvider,
                                 val optionalAutomaticScrumManager: Option[AutomaticScrumManagerActor],
                                 appConfig: ApplicationConfig) {
  def connectorConfig: ConnectorConfig = appConfig.connectorConfig
  def authorizationConfig: AuthorizationConfig = appConfig.authorizationConfig

  def clientFetchIfNoChangesPeriod: FiniteDuration = appConfig.durations.clientFetchIfNoChangesPeriod
  def defaultSprintDuration: Days = appConfig.durations.defaultSprintDuration

  def hasAutomaticScrumManagement = optionalAutomaticScrumManager.isDefined
}

object ApplicationContext {
  def apply(): ApplicationContext = context

  private lazy val context = {
    val configFile = System.getProperty("config", "application.conf")
    val appConfig = ApplicationConfig(new File(configFile))
    val projectActor = new ProjectActor(appConfig.projectConfig)
    val integration = appConfig.integrationFactory(projectActor)
    val optionalAutomaticScrumManager =
      AutomaticScrumManagerActor.optionallyPrepareAutomaticScrumManager(
        appConfig.projectConfig.scrumManagementMode,
        integration,
        appConfig.projectConfig.dataRoot,
        appConfig.durations.tickPeriod
      )

    val updater = new ProjectUpdaterActor(integration, appConfig.durations.fetchPeriod)
    val columnsHistoryProvider = new SprintColumnsHistoryProvider(projectActor)(appConfig.projectConfig)

    new ApplicationContext(
      projectActor                  = projectActor,
      updater                       = updater,
      integration                   = integration,
      columnsHistoryProvider        = columnsHistoryProvider,
      appConfig                     = appConfig,
      optionalAutomaticScrumManager = optionalAutomaticScrumManager)
  }

}