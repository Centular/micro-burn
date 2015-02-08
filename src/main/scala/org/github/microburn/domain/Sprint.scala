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
package org.github.microburn.domain

import java.util.Date

case class Sprint(id: String,
                  details: SprintDetails,
                  private val initialBoard: BoardState,
                  currentBoard: BoardState,
                  private val events: Seq[TaskEvent]) {

  private def isActive = details.isActive

  def initialDate: Date = initialBoard.date

  def initialStoryPointsSum(implicit projectConfig: ProjectConfig): BigDecimal = initialBoard.userStoriesStoryPointsSum

  def initialStoryPointsNotDoneSum(implicit projectConfig: ProjectConfig): BigDecimal = initialBoard.userStoriesNotDoneStoryPointsSum

  def finish(timestamp: Date): SprintUpdateResult = {
    val updatedSprint = copy(
      details = details.finish
    )
    SprintUpdateResult(currentBoard, updatedSprint, Nil, sprintFinished = true, sprintRemoved = false, timestamp)
  }

  def markRemoved(timestamp: Date): SprintUpdateResult = {
    val updatedSprint = copy(
      details = details.markRemoved
    )
    SprintUpdateResult(currentBoard, updatedSprint, Nil, sprintFinished = false, sprintRemoved = true, timestamp)
  }

  def update(updatedUserStories: Seq[UserStory], finishSprint: Boolean)
            (timestamp: Date): SprintUpdateResult = {
    val updatedBoard = BoardState(updatedUserStories, timestamp)
    val newAddedEvents = currentBoard.diff(updatedBoard)
    val finished = !isActive || finishSprint
    
    val updatedSprint = copy(
      details = if (finished) details.finish else details,
      currentBoard = updatedBoard,
      events = events ++ newAddedEvents      
    )
    SprintUpdateResult(updatedBoard, updatedSprint, newAddedEvents, finished, sprintRemoved = false, timestamp)
  }
  
  def columnStatesHistory(implicit config: ProjectConfig): Seq[DateWithColumnsState] = {
    val eventsSortedAndGrouped = events
      .groupBy(_.date)
      .toSeq
      .sortBy { case (date, group) => date }
      .map { case (date, group) => group }

    val boardStatesCumulative = eventsSortedAndGrouped.scanLeft(initialBoard) { (prevBoard, currEventsGroup) =>
      currEventsGroup.foldLeft(prevBoard) { (boardAcc, event) =>
        boardAcc.plus(event)
      }
    }

    boardStatesCumulative.map(_.columnsState)
  }
}

case class SprintDetails(name: String, start: Date, end: Date, isActive: Boolean, isRemoved: Boolean) {
  def finished = !isActive

  def finish = copy(isActive = false)

  def markRemoved = copy(isRemoved = true)
}

object SprintDetails {
  def apply(name: String, start: Date, end: Date): SprintDetails = SprintDetails(name, start, end, isActive = true)

  def apply(name: String, start: Date, end: Date, isActive: Boolean): SprintDetails = SprintDetails(name, start, end, isActive, isRemoved = false)
}

case class SprintUpdateResult(private val stateBeforeUpdate: BoardState,
                              updatedSprint: Sprint,
                              newAddedEvents: Seq[TaskEvent],
                              sprintFinished: Boolean,
                              sprintRemoved: Boolean,
                              timestamp: Date) {
  def importantDetailsChange: Boolean = sprintFinished || sprintRemoved// co ze zmianą nazwy/dat?
  
  def importantBoardStateChange: Boolean = newAddedEvents.nonEmpty
}

object Sprint {
  def withEmptyEvents(id: String, details: SprintDetails, state: BoardState): Sprint =
    Sprint(id, details, initialBoard = state, currentBoard = state, IndexedSeq.empty)
}