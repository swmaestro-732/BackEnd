package com.example.backend.crawl.adapter.inbound.messaging

import com.example.backend.bootstrap.config.CrawlerProperties
import com.example.backend.crawl.application.port.inbound.CrawlImportUseCase
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

@Component
@ConditionalOnExpression("'\${aws.sqs.crawl-results-queue-url:}'.trim().length() > 0")
class CrawlResultSqsConsumer(
    private val crawlImportUseCase: CrawlImportUseCase,
    properties: CrawlerProperties,
) {
    init {
        require(properties.curatorUserId != null && properties.curatorUserId > 0) {
            "크롤링 수신 활성화 시 crawler.curator-user-id에 유효한 사용자 ID가 필요합니다."
        }
    }

    // 예외는 전파해 ack하지 않는다. DLQ의 maxReceiveCount=5는 큐의 redrive policy에서 설정한다.
    @SqsListener(
        value = ["\${aws.sqs.crawl-results-queue-url}"],
        maxConcurrentMessages = "1",
        maxMessagesPerPoll = "1",
    )
    fun onMessage(message: CrawlResultCommand) {
        crawlImportUseCase.importResult(message)
    }
}
