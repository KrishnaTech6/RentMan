package com.rentman.domain.tenancy

import kotlin.jvm.JvmInline

@JvmInline
value class AddressId(val value: Long)

/** A building or flat location the landlord owns, e.g. "12 A". One address can have several renters. */
data class Address(val id: AddressId, val name: String)
