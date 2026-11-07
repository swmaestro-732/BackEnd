package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "redis")
data class RedisProperties(
    /** ElastiCache primary endpoint 호스트(스킴 없음). 빈 값이면 미연결. */
    val endpoint: String = "",
    val port: Int = 6379,
    /** IAM 인증용 ElastiCache user id. */
    val user: String = "",
    /** SigV4 서명 대상 호스트로 쓰는 replication group id. */
    val cacheName: String = "",
    val region: String = "",
)
