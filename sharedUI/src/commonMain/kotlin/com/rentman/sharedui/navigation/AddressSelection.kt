package com.rentman.sharedui.navigation

import com.rentman.domain.tenancy.AddressId
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Carries the address chosen on the address picker back to the screen that opened it. A screen result is a
 * one-shot event, so it is a flow with no replay, like navigation events.
 */
internal class AddressSelection {
    private val events = MutableSharedFlow<AddressId>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val picked: Flow<AddressId> = events.asSharedFlow()

    fun pick(id: AddressId) {
        events.tryEmit(id)
    }
}
