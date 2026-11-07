package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "docdb")
data class DocumentDbProperties(
    /** DocumentDB 클러스터 endpoint 호스트(스킴 없음). 빈 값이면 미연결. */
    val endpoint: String = "",
    val port: Int = 27017,
    val database: String = "chat",
)
