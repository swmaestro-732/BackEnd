package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 푸시 발송 설정.
 * - [maxAge]: 큐에서 이보다 오래 머문 메시지는 늦은 알림이 되므로 발송하지 않고 버린다.
 * - [androidTtl]: 기기가 꺼져 있을 때 FCM 이 보관하는 기간.
 * - [androidChannelId]: 안드로이드 앱의 활동 알림 채널 id.
 */
@ConfigurationProperties(prefix = "push")
data class PushProperties(
    val maxAge: Duration = Duration.ofHours(1),
    val androidTtl: Duration = Duration.ofDays(1),
    val androidChannelId: String = "courmy_activity",
)
