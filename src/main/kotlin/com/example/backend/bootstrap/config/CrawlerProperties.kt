package com.example.backend.bootstrap.config

import com.example.backend.common.domain.CourseVisibility
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "crawler")
data class CrawlerProperties(
    val curatorUserId: Long? = null,
    val publishVisibility: CourseVisibility = CourseVisibility.PUBLIC,
)
