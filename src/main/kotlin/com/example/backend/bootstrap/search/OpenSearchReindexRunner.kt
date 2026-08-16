package com.example.backend.bootstrap.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.course.application.port.inbound.CourseReindexUseCase
import com.example.backend.place.application.port.inbound.PlaceReindexUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * 부팅 시 인덱스가 비어있으면 자동 backfill, opensearch.reindex-on-startup=true면 강제 재색인(드리프트 복구).
 * [OpenSearchClient] 있을 때만, fail-soft. [OpenSearchIndexInitializer](@Order 0) 가 인덱스를 만든 뒤 실행된다(@Order 1).
 */
@Component
@Order(1)
class OpenSearchReindexRunner(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
    private val properties: OpenSearchProperties,
    private val placeReindexUseCase: PlaceReindexUseCase,
    private val courseReindexUseCase: CourseReindexUseCase,
) : ApplicationRunner {
    private val log = KotlinLogging.logger {}

    private data class Target(
        val alias: String,
        val reindex: () -> Int,
    )

    override fun run(args: ApplicationArguments) {
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정(로컬/CI) → no-op

        listOf(
            Target("place", placeReindexUseCase::reindexAll),
            Target("course", courseReindexUseCase::reindexAll),
        ).forEach { t ->
            try {
                val empty = client.count { c -> c.index(t.alias) }.count() == 0L
                if (properties.reindexOnStartup || empty) {
                    val n = t.reindex()
                    val reason = if (properties.reindexOnStartup) "강제" else "빈 인덱스 backfill"
                    log.info { "OpenSearch 재색인: ${t.alias} ${n}건 ($reason)" }
                }
            } catch (e: Exception) {
                log.warn { "OpenSearch 재색인 실패(무시): ${t.alias} — ${e.message}" }
            }
        }
    }
}
