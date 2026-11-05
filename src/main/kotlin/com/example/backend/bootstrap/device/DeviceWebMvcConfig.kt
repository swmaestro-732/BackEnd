package com.example.backend.bootstrap.device

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class DeviceWebMvcConfig(
    private val currentDeviceIdArgumentResolver: CurrentDeviceIdArgumentResolver,
    private val deviceActivityInterceptor: DeviceActivityInterceptor,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentDeviceIdArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(deviceActivityInterceptor)
    }
}
