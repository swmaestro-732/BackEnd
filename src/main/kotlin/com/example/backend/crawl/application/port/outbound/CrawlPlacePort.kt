package com.example.backend.crawl.application.port.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.crawl.application.port.inbound.dto.CrawledPlaceCommand

interface CrawlPlacePort {
    fun resolve(places: List<CrawledPlaceCommand>): List<ResolvedCrawlPlace>
}

data class ResolvedCrawlPlace(
    val kakaoPlaceId: String,
    val id: Long,
    val coordinate: Coordinate,
)
