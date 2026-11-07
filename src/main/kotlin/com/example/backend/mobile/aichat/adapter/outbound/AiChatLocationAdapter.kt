package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.aichat.application.port.outbound.AiChatLocationPort
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.GeoLocation
import org.opensearch.client.opensearch._types.SortOrder
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — "내 근처" 좌표를 법정동코드로 바꾼다. 행정구역 경계 데이터가 없어 place 색인의
 * 가까운 장소들(반경 1.5km, 거리순 20건)이 가진 areaCode 중 가장 많은 값을 쓴다.
 * 엔진이 없거나(로컬·CI) 근처에 장소가 없으면 null — 호출자가 지역 칩을 다시 안내한다.
 */
@Component
class AiChatLocationAdapter(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
    properties: OpenSearchProperties,
) : AiChatLocationPort {
    private val log = KotlinLogging.logger {}
    private val indexAlias = properties.withPrefix("place")

    override fun findAreaCodeAt(location: Coordinate): String? {
        val client = clientProvider.ifAvailable ?: return null
        val point = GeoLocation.of { g -> g.latlon { l -> l.lat(location.latitude).lon(location.longitude) } }
        return try {
            val response =
                client.search(
                    { s ->
                        s
                            .index(indexAlias)
                            .size(SAMPLE_SIZE)
                            .source { src -> src.filter { f -> f.includes(AREA_CODE_FIELD) } }
                            .query { q ->
                                q.bool { b ->
                                    b
                                        .filter { f -> f.exists { e -> e.field(AREA_CODE_FIELD) } }
                                        .filter { f ->
                                            f.geoDistance { g ->
                                                g.field(LOCATION_FIELD).distance(RADIUS).location(point)
                                            }
                                        }
                                }
                            }.sort { so ->
                                so.geoDistance { g -> g.field(LOCATION_FIELD).location(point).order(SortOrder.Asc) }
                            }
                    },
                    AreaCodeSource::class.java,
                )
            response
                .hits()
                .hits()
                .mapNotNull { it.source()?.areaCode }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
        } catch (e: Exception) {
            log.warn { "ai-chat 좌표 → 지역 판정 실패: ${e.message}" }
            null
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AreaCodeSource(
        val areaCode: String? = null,
    )

    private companion object {
        const val AREA_CODE_FIELD = "areaCode"
        const val LOCATION_FIELD = "location"
        const val RADIUS = "1500m"
        const val SAMPLE_SIZE = 20
    }
}
