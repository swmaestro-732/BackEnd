package com.example.backend.support

import org.springframework.boot.test.context.SpringBootTest

/**
 * 통합 테스트 베이스.
 * DB 는 환경변수(SPRING_DATASOURCE_*, CI 의 Postgres 서비스)나
 * 로컬 docker-compose 의 기본값으로 연결된다.
 */
@SpringBootTest
abstract class IntegrationTestBase
