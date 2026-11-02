package com.example.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
// 설정 클래스(@ConfigurationProperties)는 각 소유 패키지에 두고 스캔으로 등록한다. 진입점이 다른 모듈 내부를 import 하지 않게 하기 위함.
@ConfigurationPropertiesScan
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
