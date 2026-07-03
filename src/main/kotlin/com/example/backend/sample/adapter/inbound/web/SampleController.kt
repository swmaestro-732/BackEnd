package com.example.backend.sample.adapter.inbound.web

import com.example.backend.sample.adapter.inbound.web.request.CreateSampleRequest
import com.example.backend.sample.adapter.inbound.web.response.SampleResponse
import com.example.backend.sample.application.port.inbound.SampleUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — HTTP 요청을 인바운드 포트([SampleUseCase]) 호출로 변환한다.
 * Request → Command, Result → Response 로 매핑해 도메인/애플리케이션 타입을 밖으로 노출하지 않는다.
 */
@RestController
@RequestMapping("/api/samples")
class SampleController(
    private val sampleUseCase: SampleUseCase,
) {
    @GetMapping
    fun list(): List<SampleResponse> = sampleUseCase.list().map(SampleResponse::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateSampleRequest,
    ): SampleResponse = SampleResponse.from(sampleUseCase.create(request.toCommand()))
}
