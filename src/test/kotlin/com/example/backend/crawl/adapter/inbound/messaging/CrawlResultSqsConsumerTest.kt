package com.example.backend.crawl.adapter.inbound.messaging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.example.backend.bootstrap.config.CrawlerProperties
import com.example.backend.crawl.application.port.inbound.CrawlImportUseCase
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import com.example.backend.user.application.port.inbound.UserUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class CrawlResultSqsConsumerTest {
    private val logger = LoggerFactory.getLogger(CrawlResultSqsConsumer::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()
    private val users = mock(UserUseCase::class.java)
    private val properties = CrawlerProperties(curatorUserId = 2L)
    private val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val imported = mutableListOf<CrawlResultCommand>()
    private val importUseCase =
        object : CrawlImportUseCase {
            override fun importResult(command: CrawlResultCommand) {
                imported += command
            }
        }

    @BeforeEach
    fun attach() {
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun detach() {
        logger.detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `크롤러 JSON의 isCourse와 빈 선택 필드를 읽어 유스케이스로 전달한다`() {
        `when`(users.isActive(2L)).thenReturn(true)
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

        consumer.validateCurator()
        consumer.onMessage(message)

        assertFalse(received!!.isCourse)
        assertEquals(emptyList<String>(), received!!.tags)
        assertEquals("C9xYz", received!!.shortcode)
    }

    @Test
    fun `처리 실패는 삼키지 않아 SQS가 재전달한다`() {
        `when`(users.isActive(2L)).thenReturn(true)
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

        consumer.validateCurator()
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
    fun `큐레이터가 존재하지 않거나 비활성이면 기동은 허용하고 메시지는 처리하지 않는다`() {
        `when`(users.isActive(2L)).thenReturn(false)

        runner().withPropertyValues(QUEUE, "crawler.curator-user-id=2").run {
            assertThat(it).hasNotFailed()
            val consumer = it.getBean(CrawlResultSqsConsumer::class.java)
            repeat(2) {
                assertThrows(IllegalStateException::class.java) {
                    consumer.onMessage(CrawlResultCommand("run", "post", "url", "account", false))
                }
            }
            assertTrue(imported.isEmpty())
            assertThat(appender.list).hasSize(1)
            assertThat(appender.list.single().level).isEqualTo(Level.ERROR)
            assertThat(appender.list.single().formattedMessage).contains("disabled", "2", "missing or inactive")
        }
        verify(users, times(1)).isActive(2L)
    }

    @Test
    fun `큐레이터 ID가 없으면 검증은 실패하지 않고 메시지를 처리하지 않는다`() {
        val consumer = CrawlResultSqsConsumer(importUseCase, users, CrawlerProperties())

        assertDoesNotThrow { consumer.validateCurator() }
        assertThrows(IllegalStateException::class.java) {
            consumer.onMessage(CrawlResultCommand("run", "post", "url", "account", false))
        }
        assertTrue(imported.isEmpty())
    }

    private fun runner() =
        ApplicationContextRunner()
            .withBean(UserUseCase::class.java, { users })
            .withBean(CrawlerProperties::class.java, { properties })
            .withBean(CrawlImportUseCase::class.java, { importUseCase })
            .withUserConfiguration(CrawlResultSqsConsumer::class.java)

    private companion object {
        const val QUEUE = "aws.sqs.crawl-results-queue-url=https://sqs.example/crawl-results"
    }
}
