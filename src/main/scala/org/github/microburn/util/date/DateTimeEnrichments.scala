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

object DateTimeEnrichments {

  implicit class EnrichedDateTime(dateTime: DateTime) {
    def withFieldsSettedUpButNotBefore(setupFields: DateTime => DateTime,
                                       addPeriod: DateTime => DateTime): DateTime = {
      val dateWithTime = setupFields(dateTime)
      if (dateWithTime.isBefore(dateTime))
        setupFields(addPeriod(dateWithTime)) // repeated setupFields for correct time shift handling
      else
        dateWithTime
    }
    
    def withTime(time: Time): DateTime = {
      dateTime.withTime(time.hour, time.minute, 0, 0)
    }
  }

}