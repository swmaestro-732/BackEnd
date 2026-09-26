package com.example.backend.user.adapter.outbound.social

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.domain.model.SocialProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.SocketTimeoutException

class NaverProfileClientTest {
    private val builder = RestClient.builder().baseUrl("https://openapi.naver.com")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client = NaverProfileClient(builder.build())

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "token\rvalue", "token\nvalue", "token\u0000value"])
    fun `빈 토큰과 제어문자 토큰은 HTTP 호출 없이 인증 실패로 거절한다`(token: String) {
        // HTTP 기대 요청이 없으므로 호출이 발생하면 MockRestServiceServer가 테스트를 실패시킨다.
        val exception = assertThrows<BusinessException> { client.verify(token) }

        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, exception.errorCode)
        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED.message, exception.message)
        server.verify()
    }

    @Test
    fun `Bearer 토큰으로 프로필을 조회하고 네이버 식별자만 사용한다`() {
        server
            .expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer naver-access-token"))
            .andRespond(
                withSuccess(
                    """{"resultcode":"00","message":"success","response":{"id":"naver-123","email":"ignored@example.com","nickname":"별명"}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val identity = client.verify("naver-access-token")

        assertEquals(SocialProvider.NAVER, identity.provider)
        assertEquals("naver-123", identity.socialId)
        server.verify()
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403])
    fun `인증 거절은 소셜 인증 실패로 변환한다`(status: Int) {
        server
            .expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andRespond(withStatus(HttpStatus.valueOf(status)))

        val exception = assertThrows<BusinessException> { client.verify("invalid-token") }

        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, exception.errorCode)
        server.verify()
    }

    @ParameterizedTest
    @ValueSource(ints = [429, 500, 502, 503])
    fun `제공자 장애와 호출 제한은 재시도 가능한 오류로 변환한다`(status: Int) {
        server
            .expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andRespond(withStatus(HttpStatus.valueOf(status)))

        assertUnavailable()
    }

    @ParameterizedTest
    @ValueSource(strings = ["024", "028", "403"])
    fun `프로필 응답의 인증 오류는 로그인 실패로 변환한다`(code: String) {
        server
            .expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andRespond(
                withSuccess("""{"resultcode":"$code","message":"authentication failed"}""", MediaType.APPLICATION_JSON),
            )

        val exception = assertThrows<BusinessException> { client.verify("invalid-token") }

        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, exception.errorCode)
        server.verify()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "not-json",
            "{}",
            "{\"resultcode\":\"00\"}",
            "{\"resultcode\":\"00\",\"response\":{}}",
            "{\"resultcode\":\"00\",\"response\":{\"id\":\" \"}}",
            "{\"resultcode\":\"00\",\"response\":{\"id\":null}}",
            "{\"resultcode\":\"999\",\"response\":{\"id\":\"naver-123\"}}",
        ],
    )
    fun `잘못된 성공 응답으로는 인증하지 않는다`(body: String) {
        server
            .expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        assertUnavailable()
    }

    @Test
    fun `네트워크 타임아웃은 재시도 가능한 오류로 변환한다`() {
        server
            .expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andRespond(withException(SocketTimeoutException("timed out")))

        assertUnavailable()
    }

    private fun assertUnavailable() {
        val exception = assertThrows<BusinessException> { client.verify("naver-access-token") }
        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_UNAVAILABLE, exception.errorCode)
        assertEquals(503, exception.errorCode.status)
        server.verify()
    }
}
