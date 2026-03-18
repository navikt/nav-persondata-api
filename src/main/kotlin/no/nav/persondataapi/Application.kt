package no.nav.persondataapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class Application

const val APPLICATION = "watson\\oppslag-bruker"

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
