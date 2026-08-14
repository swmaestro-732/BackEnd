package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.application.port.outbound.PlaceSearchIndexPort
import com.example.backend.place.domain.model.Place
import org.opensearch.client.opensearch.OpenSearchClient
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    override fun index(places: List<Place>) {
        if (places.isEmpty()) return
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op

        try {
            val documents = places.filter { it.id != null }
            if (documents.isEmpty()) return
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
        } catch (e: Exception) {
            log.warn("place 색인 실패(무시): {}건 — {}", places.size, e.message)
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

/** OpenSearch 색인 문서 — place 인덱스 매핑(opensearch/place.json)과 필드가 일치한다. */
data class PlaceDocument(
    val name: String,
    val description: String?,
    val category: String,
    val address: String,
    val areaCode: String?,
    val location: GeoLocation,
    val status: String,
)

/** geo_point 직렬화용 — OpenSearch 는 {lat, lon} 형태를 geo_point 로 받는다. */
data class GeoLocation(
    val lat: Double,
    val lon: Double,
)
