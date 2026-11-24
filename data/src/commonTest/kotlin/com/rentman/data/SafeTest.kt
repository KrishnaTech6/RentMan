package com.rentman.data

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The single place infrastructure errors are caught (ADR 0007). */
class SafeTest {
    @Test
    fun safeFlowWrapsEachValueAsSuccess() = runTest {
        val result = safeFlow { flowOf(1, 2) }.toList()

        assertEquals(listOf(Outcome.Success(1), Outcome.Success(2)), result)
    }

    @Test
    fun safeFlowTurnsAThrownErrorIntoAStorageFailureAfterTheValuesSoFar() = runTest {
        val result = safeFlow {
            flow {
                emit(1)
                throw IllegalStateException("disk full")
            }
        }.toList()

        assertEquals(listOf(Outcome.Success(1), Outcome.Failure(DomainError.Storage("disk full"))), result)
    }

    @Test
    fun safeFlowLetsCancellationThrough() = runTest {
        assertFailsWith<CancellationException> {
            safeFlow { flow<Int> { throw CancellationException("stop") } }.toList()
        }
    }

    @Test
    fun safeCallWrapsTheResultAsSuccess() = runTest {
        assertEquals(Outcome.Success(42), safeCall { 42 })
    }

    @Test
    fun safeCallTurnsAThrownErrorIntoAStorageFailure() = runTest {
        val result = safeCall<Int> { throw IllegalStateException("locked") }

        assertEquals(Outcome.Failure(DomainError.Storage("locked")), result)
    }

    @Test
    fun safeCallLetsCancellationThrough() = runTest {
        assertFailsWith<CancellationException> {
            safeCall<Int> { throw CancellationException("stop") }
        }
    }
}
