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

/** DB 가 원본이며 색인 오류는 로그만 남긴다. 단건 동기화와 재색인은 같은 alias 에 멱등 upsert 한다. */
@Component
class OpenSearchSavedPlaceIndexAdapter(
    @Qualifier(OpenSearchConfig.INDEX_CLIENT) private val clientProvider: ObjectProvider<OpenSearchClient>,
    properties: OpenSearchProperties,
) : SavedPlaceSearchIndexPort {
    private val log = KotlinLogging.logger {}
    private val indexAlias = properties.withPrefix("saved_place")

    override fun index(document: SavedPlaceIndexDocument) = index(listOf(document))

    override fun index(documents: List<SavedPlaceIndexDocument>) {
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
                                            "visited" to document.visited,
                                            "savedAt" to document.savedAt.toEpochMilli(),
                                        ),
                                    )
                                }.build()
                        },
                    )
                }
            if (response.errors()) {
                val failed = response.items().count { it.error() != null }
                log.error { "saved_place 색인 부분 실패(무시): $failed/${documents.size}건 실패" }
            }
        } catch (e: Exception) {
            log.error(e) { "saved_place 색인 실패(무시): ${documents.size}건 — ${e.message}" }
        }
    }

    override fun delete(
        userId: Long,
        placeId: Long,
    ) {
        val client = clientProvider.ifAvailable ?: return
        try {
            client.delete { it.index(indexAlias).id("$userId:$placeId") }
        } catch (e: Exception) {
            log.error(e) { "saved_place 삭제 실패(무시): $userId:$placeId — ${e.message}" }
        }
    }
}
