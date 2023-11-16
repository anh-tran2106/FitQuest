package ca.unb.mobiledev.fitquest

import java.time.LocalDate

data class UserData (
    val id: String? = null,
    val username: String? = null,
    val password: String? = null,
    val days: Map<String, Day>? = null
)

data class Day (
    val date: LocalDate? = null,
    val stepCounter: Int? = 0,
    val stepMax: Int? = 0
)
