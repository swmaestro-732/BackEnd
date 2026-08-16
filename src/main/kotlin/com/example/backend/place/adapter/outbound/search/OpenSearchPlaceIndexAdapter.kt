package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import com.example.backend.place.domain.model.Place
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [PlaceSearchIndexPort] 를 OpenSearch bulk 색인으로 구현한다.
 *
 * [OpenSearchClient] 가 없으면(=opensearch.endpoint 미주입, 로컬·CI) no-op, 색인 중 예외는 warn 로그만
 * 남기고 삼킨다(fail-soft) — 검색은 부가 기능이라 장소 저장 흐름을 막지 않는다. alias `place` 에 upsert 한다.
 */
@Component
class OpenSearchPlaceIndexAdapter(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
) : PlaceSearchIndexPort {
    private val log = KotlinLogging.logger {}

    override fun index(places: List<Place>) {
        if (places.isEmpty()) return
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op

        try {
            val documents = places.filter { it.id != null }
            if (documents.isEmpty()) return
            val response =
                client.bulk { bulk ->
                    bulk.operations(
                        documents.map { place ->
                            val document = place.toDocument()
                            org.opensearch.client.opensearch.core.bulk.BulkOperation
                                .Builder()
                                .index { op -> op.index(INDEX_ALIAS).id(place.id.toString()).document(document) }
                                .build()
                        },
                    )
                }
            // bulk 는 예외 없이 200 을 주면서 개별 문서만 거부될 수 있다(매핑 충돌 등) → 항목별 실패를 집계해 로그로 드러낸다.
            if (response.errors()) {
                val failed = response.items().count { it.error() != null }
                log.warn { "place 색인 부분 실패(무시): $failed/${documents.size}건 실패" }
            }
        } catch (e: Exception) {
            log.warn { "place 색인 실패(무시): ${places.size}건 — ${e.message}" }
        }
    }

    private fun Place.toDocument(): PlaceDocument =
        PlaceDocument(
            name = name,
            description = description,
            category = category.name,
            address = address,
            areaCode = areaCode,
            location = GeoLocation(lat = location.latitude, lon = location.longitude),
            status = status.name,
        )

    private companion object {
        const val INDEX_ALIAS = "place"
    }
}
