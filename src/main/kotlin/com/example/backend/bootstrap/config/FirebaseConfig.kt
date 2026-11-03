package com.example.backend.bootstrap.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

/** 서비스 계정 JSON이 있는 환경에서만 FCM 발송 빈을 생성한다. */
@Configuration
@ConditionalOnExpression("@environment.getProperty('firebase.credentials-json', '').trim().length() > 0")
class FirebaseConfig {
    @Bean(destroyMethod = "delete")
    fun firebaseApp(
        @Value("\${firebase.credentials-json}") credentialsJson: String,
    ): FirebaseApp {
        val credentials = credentialsJson.byteInputStream().use { GoogleCredentials.fromStream(it) }
        val options = FirebaseOptions.builder().setCredentials(credentials).build()
        // 기본 앱이나 다른 테스트 컨텍스트의 앱과 이름이 충돌하지 않게 분리한다.
        return FirebaseApp.initializeApp(options, "push-${UUID.randomUUID()}")
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)
}
