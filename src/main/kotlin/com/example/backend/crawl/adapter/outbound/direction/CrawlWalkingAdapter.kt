package com.example.backend.crawl.adapter.outbound.direction

import com.example.backend.common.geo.Coordinate
import com.example.backend.crawl.application.port.outbound.CrawlWalkingPort
import com.example.backend.direction.application.port.inbound.WalkingDurationUseCase
import org.springframework.stereotype.Component

@Component
class CrawlWalkingAdapter(
    private val walking: WalkingDurationUseCase,
) : CrawlWalkingPort {
    override fun walkingSegments(points: List<Coordinate>): List<Int> = walking.walkingSegments(points)
}
