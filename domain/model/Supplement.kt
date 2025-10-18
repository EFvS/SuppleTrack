package com.efvs.suppletrack.domain.model

data class Supplement(
    val id: Long = 0,
    val profileId: Long,
    val name: String,
    val dosage: String? = null,
    val note: String? = null,
    val icon: String,
    val color: Long,
    val interval: SupplementInterval?, // null if PRN
    val startDate: Long,
    val endDate: Long?,
    val isActive: Boolean = true
)

sealed class SupplementInterval {
    data class Daily(val times: List<Int>) : SupplementInterval() // e.g., [8, 20] for 8:00, 20:00
    data class EveryXDays(val days: Int, val time: Int) : SupplementInterval()
    data class Weekdays(val weekdays: List<Int>, val time: Int) : SupplementInterval()
    object PRN : SupplementInterval() // as needed
}