package com.kkfittracking.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

/** Today's date, and the new one shortly after midnight (checked once a minute). */
fun todayFlow(clock: () -> LocalDate = LocalDate::now): Flow<LocalDate> = flow {
    while (true) {
        emit(clock())
        delay(60_000)
    }
}.distinctUntilChanged()
