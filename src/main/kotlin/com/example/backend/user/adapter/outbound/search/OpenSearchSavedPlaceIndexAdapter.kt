package com.example.backend.user.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchConfig
import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.OpenSearchException
import org.opensearch.client.opensearch._types.VersionType
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * DB 가 원본이며 같은 alias 에 멱등 upsert 한다.
 * - 단건 [index]/[delete](동기 경로): 실패를 **던진다** — 리스너가 받아 SQS 폴백으로 재처리한다.
 *   동시성(순서 역전) 보호로 external version 을 쓴다 — index 는 savedAt, delete 는 removedAt 을 version 으로 실어,
 *   늦게 도착한 과거 쓰기(낮은 version)는 409(version conflict)로 무시한다(= 더 최신이 이미 있음, 정상 처리).
 * - 벌크 [index](재색인 backfill): best-effort 라 실패를 로그만 남기고 삼킨다. 변경 필드 갱신을 위해 version 을 쓰지 않는다(제어된 배치, last-write-wins).
 * 색인 클라이언트 미설정(로컬·CI)이면 모든 경로가 no-op 다.
 */
@Component
class OpenSearchSavedPlaceIndexAdapter(
    @Qualifier(OpenSearchConfig.INDEX_CLIENT) private val clientProvider: ObjectProvider<OpenSearchClient>,
    properties: OpenSearchProperties,
) : SavedPlaceSearchIndexPort {
    private val log = KotlinLogging.logger {}
    private val indexAlias = properties.withPrefix("saved_place")

    override fun index(document: SavedPlaceIndexDocument) {
        // 단건 동기 색인 — external version = savedAt. 전송 실패는 던져 리스너가 폴백으로 재처리한다.
        val client = clientProvider.ifAvailable ?: return
        try {
            client.index { req ->
                req
                    .index(indexAlias)
                    .id("${document.userId}:${document.placeId}")
                    .requireAlias(true)
                    .versionType(VersionType.External)
                    .version(document.savedAt.toEpochMilli())
                    .document(source(document))
            }
        } catch (e: OpenSearchException) {
            if (e.isVersionConflict()) {
                log.debug { "saved_place 색인 version conflict(무시, 더 최신 존재): ${document.userId}:${document.placeId}" }
            } else {
                throw e
            }
        }
    }

    override fun index(documents: List<SavedPlaceIndexDocument>) {
        // 벌크 재색인(backfill) — version 없이 overwrite(변경 필드 갱신), best-effort.
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
                                    op
                                        .index(
                                            indexAlias,
                                        ).id("${document.userId}:${document.placeId}")
                                        .document(source(document))
                                }.build()
                        },
                    )
                }
            if (response.errors()) {
                val failed = response.items().count { it.error() != null }
                log.error { "saved_place 재색인 부분 실패(무시): $failed/${documents.size}건 실패" }
            }
        } catch (e: Exception) {
            log.error(e) { "saved_place 재색인 실패(무시): ${documents.size}건 — ${e.message}" }
        }
    }

    override fun delete(
        userId: Long,
        placeId: Long,
        versionEpochMs: Long,
    ) {
        // 단건 삭제는 동기 경로 — external version = removedAt. 늦게 온 과거 save 를 덮어쓰지 못하게 tombstone version 을 남긴다.
        // 없는 문서 삭제는 not_found 라 예외 아님(멱등). 전송 실패는 던져 리스너가 폴백으로 재처리한다.
        val client = clientProvider.ifAvailable ?: return
        try {
            client.delete { req ->
                req
                    .index(indexAlias)
                    .id("$userId:$placeId")
                    .versionType(VersionType.External)
                    .version(versionEpochMs)
            }
        } catch (e: OpenSearchException) {
            if (e.isVersionConflict()) {
                log.debug { "saved_place 삭제 version conflict(무시, 더 최신 존재): $userId:$placeId" }
            } else {
                throw e
            }
        }
    }

    private fun source(document: SavedPlaceIndexDocument): Map<String, Any?> =
        mapOf(
            "userId" to document.userId,
            "placeId" to document.placeId,
            "location" to mapOf("lat" to document.location.latitude, "lon" to document.location.longitude),
            "savedAt" to document.savedAt.toEpochMilli(),
            "name" to document.name,
            "category" to document.category,
            "imageUrl" to document.imageUrl,
            "rating" to document.rating,
            "ratingCount" to document.ratingCount,
        )

    private fun OpenSearchException.isVersionConflict(): Boolean = status() == 409
}
