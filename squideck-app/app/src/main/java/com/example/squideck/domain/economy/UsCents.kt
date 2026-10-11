package com.example.squideck.domain.economy

/** Money as whole US cents, so balances and payouts never suffer floating-point rounding. Always displayed in cents. */
@JvmInline
value class UsCents(val value: Long) : Comparable<UsCents> {
    operator fun plus(other: UsCents) = UsCents(value + other.value)
    operator fun minus(other: UsCents) = UsCents(value - other.value)
    operator fun times(factor: Int) = UsCents(value * factor)
    override operator fun compareTo(other: UsCents) = value.compareTo(other.value)

    /** "110¢" */
    override fun toString(): String = "$value\u00a2"

    /** "+9¢" / "-11¢" */
    fun toSignedString(): String = if (value >= 0) "+$this" else toString()
}
