package com.example.backend.crawl.adapter.inbound.messaging

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component

@Component
@ConditionalOnExpression("'\${aws.sqs.crawl-results-queue-url:}'.trim().length() > 0")
class CrawlResultConsumerWarning(
    @Value("\${crawler.curator-user-id:}") private val curatorUserId: String,
) {
    private val log = KotlinLogging.logger {}

    @PostConstruct
    fun warnIfDisabled() {
        if (curatorUserId.isBlank()) {
            log.warn { "Crawl result consumer disabled: queue URL is set but crawler.curator-user-id is blank" }
        }
    }
}
