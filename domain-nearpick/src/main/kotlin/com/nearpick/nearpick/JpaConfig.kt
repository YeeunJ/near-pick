package com.nearpick.nearpick

import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@AutoConfigurationPackage  // com.nearpick.nearpick 패키지를 entity scan 대상으로 등록
@EnableJpaRepositories(basePackages = ["com.nearpick.nearpick"])
class JpaConfig
