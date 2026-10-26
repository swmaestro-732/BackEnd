package com.example.backend.crawl.adapter.inbound.messaging

import com.example.backend.bootstrap.config.CrawlerProperties
import com.example.backend.crawl.application.port.inbound.CrawlImportUseCase
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import com.example.backend.user.application.port.inbound.UserUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class CrawlResultSqsConsumerTest {
    private val users = mock(UserUseCase::class.java)
    private val properties = CrawlerProperties(curatorUserId = 2L)
    private val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @Test
    fun `크롤러 JSON의 isCourse와 빈 선택 필드를 읽어 유스케이스로 전달한다`() {
        val message =
            mapper.readValue(
                """{"runId":"20261027-0400","shortcode":"C9xYz","sourceUrl":"https://www.instagram.com/p/C9xYz/",
                "account":"eateat.mag","isCourse":false,"courseReason":"not course","places":[]}""",
                CrawlResultCommand::class.java,
            )
        var received: CrawlResultCommand? = null
        val consumer =
            CrawlResultSqsConsumer(
                object : CrawlImportUseCase {
                    override fun importResult(command: CrawlResultCommand) {
                        received = command
                    }
                },
                users,
                properties,
            )

        consumer.onMessage(message)

        assertFalse(received!!.isCourse)
        assertEquals(emptyList<String>(), received!!.tags)
        assertEquals("C9xYz", received!!.shortcode)
    }

    @Test
    fun `처리 실패는 삼키지 않아 SQS가 재전달한다`() {
        val failure = IllegalStateException("database unavailable")
        val consumer =
            CrawlResultSqsConsumer(
                object : CrawlImportUseCase {
                    override fun importResult(command: CrawlResultCommand): Unit = throw failure
                },
                users,
                properties,
            )
        val message = CrawlResultCommand("run", "post", "url", "account", false)

        assertSame(failure, assertThrows(IllegalStateException::class.java) { consumer.onMessage(message) })
    }

    @Test
    fun `큐 URL과 큐레이터 ID가 모두 있을 때만 소비자를 만들고 한 번 검증한다`() {
        `when`(users.isActive(2L)).thenReturn(true)

        runner().withPropertyValues(QUEUE, "crawler.curator-user-id=2").run {
            assertThat(it).hasSingleBean(CrawlResultSqsConsumer::class.java)
            it.getBean(CrawlResultSqsConsumer::class.java).onMessage(
                CrawlResultCommand("run", "post", "url", "account", false),
            )
        }
        runner().withPropertyValues(QUEUE).run {
            assertThat(it).doesNotHaveBean(CrawlResultSqsConsumer::class.java)
        }
        runner().withPropertyValues(QUEUE, "crawler.curator-user-id=   ").run {
            assertThat(it).doesNotHaveBean(CrawlResultSqsConsumer::class.java)
        }
        runner().withPropertyValues("crawler.curator-user-id=2").run {
            assertThat(it).doesNotHaveBean(CrawlResultSqsConsumer::class.java)
        }
        verify(users, times(1)).isActive(2L)
    }

    @Test
    fun `큐레이터가 존재하지 않거나 비활성이면 명확한 오류로 기동을 중단한다`() {
        `when`(users.isActive(2L)).thenReturn(false)

        runner().withPropertyValues(QUEUE, "crawler.curator-user-id=2").run {
            assertThat(it).hasFailed()
            assertThat(it.startupFailure)
                .hasRootCauseMessage("crawler.curator-user-id=2 must identify an existing active user")
        }
        verify(users, times(1)).isActive(2L)
    }

    private fun runner() =
        ApplicationContextRunner()
            .withBean(UserUseCase::class.java, { users })
            .withBean(CrawlerProperties::class.java, { properties })
            .withBean(
                CrawlImportUseCase::class.java,
                {
                    object : CrawlImportUseCase {
                        override fun importResult(command: CrawlResultCommand) = Unit
                    }
                },
            ).withUserConfiguration(CrawlResultSqsConsumer::class.java)

    private companion object {
        const val QUEUE = "aws.sqs.crawl-results-queue-url=https://sqs.example/crawl-results"
    }
}
