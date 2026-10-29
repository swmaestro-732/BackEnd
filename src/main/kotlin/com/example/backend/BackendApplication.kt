package com.example.backend

import com.example.backend.bootstrap.config.GooglePlacesProperties
import com.example.backend.bootstrap.config.KakaoLocalProperties
import com.example.backend.bootstrap.config.MediaProperties
import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.bootstrap.config.RedisProperties
import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.bootstrap.config.TmapProperties
import com.example.backend.bootstrap.security.AppleOauthProperties
import com.example.backend.bootstrap.security.GoogleOauthProperties
import com.example.backend.bootstrap.security.JwtProperties
import com.example.backend.bootstrap.security.KakaoOauthProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    JwtProperties::class,
    KakaoOauthProperties::class,
    GoogleOauthProperties::class,
    AppleOauthProperties::class,
    MediaProperties::class,
    KakaoLocalProperties::class,
    TmapProperties::class,
    GooglePlacesProperties::class,
    OpenSearchProperties::class,
    RedisProperties::class,
    SqsProperties::class,
)
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
