package com.rentman.domain

import kotlinx.coroutines.CoroutineDispatcher

/** The only source of dispatchers. Nothing else may reference `Dispatchers.*` directly. */
interface AppDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
