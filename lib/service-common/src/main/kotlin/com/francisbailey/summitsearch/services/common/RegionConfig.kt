package com.francisbailey.summitsearch.services.common

import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
open class RegionConfig(
    environment: Environment
) {

    val environmentType: EnvironmentType = EnvironmentType.valueOf(
        environment.getProperty(TYPE_KEY, EnvironmentType.BETA.name)
    )

    val isProd: Boolean = environmentType == EnvironmentType.PROD
    val isBeta: Boolean = environmentType == EnvironmentType.BETA
    val isDev: Boolean = environmentType == EnvironmentType.DEV


    enum class EnvironmentType {
        PROD,
        DEV,
        BETA;
    }

    companion object {
        const val TYPE_KEY = "ENVIRONMENT_TYPE"
    }

}