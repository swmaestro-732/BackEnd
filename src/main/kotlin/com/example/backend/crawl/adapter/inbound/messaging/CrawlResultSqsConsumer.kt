package com.example.backend.crawl.adapter.inbound.messaging

import com.example.backend.bootstrap.config.CrawlerProperties
import com.example.backend.crawl.application.port.inbound.CrawlImportUseCase
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import com.example.backend.user.application.port.inbound.UserUseCase
import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.stereotype.Component

/**
 * 크롤링 결과 큐 수신. 큐 URL과 큐레이터 사용자 ID가 모두 설정된 배포에서만 활성화한다 —
 * 큐레이터 누락은 별도 컴포넌트가 경고하며, 설정된 큐레이터가 비활성이면 소비자만 비활성화한다.
 */
@Component
@DependsOnDatabaseInitialization
@ConditionalOnExpression(
    "'\${aws.sqs.crawl-results-queue-url:}'.trim().length() > 0 && '\${crawler.curator-user-id:}'.trim().length() > 0",
)
class CrawlResultSqsConsumer(
    private val crawlImportUseCase: CrawlImportUseCase,
    private val users: UserUseCase,
    private val properties: CrawlerProperties,
) {
    private val log = KotlinLogging.logger {}
    private var enabled = false

    @PostConstruct
    fun validateCurator() {
        val curatorId = properties.curatorUserId
        enabled = curatorId != null && users.isActive(curatorId)
        if (!enabled) {
            log.error { "Crawl result consumer disabled: curator user $curatorId missing or inactive" }
        }
    }

    // 예외는 전파해 ack하지 않는다. DLQ의 maxReceiveCount=5는 큐의 redrive policy에서 설정한다.
    @SqsListener(
        value = ["\${aws.sqs.crawl-results-queue-url}"],
        maxConcurrentMessages = "1",
        maxMessagesPerPoll = "1",
    )
    fun onMessage(message: CrawlResultCommand) {
        check(enabled) { "Crawl result consumer disabled" }
        crawlImportUseCase.importResult(message)
    }
}
