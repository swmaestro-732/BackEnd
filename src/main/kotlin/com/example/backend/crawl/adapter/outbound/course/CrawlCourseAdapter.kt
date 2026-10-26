package com.example.backend.crawl.adapter.outbound.course

import com.example.backend.bootstrap.config.CrawlerProperties
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommand
import com.example.backend.course.application.port.inbound.dto.CreateCoursePlaceCommand
import com.example.backend.crawl.application.port.outbound.CrawlCoursePort
import com.example.backend.crawl.application.port.outbound.CuratedCourseCommand
import org.springframework.stereotype.Component

@Component
class CrawlCourseAdapter(
    private val courseUseCase: CourseUseCase,
    private val properties: CrawlerProperties,
) : CrawlCoursePort {
    override fun create(command: CuratedCourseCommand): Long =
        requireNotNull(
            courseUseCase
                .create(
                    CreateCourseCommand(
                        userId = requireNotNull(properties.curatorUserId),
                        title = command.title,
                        description = command.description,
                        coverImageUrl = command.coverImageUrl,
                        tags = command.tags,
                        visibility = properties.publishVisibility,
                        isPublished = true,
                        duplicatedFromId = null,
                        places =
                            command.places.map { place ->
                                CreateCoursePlaceCommand(
                                    placeId = place.placeId,
                                    orderNo = place.orderNo,
                                    caption = null,
                                    imageUrls = place.imageUrls,
                                    walkingMinutes = place.walkingMinutes,
                                )
                            },
                    ),
                ).id,
        )
}
