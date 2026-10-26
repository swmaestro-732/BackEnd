package com.example.backend.crawl.application.port.outbound

import com.example.backend.common.geo.Coordinate

interface CrawlWalkingPort {
    fun walkingSegments(points: List<Coordinate>): List<Int>
}
