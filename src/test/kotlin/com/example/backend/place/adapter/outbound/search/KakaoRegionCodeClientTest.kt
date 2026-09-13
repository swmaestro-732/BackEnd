package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.KakaoLocalProperties
import com.example.backend.common.geo.Coordinate
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class KakaoRegionCodeClientTest {
    private val builder = RestClient.builder().baseUrl("https://dapi.kakao.com")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client =
        KakaoRegionCodeClient(
            kakaoRestClient = builder.build(),
            kakaoLocalProperties = KakaoLocalProperties(restKey = "test-key"),
        )

    private val coord = Coordinate(latitude = 37.5445, longitude = 127.0578)

    @Test
    fun `법정동(B) 문서의 10자리 code 를 반환한다`() {
        val body =
            """{"documents":[
                {"region_type":"H","code":"1234567890"},
                {"region_type":"B","code":"1168010100"}
            ]}"""
        server
            .expect(requestTo(containsString("/v2/local/geo/coord2regioncode.json")))
            .andExpect(header("Authorization", "KakaoAK test-key"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        val result = client.findLegalDongCode(coord)

        server.verify()
        assertEquals("1168010100", result)
    }

    @Test
    fun `B 타입 문서가 없으면 null 을 반환한다`() {
        val body = """{"documents":[{"region_type":"H","code":"1168010100"}]}"""
        server
            .expect(requestTo(containsString("/v2/local/geo/coord2regioncode.json")))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        assertNull(client.findLegalDongCode(coord))
        server.verify()
    }

    @Test
    fun `code 가 10자리 숫자가 아니면 null 을 반환한다`() {
        val body = """{"documents":[{"region_type":"B","code":"SHORT"}]}"""
        server
            .expect(requestTo(containsString("/v2/local/geo/coord2regioncode.json")))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        assertNull(client.findLegalDongCode(coord))
        server.verify()
    }

    @Test
    fun `documents 가 비어 있으면 null 을 반환한다`() {
        val body = """{"documents":[]}"""
        server
            .expect(requestTo(containsString("/v2/local/geo/coord2regioncode.json")))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        assertNull(client.findLegalDongCode(coord))
        server.verify()
    }

    @Test
    fun `호출 실패 시 null 을 반환한다(fail-soft)`() {
        server
            .expect(requestTo(containsString("/v2/local/geo/coord2regioncode.json")))
            .andRespond(withServerError())

        assertNull(client.findLegalDongCode(coord))
        server.verify()
    }
}
