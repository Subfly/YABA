package dev.subfly.yabacore.database

import android.content.Context

actual fun buildDatabase(platformContext: Any?): YabaDatabase {
    val context =
            platformContext as? Context
                    ?: error(
                            "Android DatabaseProvider.initialize requires an android.content.Context"
                    )
    return context.createYabaDatabase()
}
