package com.example.backend.user.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchConfig
import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * DB 가 원본이며 같은 alias 에 멱등 upsert 한다.
 * - 단건 [index]/[delete](동기 경로): 실패를 **던진다** — 리스너가 받아 SQS 폴백으로 재처리한다.
 * - 벌크 [index](재색인 backfill): best-effort 라 실패를 로그만 남기고 삼킨다(다음 재색인이 보정).
 * 색인 클라이언트 미설정(로컬·CI)이면 모든 경로가 no-op 다.
 */
@Component
class OpenSearchSavedPlaceIndexAdapter(
    @Qualifier(OpenSearchConfig.INDEX_CLIENT) private val clientProvider: ObjectProvider<OpenSearchClient>,
    properties: OpenSearchProperties,
) : SavedPlaceSearchIndexPort {
    private val log = KotlinLogging.logger {}
    private val indexAlias = properties.withPrefix("saved_place")

    override fun index(document: SavedPlaceIndexDocument) = bulkIndex(listOf(document), failSoft = false)

    override fun index(documents: List<SavedPlaceIndexDocument>) = bulkIndex(documents, failSoft = true)

    private fun bulkIndex(
        documents: List<SavedPlaceIndexDocument>,
        failSoft: Boolean,
    ) {
        if (documents.isEmpty()) return
        val client = clientProvider.ifAvailable ?: return
        try {
            val response =
                client.bulk { bulk ->
                    bulk.requireAlias(true).operations(
                        documents.map { document ->
                            BulkOperation
                                .Builder()
                                .index { op ->
                                    op.index(indexAlias).id("${document.userId}:${document.placeId}").document(
                                        mapOf(
                                            "userId" to document.userId,
                                            "placeId" to document.placeId,
                                            "location" to
                                                mapOf(
                                                    "lat" to document.location.latitude,
                                                    "lon" to document.location.longitude,
                                                ),
                                            "savedAt" to document.savedAt.toEpochMilli(),
                                            "name" to document.name,
                                            "category" to document.category,
                                            "imageUrl" to document.imageUrl,
                                            "rating" to document.rating,
                                            "ratingCount" to document.ratingCount,
                                        ),
                                    )
                                }.build()
                        },
                    )
                }
            if (response.errors()) {
                val failed = response.items().count { it.error() != null }
                if (failSoft) {
                    log.error { "saved_place 색인 부분 실패(무시): $failed/${documents.size}건 실패" }
                } else {
                    error("saved_place 색인 실패: $failed/${documents.size}건")
                }
            }
        } catch (e: Exception) {
            if (failSoft) {
                log.error(e) { "saved_place 색인 실패(무시): ${documents.size}건 — ${e.message}" }
            } else {
                throw e
            }
        }
    }

    override fun delete(
        userId: Long,
        placeId: Long,
    ) {
        // 단건 삭제는 동기 경로 — 전송 실패를 던져 리스너가 폴백으로 재처리하게 한다(없는 문서 삭제는 not_found 라 예외 아님, 멱등).
        val client = clientProvider.ifAvailable ?: return
        client.delete { it.index(indexAlias).id("$userId:$placeId") }
    }
}
