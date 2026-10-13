package com.example.backend.common.search

/** 색인용 OpenSearchClient 빈 이름(응답 대기를 늘린 별도 클라이언트). 주입 시 `@Qualifier(OpenSearchIndexClient.NAME)`. */
object OpenSearchIndexClient {
    const val NAME = "openSearchIndexClient"
}
