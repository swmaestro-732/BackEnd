package com.example.backend.bootstrap.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCredentials
import io.lettuce.core.RedisCredentialsProvider
import io.lettuce.core.RedisURI
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

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
                .build()
        return RedisClient.create(uri)
    }
}
