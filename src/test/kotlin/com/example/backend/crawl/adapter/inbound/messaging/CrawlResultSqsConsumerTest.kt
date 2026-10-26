package com.example.backend.crawl.adapter.inbound.messaging

import com.example.backend.crawl.application.port.inbound.CrawlImportUseCase
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class CrawlResultSqsConsumerTest {
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
            )
        val message = CrawlResultCommand("run", "post", "url", "account", false)

        assertSame(failure, assertThrows(IllegalStateException::class.java) { consumer.onMessage(message) })
    }

    @Test
    fun `큐 URL과 큐레이터 ID가 모두 있을 때만 소비자를 만든다`() {
        val runner =
            ApplicationContextRunner()
                .withBean(
                    CrawlImportUseCase::class.java,
                    {
                        object : CrawlImportUseCase {
                            override fun importResult(command: CrawlResultCommand) = Unit
                        }
                    },
                ).withUserConfiguration(CrawlResultSqsConsumer::class.java)
        val queue = "aws.sqs.crawl-results-queue-url=https://sqs.example/crawl-results"

        runner.withPropertyValues(queue, "crawler.curator-user-id=2").run {
            assertEquals(1, it.getBeanNamesForType(CrawlResultSqsConsumer::class.java).size)
        }
        runner.withPropertyValues(queue).run {
            assertEquals(0, it.getBeanNamesForType(CrawlResultSqsConsumer::class.java).size)
        }
        runner.withPropertyValues("crawler.curator-user-id=2").run {
            assertEquals(0, it.getBeanNamesForType(CrawlResultSqsConsumer::class.java).size)
        }
    }
}
