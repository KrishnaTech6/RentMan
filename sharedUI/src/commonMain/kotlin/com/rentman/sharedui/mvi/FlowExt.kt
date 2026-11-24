package com.rentman.sharedui.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Like `flatMapConcat`, but a value that arrives while the previous one is still running is dropped.
 * Used for writes, so a second tap on Save can't start a second write.
 */
internal fun <T, R> Flow<T>.flatMapFirst(transform: suspend (T) -> Flow<R>): Flow<R> = channelFlow {
    val busy = Mutex()
    collect { value ->
        if (busy.tryLock()) {
            launch {
                try {
                    transform(value).collect { send(it) }
                } finally {
                    busy.unlock()
                }
            }
        }
    }
}
