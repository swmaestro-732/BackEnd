package com.example.backend.bootstrap.config

import com.example.backend.bootstrap.search.OpenSearchIndexInitializer
import com.example.backend.bootstrap.search.OpenSearchReindexRunner
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.aws.AwsSdk2Transport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean

/** IAM 모드에서 실제 OpenSearch 연결 없이 SigV4 transport 와 클라이언트 빈이 생성되는지 검증한다. */
@TestPropertySource(
    properties = [
        "opensearch.endpoint= https://localhost:1 ",
        "opensearch.auth-mode=iam",
        "opensearch.region=ap-northeast-2",
    ],
)
// 부팅 시 OpenSearch 를 호출하는 초기화·재색인만 막고, 실제 transport 와 클라이언트를 생성한다.
@MockitoBean(types = [OpenSearchIndexInitializer::class, OpenSearchReindexRunner::class])
class OpenSearchIamConfigTest
    @Autowired
    constructor(
        private val context: ApplicationContext,
    ) : IntegrationTestBase() {
        @Test
        fun `iam 모드에서 네트워크 연결 없이 OpenSearchClient 빈이 생성된다`() {
            assertTrue(context.getBeanNamesForType(OpenSearchClient::class.java).isNotEmpty())
            assertInstanceOf(AwsSdk2Transport::class.java, context.getBean(OpenSearchTransport::class.java))
        }
    }
