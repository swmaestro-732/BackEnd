package com.example.backend.bootstrap.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCredentials
import io.lettuce.core.RedisCredentialsProvider
import io.lettuce.core.RedisURI
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import java.time.Duration

@Configuration
@ConditionalOnExpression("'\${redis.endpoint:}'.trim().length() > 0")
class RedisConfig(
    private val properties: RedisProperties,
    private val tokenGenerator: ElastiCacheIamTokenGenerator,
) {
    @Bean
    fun redisCredentialsProvider(): RedisCredentialsProvider =
        RedisCredentialsProvider {
            Mono.fromSupplier { RedisCredentials.just(properties.user, tokenGenerator.generateToken()) }
        }

    @Bean(destroyMethod = "shutdown")
    fun redisClient(credentialsProvider: RedisCredentialsProvider): RedisClient {
        val uri =
            RedisURI
                .builder()
                .withHost(properties.endpoint.trim())
                .withPort(properties.port)
                .withSsl(true)
                .withAuthentication(credentialsProvider)
                // 기본 60초면 Redis 가 멈췄을 때 요청 스레드가 그만큼 묶인다 — 세션 조회 같은 사용자 경로는 짧게 끊고 503 으로 넘긴다.
                .withTimeout(COMMAND_TIMEOUT)
                .build()
        return RedisClient.create(uri)
    }

    private companion object {
        val COMMAND_TIMEOUT: Duration = Duration.ofSeconds(2)
    }
}
