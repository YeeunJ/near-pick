package com.nearpick.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NearPickApplication

fun main(args: Array<String>) {
    runApplication<NearPickApplication>(*args)
}
