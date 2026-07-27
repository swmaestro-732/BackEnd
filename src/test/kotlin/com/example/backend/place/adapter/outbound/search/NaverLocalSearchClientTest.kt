package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.NaverSearchProperties
import com.example.backend.place.domain.model.ExternalPlaceSource
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class NaverLocalSearchClientTest {
    private val builder = RestClient.builder().baseUrl("https://openapi.naver.com")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client =
        NaverLocalSearchClient(
            naverRestClient = builder.build(),
            naverSearchProperties = NaverSearchProperties(clientId = "id", clientSecret = "secret"),
        )

    @Test
    fun `제목 HTML 을 제거하고 좌표를 10^7 로 나눠 매핑한다`() {
        val body =
            """
            {"items":[
              {"title":"<b>어니언</b> 성수","category":"카페","address":"서울 성동구 성수동",
               "roadAddress":"서울 성동구 아차산로 5","telephone":"02-1234-5678",
               "mapx":"1270578000","mapy":"375445000"}
            ]}
            """.trimIndent()
        server
            .expect(requestTo(containsString("/v1/search/local.json")))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        val result = client.search("어니언", null)

        server.verify()
        assertEquals(1, result.size)
        val place = result.first()
        assertEquals("어니언 성수", place.name)
        assertEquals(127.0578, place.coordinate.longitude, 1e-9)
        assertEquals(37.5445, place.coordinate.latitude, 1e-9)
        assertEquals("서울 성동구 아차산로 5", place.roadAddress)
        assertEquals("02-1234-5678", place.telephone)
        assertEquals(ExternalPlaceSource.NAVER, place.source)
    }

    @Test
    fun `호출이 실패하면 빈 목록을 반환한다`() {
        server.expect(requestTo(containsString("/v1/search/local.json"))).andRespond(withServerError())

        val result = client.search("어니언", null)

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `clientId 가 비어 있으면 호출하지 않고 빈 목록을 반환한다`() {
        val blankClient =
            NaverLocalSearchClient(
                naverRestClient = builder.build(),
                naverSearchProperties = NaverSearchProperties(clientId = "", clientSecret = ""),
            )

        val result = blankClient.search("어니언", null)

        assertEquals(emptyList<Any>(), result)
    }
}
