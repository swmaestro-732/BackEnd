package com.example.backend.sample.adapter.inbound.web

import com.example.backend.sample.application.port.inbound.SampleUseCase
import com.example.backend.sample.domain.Sample
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — HTTP 요청을 인바운드 포트([SampleUseCase]) 호출로 변환한다.
 * 도메인 모델은 밖으로 노출하지 않고 요청/응답 DTO 로 매핑한다.
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
    ): SampleResponse = SampleResponse.from(sampleUseCase.create(request.name))
}

data class CreateSampleRequest(
    @field:NotBlank
    @field:Size(max = Sample.MAX_NAME_LENGTH)
    val name: String,
)

data class SampleResponse(
    val id: Int,
    val name: String,
) {
    companion object {
        fun from(sample: Sample): SampleResponse =
            SampleResponse(
                id = checkNotNull(sample.id) { "영속화된 Sample 은 id 를 가진다." },
                name = sample.name,
            )
    }
}
