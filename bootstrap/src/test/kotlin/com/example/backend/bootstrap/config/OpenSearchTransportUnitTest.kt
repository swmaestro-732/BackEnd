package com.example.backend.bootstrap.config

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opensearch.client.transport.aws.AwsSdk2Transport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport

/**
 * [OpenSearchConfig.openSearchTransport] 의 전송 생성 분기를 Spring 컨텍스트·네트워크 없이 직접 검증한다.
 * (통합테스트는 DB 가 필요하므로, auth 분기 로직은 이 순수 단위 테스트로 결정적으로 커버한다.)
 */
class OpenSearchTransportUnitTest {
    private fun config(
        authMode: String,
        region: String = "",
    ) = OpenSearchConfig(
        OpenSearchProperties(
            endpoint = "vpc-example.ap-northeast-2.es.amazonaws.com",
            username = "admin",
            password = "admin",
            authMode = authMode,
            region = region,
        ),
    )

    @Test
    fun `iam 모드 + region 이면 AwsSdk2Transport 를 만들고 close 가 정상 동작한다`() {
        val transport = config(authMode = "iam", region = "ap-northeast-2").openSearchTransport()
        assertInstanceOf(AwsSdk2Transport::class.java, transport)
        // close() 오버라이드(httpClient/credentialsProvider 정리) 가 예외 없이 수행되는지 — 리소스 누수 방지 경로 커버.
        transport.close()
    }

    @Test
    fun `iam 모드인데 region 이 공백이면 기동을 중단한다`() {
        assertThrows<IllegalArgumentException> {
            config(authMode = "iam", region = " ").openSearchTransport()
        }
    }

    @Test
    fun `기본 basic 모드면 ApacheHttpClient5Transport 를 만든다`() {
        val transport = config(authMode = "basic").openSearchTransport()
        assertInstanceOf(ApacheHttpClient5Transport::class.java, transport)
        transport.close()
    }

    @Test
    fun `basic iam 이 아닌 authMode 는 기동을 중단한다`() {
        assertThrows<IllegalArgumentException> {
            config(authMode = "sigv4", region = "ap-northeast-2").openSearchTransport()
        }
    }
}
