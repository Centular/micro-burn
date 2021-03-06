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
package org.github.microburn.util.date

import org.joda.time.DateTime

object DateMath {

  def minOfDates(dates: DateTime*): DateTime = {
    dates.sortWith(_.compareTo(_) < 0).head
  }

  def maxOfDates(dates: DateTime*): DateTime = {
    dates.sortWith(_.compareTo(_) < 0).last
  }

  def roundDate(date: DateTime): DateTime = {
    val base = if (date.isAfter(halfOfDay(date))) {
      date.plusDays(1)
    } else {
      date
    }
    base.withTime(0, 0, 0, 0)
  }

  private def halfOfDay(date: DateTime): DateTime = {
    date.withTime(12, 0, 0, 0)
  }

}