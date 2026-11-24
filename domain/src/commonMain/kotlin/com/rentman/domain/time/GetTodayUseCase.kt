package com.rentman.domain.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Today's date in the user's time zone. The only way domain code reads the date. */
class GetTodayUseCase(private val clock: Clock, private val timeZone: TimeZone) {
    operator fun invoke(): LocalDate = clock.now().toLocalDateTime(timeZone).date
}
