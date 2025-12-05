package dev.subfly.yabacore.unfurl

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

internal actual val unfurlEngine: HttpClientEngineFactory<*> = OkHttp
