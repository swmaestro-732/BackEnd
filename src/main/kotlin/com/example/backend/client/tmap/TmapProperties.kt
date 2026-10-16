package com.example.backend.client.tmap

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * T map 보행자 경로 API 앱 키. 빈 값이면 클라이언트가 호출하지 않고 [TmapPedestrianResult.Failed] 를 반환한다.
 */
@ConfigurationProperties(prefix = "tmap")
data class TmapProperties(
    val appKey: String = "",
)
