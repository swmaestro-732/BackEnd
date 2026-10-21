package com.example.backend.bootstrap.config

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestPropertySource

/**
 * iam 모드에 assume-role-arn 을 주면 STS assume 자격증명으로 클라이언트 빈이 만들어지는지 검증한다(SCRUM-675).
 * 클라이언트 생성은 STS 를 호출하지 않으므로 자격증명 없는 로컬, CI 에서도 부팅된다.
 */
@TestPropertySource(
    properties = [
        "opensearch.endpoint=localhost",
        "opensearch.auth-mode=iam",
        "opensearch.region=ap-northeast-2",
        "opensearch.assume-role-arn=arn:aws:iam::123456789012:role/test-opensearch-admin",
    ],
)
class OpenSearchConfigAssumeRoleTest
    @Autowired
    constructor(
        private val context: ApplicationContext,
    ) : IntegrationTestBase() {
        @Test
        fun `assume-role-arn 이 있어도 검색, 색인 클라이언트 빈이 생성된다`() {
            assertTrue(context.getBeanNamesForType(OpenSearchClient::class.java).size >= 2)
        }
    }
