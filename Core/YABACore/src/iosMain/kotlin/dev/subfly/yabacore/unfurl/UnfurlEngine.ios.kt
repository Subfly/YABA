package dev.subfly.yabacore.unfurl

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual val unfurlEngine: HttpClientEngineFactory<*> = Darwin
