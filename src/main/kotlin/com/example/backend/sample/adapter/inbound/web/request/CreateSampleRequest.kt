package com.example.backend.sample.adapter.inbound.web.request

import com.example.backend.sample.application.dto.CreateSampleCommand
import com.example.backend.sample.domain.model.Sample
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 웹 요청 DTO. 애플리케이션 경계 타입([CreateSampleCommand])으로 매핑한다. */
data class CreateSampleRequest(
    @field:NotBlank
    @field:Size(max = Sample.MAX_NAME_LENGTH)
    val name: String,
) {
    fun toCommand(): CreateSampleCommand = CreateSampleCommand(name = name)
}
