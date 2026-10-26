package com.example.backend.place.application.port.inbound

import com.example.backend.place.application.port.inbound.dto.RegisterPlaceCommand
import com.example.backend.place.application.port.inbound.dto.RegisteredPlaceResult

interface PlaceRegistrationUseCase {
    /** 카카오 ID로 활성 장소를 재사용하며 신규 장소만 저장한다. 기존 장소의 데이터는 덮어쓰지 않는다. */
    fun register(commands: List<RegisterPlaceCommand>): List<RegisteredPlaceResult>
}
