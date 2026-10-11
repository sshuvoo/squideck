package com.example.squideck.domain.economy

import kotlin.math.abs

/** Money as whole US cents, so balances and payouts never suffer floating-point rounding. */
@JvmInline
value class UsCents(val value: Long) : Comparable<UsCents> {
    operator fun plus(other: UsCents) = UsCents(value + other.value)
    operator fun minus(other: UsCents) = UsCents(value - other.value)
    operator fun times(factor: Int) = UsCents(value * factor)
    override operator fun compareTo(other: UsCents) = value.compareTo(other.value)

    /** "$1.10" */
    override fun toString(): String = "${if (value < 0) "-" else ""}$${abs(value) / 100}.${(abs(value) % 100).toString().padStart(2, '0')}"

    /** "+$0.09" / "-$0.11" */
    fun toSignedString(): String = if (value >= 0) "+$this" else toString()
}
