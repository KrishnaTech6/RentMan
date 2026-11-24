package com.rentman.sharedui.mvi

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FlatMapFirstTest {
    @Test
    fun dropsValuesThatArriveWhileTheFirstIsStillRunning() = runTest {
        val results = flow {
            emit(1)
            emit(2) // arrives while 1 is still running, so it is dropped
            delay(200)
            emit(3) // 1 has finished by now
        }.flatMapFirst { value -> flow { delay(100); emit(value) } }
            .toList()

        assertEquals(listOf(1, 3), results)
    }
}
