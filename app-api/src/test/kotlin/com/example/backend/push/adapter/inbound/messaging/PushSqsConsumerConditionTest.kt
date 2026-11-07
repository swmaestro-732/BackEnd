package com.example.backend.push.adapter.inbound.messaging

import com.example.backend.bootstrap.config.PushProperties
import com.example.backend.push.application.port.inbound.PushDeliveryUseCase
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Clock

/** 큐와 Firebase 키가 모두 있을 때만 리스너 빈이 생기는지 확인한다(키 없이 수신하면 메시지가 사라진다). */
class PushSqsConsumerConditionTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withBean(PushDeliveryUseCase::class.java, { mock(PushDeliveryUseCase::class.java) })
            .withBean(PushProperties::class.java, { PushProperties() })
            .withBean(Clock::class.java, { Clock.systemUTC() })
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            // 실제 앱처럼 컨텍스트 준비 뒤(@Import 처리 시점)에 조건을 평가하도록 설정 클래스로 감싼다.
            .withUserConfiguration(ConsumerImport::class.java)

    @Test
    fun `큐와 키가 모두 있으면 리스너를 만든다`() {
        contextRunner
            .withPropertyValues("aws.sqs.push-queue-url=$QUEUE_URL", "firebase.credentials-json={\"type\":\"it's\"}")
            .run { assertThat(it).hasSingleBean(PushSqsConsumer::class.java) }
    }

    @Test
    fun `키가 없으면 큐가 있어도 리스너를 만들지 않는다`() {
        contextRunner
            // 실행 환경 변수에 키가 있어도 테스트가 흔들리지 않게 빈 값으로 덮는다.
            .withPropertyValues("aws.sqs.push-queue-url=$QUEUE_URL", "firebase.credentials-json=")
            .run { assertThat(it).doesNotHaveBean(PushSqsConsumer::class.java) }
    }

    @Test
    fun `큐가 없으면 키가 있어도 리스너를 만들지 않는다`() {
        contextRunner
            .withPropertyValues("aws.sqs.push-queue-url=", "firebase.credentials-json={}")
            .run { assertThat(it).doesNotHaveBean(PushSqsConsumer::class.java) }
    }

    @Configuration
    @Import(PushSqsConsumer::class)
    class ConsumerImport

    private companion object {
        const val QUEUE_URL = "https://sqs.ap-northeast-2.amazonaws.com/123/push"
    }
}
