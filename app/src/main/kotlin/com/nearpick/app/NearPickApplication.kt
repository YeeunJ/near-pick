package com.nearpick.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.nearpick"])
class NearPickApplication

fun main(args: Array<String>) {
    runApplication<NearPickApplication>(*args)
}
