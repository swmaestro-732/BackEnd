package com.example.backend.push.adapter.config

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
        // 미설정(0)이면 무한 대기다. 묶음 처리가 push 큐 visibility timeout(30초) 안에 끝나도록 짧게 둔다.
        val options =
            FirebaseOptions
                .builder()
                .setCredentials(credentials)
                .setConnectTimeout(FCM_CONNECT_TIMEOUT_MILLIS)
                .setReadTimeout(FCM_READ_TIMEOUT_MILLIS)
                .build()
        // 기본 앱이나 다른 테스트 컨텍스트의 앱과 이름이 충돌하지 않게 분리한다.
        return FirebaseApp.initializeApp(options, "push-${UUID.randomUUID()}")
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)

    private companion object {
        const val FCM_CONNECT_TIMEOUT_MILLIS = 5_000
        const val FCM_READ_TIMEOUT_MILLIS = 10_000
    }
}
