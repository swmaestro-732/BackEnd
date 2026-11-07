package com.example.backend.support

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile

/**
 * OpenSearch 통합테스트용 공유 컨테이너 — analysis-nori(한글 형태소) 플러그인을 설치한 커스텀 이미지.
 *
 * 베이스 이미지엔 nori 가 없어 실 매핑(analyzer: nori)을 그대로 검증할 수 없다 → Dockerfile 로 플러그인을
 * 설치해 AWS OpenSearch(번들 nori)와 동일 조건을 만든다. 여러 통합테스트가 하나의 컨테이너를 공유하도록
 * 싱글턴(lazy)으로 한 번만 기동한다(Ryuk 이 JVM 종료 시 정리). 보안 플러그인은 꺼 평문 HTTP(9200)로 뜬다.
 */
object NoriOpenSearchContainer {
    val instance: GenericContainer<*> by lazy {
        GenericContainer(
            ImageFromDockerfile()
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("opensearchproject/opensearch:2.17.1")
                        .run("/usr/share/opensearch/bin/opensearch-plugin install --batch analysis-nori")
                        .build()
                },
        ).withExposedPorts(9200)
            .withEnv("discovery.type", "single-node")
            .withEnv("DISABLE_SECURITY_PLUGIN", "true") // 평문 HTTP·인증 없음(로컬 검증 목적)
            .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
            .waitingFor(Wait.forHttp("/").forPort(9200).forStatusCode(200))
            .apply { start() }
    }

    /** `opensearch.endpoint` 등 프로퍼티로 넘길 base URL(`http://host:port`). */
    fun endpoint(): String = "http://${instance.host}:${instance.getMappedPort(9200)}"
}
