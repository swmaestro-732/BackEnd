package com.example.backend.sample.application.port.inbound

import com.example.backend.sample.domain.Sample

/**
 * 인바운드 포트 — 애플리케이션이 바깥(웹 등)에 제공하는 유스케이스 계약.
 * 인바운드 어댑터(컨트롤러)는 이 인터페이스에만 의존한다.
 */
interface SampleUseCase {
    fun list(): List<Sample>

    fun create(name: String): Sample
}
