package dev.subfly.yabacore.util

import dev.subfly.yabacore.yabacore.generated.resources.Res

object BundleReader {

    fun getIconUri(name: String): String {
        return Res.getUri("files/icons/${name}.svg")
    }
}
