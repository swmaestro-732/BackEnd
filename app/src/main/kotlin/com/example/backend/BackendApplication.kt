package com.example.backend

import com.example.backend.common.search.OpenSearchProperties
import com.example.backend.direction.adapter.outbound.tmap.TmapProperties
import com.example.backend.media.application.MediaProperties
import com.example.backend.place.adapter.outbound.search.KakaoLocalProperties
import com.example.backend.user.adapter.outbound.messaging.SqsProperties
import com.example.backend.user.adapter.outbound.security.JwtProperties
import com.example.backend.user.adapter.outbound.social.GoogleOauthProperties
import com.example.backend.user.adapter.outbound.social.KakaoOauthProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    JwtProperties::class,
    KakaoOauthProperties::class,
    GoogleOauthProperties::class,
    MediaProperties::class,
    KakaoLocalProperties::class,
    TmapProperties::class,
    OpenSearchProperties::class,
    SqsProperties::class,
)
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
