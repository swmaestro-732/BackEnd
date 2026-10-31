package com.example.backend.bootstrap.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

@Configuration
@ConditionalOnExpression("'\${docdb.endpoint:}'.trim().length() > 0")
class MongoConfig(
    private val properties: DocumentDbProperties,
) {
    @Bean
    fun mongoClientSettings(): MongoClientSettings {
        // JAR 안의 PEM 을 읽어 이 클라이언트에만 적용한다. JVM 전역 trust store 는 변경하지 않는다.
        val certificates =
            ClassPathResource("global-bundle.pem").inputStream.use {
                CertificateFactory.getInstance("X.509").generateCertificates(it)
            }
        require(certificates.isNotEmpty()) { "DocumentDB CA 번들이 비어 있습니다." }
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        certificates.forEachIndexed { index, certificate ->
            trustStore.setCertificateEntry("rds-ca-$index", certificate)
        }
        val trustManagers =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(trustStore) }
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustManagers.trustManagers, null) }

        // DocumentDB 는 retryable writes 를 지원하지 않는다. IAM 자격증명은 드라이버의 SDK v2 기본 체인에 맡긴다.
        val uri =
            "mongodb://${properties.endpoint.trim()}:${properties.port}/" +
                "?replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false" +
                "&authSource=\$external&authMechanism=MONGODB-AWS&tls=true"
        return MongoClientSettings
            .builder()
            .applyConnectionString(ConnectionString(uri))
            .applyToSslSettings { it.context(sslContext) }
            .build()
    }

    @Bean(destroyMethod = "close")
    fun mongoClient(settings: MongoClientSettings): MongoClient = MongoClients.create(settings)

    @Bean
    fun mongoDatabaseFactory(client: MongoClient): MongoDatabaseFactory =
        SimpleMongoClientDatabaseFactory(client, properties.database)
}
