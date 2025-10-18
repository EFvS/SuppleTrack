package com.efvs.suppletrack.domain.model

data class Profile(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val isActive: Boolean = false
)