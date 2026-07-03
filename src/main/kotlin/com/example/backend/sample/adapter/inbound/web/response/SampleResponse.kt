package com.example.backend.sample.adapter.inbound.web.response

import com.example.backend.sample.application.dto.SampleResult

/** 웹 응답 DTO. 유스케이스 결과([SampleResult])를 직렬화 형태로 변환한다. */
data class SampleResponse(
    val id: Int,
    val name: String,
) {
    companion object {
        fun from(result: SampleResult): SampleResponse = SampleResponse(id = result.id, name = result.name)
    }
}
