package com.example.backend.bootstrap.appversion

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class AppVersionPolicyProvider(
    private val repository: AppVersionPolicyRepository,
    @Value("\${app.version.cache-ttl:PT1M}") private val ttl: Duration,
) {
    private val log = KotlinLogging.logger {}

    @Volatile
    private var snapshot: Map<Pair<AppFeature, AppPlatform>, Int> = emptyMap()

    @Volatile
    private var loadedAt: Instant = Instant.EPOCH

    fun minBuild(
        feature: AppFeature,
        platform: AppPlatform,
    ): Int? {
        refreshIfStale()
        return snapshot[feature to platform]
    }

    private fun refreshIfStale() {
        if (Duration.between(loadedAt, Instant.now()) < ttl) return
        synchronized(this) {
            if (Duration.between(loadedAt, Instant.now()) < ttl) return
            runCatching { repository.loadAll() }
                .onSuccess { snapshot = it }
                .onFailure { log.warn(it) { "앱 버전 정책 갱신 실패 — 마지막 스냅샷 유지" } }
            loadedAt = Instant.now() // 실패하면 TTL 뒤에 재시도
        }
    }
}
