package com.example.backend.web

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class RequiresAppFeature(
    val value: AppFeature,
)
