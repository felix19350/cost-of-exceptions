package com.example

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import java.util.concurrent.ConcurrentHashMap

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

/**
 * A sort of data store kind of thing.
 * */
object DataStore {

    private val map: MutableMap<String, String> = ConcurrentHashMap()

    fun put(key: String, value: String) {
        map[key] = value
    }

    fun get(key: String): String? {
        return map[key]
    }
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    // Bootstrap the datastore with some well known values.
    repeat(10) { i -> DataStore.put("key-$i", "value-$i") }

    routing {
        // F for functional handling
        get("/f/store/{key}") {
            val key = call.parameters["key"] ?: ""
            when (val result = functionalHandler(key)) {
                is Either.Left -> call.respond(HttpStatusCode.NotFound)
                is Either.Right -> call.respondText(result.b)
            }
        }

        // E for regular exception handling
        get("/e/store/{key}") {
            try {
                val key = call.parameters["key"] ?: ""
                val result = DataStore.get(key)
                requireNotNull(result)
                call.respondText(result)
            } catch (ex: Exception) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

data class SimpleError(val msg: String)

private fun functionalHandler(key: String): Either<SimpleError, String> {
    val result = DataStore.get(key)
    return if (result != null) {
        Right(result)
    } else {
        Left(SimpleError("Key $key does not exist."))
    }
}
