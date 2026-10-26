package com.example.backend.crawl.adapter.inbound.messaging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class CrawlResultConsumerWarningTest {
    private val logger = LoggerFactory.getLogger(CrawlResultConsumerWarning::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()
    private val runner = ApplicationContextRunner().withUserConfiguration(CrawlResultConsumerWarning::class.java)

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

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `큐 URL만 설정되면 기동 시 WARN을 남긴다`(curator: String) {
        runner.withPropertyValues(QUEUE, "crawler.curator-user-id=$curator").run {
            assertThat(it).hasSingleBean(CrawlResultConsumerWarning::class.java)
            assertThat(appender.list).hasSize(1)
            assertThat(appender.list.single().level).isEqualTo(Level.WARN)
            assertThat(appender.list.single().formattedMessage).contains("crawler.curator-user-id", "disabled")
        }
    }

    @Test
    fun `큐 URL이 없으면 경고 컴포넌트를 만들지 않는다`() {
        runner.run {
            assertThat(it).doesNotHaveBean(CrawlResultConsumerWarning::class.java)
            assertThat(appender.list).isEmpty()
        }
    }

    @Test
    fun `큐 URL이 공백이면 경고 컴포넌트를 만들지 않는다`() {
        runner.withPropertyValues("aws.sqs.crawl-results-queue-url=   ").run {
            assertThat(it).doesNotHaveBean(CrawlResultConsumerWarning::class.java)
            assertThat(appender.list).isEmpty()
        }
    }

    @Test
    fun `큐와 큐레이터가 모두 설정되면 컴포넌트는 만들되 경고하지 않는다`() {
        runner.withPropertyValues(QUEUE, "crawler.curator-user-id=2").run {
            assertThat(it).hasSingleBean(CrawlResultConsumerWarning::class.java)
            assertThat(appender.list).isEmpty()
        }
    }

    private companion object {
        const val QUEUE = "aws.sqs.crawl-results-queue-url=https://sqs.example/crawl-results"
    }
}
