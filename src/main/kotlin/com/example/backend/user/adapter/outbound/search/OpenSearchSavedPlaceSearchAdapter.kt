package com.example.backend.user.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.inbound.SavedPlaceSearchQueryPort
import com.example.backend.user.application.port.inbound.dto.NearbySavedHit
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.DistanceUnit
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.GeoDistanceType
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/** 사용자·반경을 쿼리에서 먼저 제한한 뒤 거리순으로 조회한다. 엔진 장애는 홈 근처 섹션에만 한정한다. */
@Component
class OpenSearchSavedPlaceSearchAdapter(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
    properties: OpenSearchProperties,
) : SavedPlaceSearchQueryPort {
    private val log = KotlinLogging.logger {}
    private val indexAlias = properties.withPrefix("saved_place")

    override fun nearestSaved(
        userId: Long,
        origin: Coordinate,
        radiusMeters: Double,
        size: Int,
    ): List<NearbySavedHit> {
        val client = clientProvider.ifAvailable
        if (client == null) {
            log.warn { "saved_place 검색엔진 미설정 — 빈 목록으로 대체" }
            return emptyList()
        }
        return try {
            val request =
                SearchRequest
                    .Builder()
                    .index(indexAlias)
                    .size(size)
                    .source {
                        it.filter { f ->
                            f.includes("placeId", "name", "category", "imageUrl", "rating", "ratingCount")
                        }
                    }.query { q ->
                        q.bool { b ->
                            b
                                .filter { f -> f.term { t -> t.field("userId").value(FieldValue.of(userId)) } }
                                .filter { f ->
                                    f.geoDistance { g ->
                                        g
                                            .field("location")
                                            .distance("${radiusMeters}m")
                                            .location { l ->
                                                l.latlon { ll -> ll.lat(origin.latitude).lon(origin.longitude) }
                                            }.distanceType(GeoDistanceType.Arc)
                                    }
                                }
                        }
                    }.sort { s ->
                        s.geoDistance { g ->
                            g
                                .field("location")
                                .location { l -> l.latlon { ll -> ll.lat(origin.latitude).lon(origin.longitude) } }
                                .order(SortOrder.Asc)
                                .distanceType(GeoDistanceType.Arc)
                                .unit(DistanceUnit.Meters)
                        }
                    }.sort { s -> s.doc { it.order(SortOrder.Asc) } }
                    .build()
            val response = client.search(request, JsonData::class.java)
            check(!response.timedOut() && response.shards().failed() == 0) { "저장 장소 검색이 일부만 완료되었습니다." }
            response.hits().hits().map { hit ->
                val source = checkNotNull(hit.source()).toJson().asJsonObject()
                NearbySavedHit(
                    placeId = source.getJsonNumber("placeId").longValue(),
                    distanceMeters = hit.sort().first().toDouble(),
                    name = source.getString("name"),
                    category = source.getString("category"),
                    imageUrl =
                        source
                            .takeIf {
                                it.containsKey(
                                    "imageUrl",
                                ) && !it.isNull("imageUrl")
                            }?.getString("imageUrl"),
                    rating = source.getJsonNumber("rating").doubleValue(),
                    ratingCount = source.getJsonNumber("ratingCount").intValue(),
                )
            }
        } catch (e: Exception) {
            log.warn(e) { "saved_place 거리순 검색 실패 — 빈 목록으로 대체" }
            emptyList()
        }
    }
}
