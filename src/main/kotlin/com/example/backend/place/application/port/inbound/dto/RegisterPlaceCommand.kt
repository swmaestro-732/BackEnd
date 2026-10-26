package com.example.backend.place.application.port.inbound.dto

import com.example.backend.common.geo.Coordinate

data class RegisterPlaceCommand(
    val kakaoPlaceId: String,
    val name: String,
    val category: String,
    val coordinate: Coordinate,
    val address: String,
    val description: String? = null,
    val imageUrl: String? = null,
)

data class RegisteredPlaceResult(
    val id: Long,
    val kakaoPlaceId: String,
    val category: String,
    val coordinate: Coordinate,
)
