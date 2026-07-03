package com.example.backend.sample.application.dto

import com.example.backend.sample.domain.model.Sample

/** 유스케이스 출력 — 도메인 모델을 밖으로 노출하지 않기 위한 애플리케이션 경계 타입. */
data class SampleResult(
    val id: Int,
    val name: String,
) {
    companion object {
        fun from(sample: Sample): SampleResult =
            SampleResult(
                id = checkNotNull(sample.id) { "영속화된 Sample 은 id 를 가진다." },
                name = sample.name,
            )
    }
}
