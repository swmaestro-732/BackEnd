package com.example.backend.support

import com.example.backend.bootstrap.config.AsyncConfig
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * 통합 테스트 베이스.
 * DB 는 환경변수(SPRING_DATASOURCE_*, CI 의 Postgres 서비스)나
 * 로컬 docker-compose 의 기본값으로 연결된다.
 */
@SpringBootTest
abstract class IntegrationTestBase {
    @Autowired
    @Qualifier(AsyncConfig.NOTIFICATION_EXECUTOR)
    private lateinit var notificationExecutor: ThreadPoolTaskExecutor

    /**
     * 커밋 후 비동기 알림 기록이 다음 테스트의 픽스처(TRUNCATE)와 겹치면 교착이 난다.
     * 테스트마다 알림 실행기가 빌 때까지 기다려 테스트 사이 DB 작업을 직렬화한다.
     */
    @AfterEach
    fun awaitNotificationTasks() = awaitNotificationIdle()

    protected fun awaitNotificationIdle(timeoutMillis: Long = 5_000) {
        val pool = notificationExecutor.threadPoolExecutor
        val deadline = System.currentTimeMillis() + timeoutMillis
        // activeCount 는 작업자가 큐에서 꺼낸 직후 잠깐 0 이 될 수 있어, 제출 수와 완료 수를 비교한다.
        while (pool.completedTaskCount < pool.taskCount) {
            check(System.currentTimeMillis() < deadline) { "알림 실행기가 ${timeoutMillis}ms 안에 비지 않았습니다." }
            Thread.sleep(10)
        }
    }
}
