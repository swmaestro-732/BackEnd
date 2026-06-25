package com.example.backend.domain.sample

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

@RestController
@RequestMapping("/api/samples")
class SampleController(
    private val service: SampleService,
) {
    @GetMapping
    fun list(): List<SampleResponse> = service.list().map { SampleResponse(it.id, it.name) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateSampleRequest,
    ): SampleResponse {
        val sample = service.create(request.name)
        return SampleResponse(sample.id, sample.name)
    }
}

data class CreateSampleRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
)

data class SampleResponse(
    val id: Int,
    val name: String,
)
