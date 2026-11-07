package com.example.backend.bootstrap.config

import com.mongodb.AuthenticationMechanism
import com.mongodb.MongoClientSettings
import com.mongodb.ReadPreference
import com.mongodb.ServerAddress
import com.mongodb.client.MongoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory

class MongoConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withPropertyValues("DOCDB_ENDPOINT=")
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withUserConfiguration(AutoConfig::class.java, MongoConfig::class.java)

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["   "])
    fun `endpoint 가 없거나 공백이면 Mongo 빈을 생성하지 않는다`(endpoint: String?) {
        val runner =
            when (endpoint) {
                null -> contextRunner
                else -> contextRunner.withPropertyValues("docdb.endpoint=$endpoint")
            }

        runner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).doesNotHaveBean(MongoClient::class.java)
            assertThat(context).doesNotHaveBean(MongoClientSettings::class.java)
            assertThat(context).doesNotHaveBean(MongoDatabaseFactory::class.java)
        }
    }

    @Test
    fun `IAM 과 TLS 설정은 실제 DocumentDB 연결 없이 구성된다`() {
        val settings =
            MongoConfig(
                DocumentDbProperties(endpoint = " docdb.example.com ", port = 27018),
            ).mongoClientSettings()

        assertThat(settings.clusterSettings.hosts).containsExactly(ServerAddress("docdb.example.com", 27018))
        assertThat(settings.clusterSettings.requiredReplicaSetName).isEqualTo("rs0")
        assertThat(settings.readPreference).isEqualTo(ReadPreference.secondaryPreferred())
        assertThat(settings.retryWrites).isFalse()
        assertThat(settings.credential!!.authenticationMechanism).isEqualTo(AuthenticationMechanism.MONGODB_AWS)
        assertThat(settings.credential!!.source).isEqualTo("\$external")
        assertThat(settings.credential!!.userName).isNull()
        assertThat(settings.credential!!.password).isNull()
        assertThat(settings.sslSettings.isEnabled).isTrue()
        assertThat(settings.sslSettings.isInvalidHostNameAllowed).isFalse()
        assertThat(settings.sslSettings.context).isNotNull()
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DocumentDbProperties::class)
    @ImportAutoConfiguration(
        MongoAutoConfiguration::class,
        DataMongoAutoConfiguration::class,
        DataMongoRepositoriesAutoConfiguration::class,
    )
    class AutoConfig
}
