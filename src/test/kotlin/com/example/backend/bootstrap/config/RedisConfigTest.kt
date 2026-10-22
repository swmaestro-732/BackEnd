package com.example.backend.bootstrap.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCredentialsProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class RedisConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(
                PropertiesConfig::class.java,
                RedisConfig::class.java,
                ElastiCacheIamTokenGenerator::class.java,
            )

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["   "])
    fun `endpoint 가 없거나 공백이면 Redis 빈을 생성하지 않는다`(endpoint: String?) {
        val runner =
            when (endpoint) {
                null -> contextRunner
                else -> contextRunner.withPropertyValues("redis.endpoint=$endpoint")
            }

        runner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).doesNotHaveBean(RedisClient::class.java)
            assertThat(context).doesNotHaveBean(RedisCredentialsProvider::class.java)
        }
    }

    @Test
    fun `설정 바인딩과 클라이언트 생성은 실제 Redis 연결 없이 완료된다`() {
        contextRunner
            .withPropertyValues(
                "redis.endpoint= localhost ",
                "redis.port=6380",
                "redis.user=iam-user",
                "redis.cache-name=My-Cache",
                "redis.region=ap-northeast-2",
            ).run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(RedisClient::class.java)
                val credentials = context.getBean(RedisCredentialsProvider::class.java).resolveCredentials().block()!!
                assertThat(credentials.username).isEqualTo("iam-user")
                val uri = URI.create("http://${String(credentials.password)}")
                val query = URLDecoder.decode(uri.rawQuery, StandardCharsets.UTF_8)
                assertThat(uri.host).isEqualTo("my-cache")
                assertThat(uri.path).isEqualTo("/")
                assertThat(query).contains("Action=connect", "User=iam-user", "X-Amz-Expires=900")
                assertThat(query).contains("/ap-northeast-2/elasticache/aws4_request", "X-Amz-SignedHeaders=host")
                assertThat(query).containsPattern("X-Amz-Signature=[0-9a-f]{64}")
                assertThat(query).doesNotContain("ResourceType=")
            }
    }

    @Test
    fun `자격증명을 구독할 때마다 새 토큰을 생성한다`() {
        val generator = mock(ElastiCacheIamTokenGenerator::class.java)
        `when`(generator.generateToken()).thenReturn("first-token", "second-token")
        val provider = RedisConfig(RedisProperties(user = "iam-user"), generator).redisCredentialsProvider()
        val credentials = provider.resolveCredentials()

        assertThat(String(credentials.block()!!.password)).isEqualTo("first-token")
        assertThat(String(credentials.block()!!.password)).isEqualTo("second-token")
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RedisProperties::class)
    class PropertiesConfig
}
