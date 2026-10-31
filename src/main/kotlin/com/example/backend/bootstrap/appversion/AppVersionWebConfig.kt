package com.example.backend.bootstrap.appversion

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AppVersionWebConfig(
    private val appVersionInterceptor: AppVersionInterceptor,
    private val currentDeviceIdArgumentResolver: CurrentDeviceIdArgumentResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(appVersionInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentDeviceIdArgumentResolver)
    }
}
