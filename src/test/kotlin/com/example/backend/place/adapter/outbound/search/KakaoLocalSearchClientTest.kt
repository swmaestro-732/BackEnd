package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.KakaoLocalProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.place.domain.model.ExternalPlaceSource
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class KakaoLocalSearchClientTest {
    private val builder = RestClient.builder().baseUrl("https://dapi.kakao.com")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client =
        KakaoLocalSearchClient(
            kakaoRestClient = builder.build(),
            kakaoLocalProperties = KakaoLocalProperties(restKey = "rest-key"),
        )

    @Test
    fun `documents 를 좌표와 함께 매핑하고 Authorization 헤더를 붙인다`() {
        val body =
            """
            {"documents":[
              {"place_name":"콤포트 성수","category_name":"음식점 > 카페",
               "road_address_name":"서울 성동구 서울숲2길 3","address_name":"서울 성동구 성수동1가",
               "x":"127.0561","y":"37.5432","phone":"02-9876-5432"}
            ]}
            """.trimIndent()
        server
            .expect(requestTo(containsString("/v2/local/search/keyword.json")))
            .andExpect(header("Authorization", "KakaoAK rest-key"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        val result = client.search("콤포트", Coordinate(latitude = 37.5432, longitude = 127.0561))

        server.verify()
        assertEquals(1, result.size)
        val place = result.first()
        assertEquals("콤포트 성수", place.name)
        assertEquals("음식점 > 카페", place.category)
        assertEquals("서울 성동구 서울숲2길 3", place.roadAddress)
        assertEquals(127.0561, place.coordinate.longitude, 1e-9)
        assertEquals(37.5432, place.coordinate.latitude, 1e-9)
        assertEquals("02-9876-5432", place.telephone)
        assertEquals(ExternalPlaceSource.KAKAO, place.source)
    }

    @Test
    fun `호출이 실패하면 빈 목록을 반환한다(fail-soft)`() {
        server
            .expect(requestTo(containsString("/v2/local/search/keyword.json")))
            .andRespond(withServerError())

        val result = client.search("콤포트", null)

        server.verify()
        assertEquals(emptyList<Any>(), result)
    }
}
