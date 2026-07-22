package com.example.backend

import com.example.backend.bootstrap.config.MediaProperties
import com.example.backend.bootstrap.security.JwtProperties
import com.example.backend.bootstrap.security.KakaoOauthProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties::class, KakaoOauthProperties::class, MediaProperties::class)
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
