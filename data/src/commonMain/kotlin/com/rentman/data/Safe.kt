package com.rentman.data

import com.rentman.domain.DomainError
import com.rentman.domain.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The single place infrastructure errors are caught (ADR 0007). Everything above the data layer sees
 * [Outcome] values and never needs its own try/catch.
 */
internal fun <T> safeFlow(source: () -> Flow<T>): Flow<Outcome<T>> =
    source()
        .map<T, Outcome<T>> { Outcome.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(Outcome.Failure(DomainError.Storage(error.message)))
        }

internal suspend fun <T> safeCall(block: suspend () -> T): Outcome<T> =
    try {
        Outcome.Success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Outcome.Failure(DomainError.Storage(error.message))
    }
