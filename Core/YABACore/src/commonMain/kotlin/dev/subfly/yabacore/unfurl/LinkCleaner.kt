package dev.subfly.yabacore.unfurl

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom

object LinkCleaner {
    private val trackingParameters: Set<String> = setOf(
        // Google Analytics & Ads
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id",
        "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
        "gclid", "gclsrc", "dclid", "wbraid", "gbraid",

        // Facebook & Social
        "fbclid", "fb_action_ids", "fb_action_types", "fb_ref", "fb_source",
        "igshid", "igsh", "twclid",

        // Microsoft/Bing
        "msclkid", "msclsrc",

        // Amazon tracking (but preserve affiliate tags intentionally added)
        "ref", "ref_", "linkCode", "camp", "creative", "creativeASIN",
        "ascsubtag", "asc_campaign", "asc_refurl", "asc_source", "pd_rd_r", "pd_rd_w",
        "pd_rd_wg", "pf_rd_p", "pf_rd_r", "pf_rd_s", "pf_rd_t", "pf_rd_i", "psc",

        // Email marketing
        "mc_cid", "mc_eid", "mc_tc",

        // HubSpot
        "hsCtaTracking", "hsa_acc", "hsa_ad", "hsa_cam", "hsa_grp", "hsa_kw",
        "hsa_mt", "hsa_net", "hsa_src", "hsa_tgt", "hsa_ver", "_hsenc", "_hsmi",
        "__hssc", "__hstc", "__hsfp",

        // LinkedIn
        "li_fat_id", "lipi", "licu",

        // TikTok
        "tt_medium", "tt_content",

        // Other platforms
        "yclid", "zanpid", "ranMID", "ranEAID", "ranSiteID", "spm", "scm",
        "vn", "vp", "share", "sharesource", "icn", "icp", "icl", "ict", "icm",
        "_openstat", "ito", "itm_source", "itm_medium", "itm_campaign",
        "pk_source", "pk_medium", "pk_campaign", "pk_keyword", "pk_content",
        "mtm_source", "mtm_medium", "mtm_campaign", "mtm_keyword", "mtm_content",
        "nr_email_referer", "vero_conv", "vero_id", "wickedid", "gdfms", "gdftrk",
        "gdffi", "mkt_tok", "trk", "trkCampaign", "sc_campaign", "sc_channel",
        "elqTrackId", "elqTrack", "assetType", "assetId", "recipientId",
        "campaignId", "siteId", "at_medium", "at_campaign", "at_source",
    )

    fun clean(url: String): String {
        if (url.isEmpty()) return url

        val extractedUrl = extractURLFromText(url)
        val parsed = runCatching { Url(extractedUrl) }.getOrNull() ?: return url

        val unwrapped = unwrapRedirects(parsed)
        val cleaned = applySiteSpecificCleaning(unwrapped)
        return cleaned.toString()
    }

    private fun extractURLFromText(text: String): String {
        val regex = Regex("https?://\\S+")
        return regex.find(text)?.value ?: text
    }

    private fun unwrapRedirects(url: Url): Url {
        val host = url.host.lowercase()

        if (host == "l.facebook.com") {
            url.parameters["u"]?.let { redirectUrl ->
                decodeUrlOrNull(redirectUrl)?.let { return it }
            }
        }

        if (host == "www.google.com" && url.encodedPath == "/url") {
            url.parameters["url"]?.let { redirectUrl ->
                decodeUrlOrNull(redirectUrl)?.let { return it }
            }
        }

        if (host == "href.li") {
            val raw = url.toString()
            val afterQuestion = raw.substringAfter('?', missingDelimiterValue = "")
            decodeUrlOrNull(afterQuestion)?.let { return it }
        }

        if (host == "cts.businesswire.com") {
            url.parameters["url"]?.let { redirectUrl ->
                decodeUrlOrNull(redirectUrl)?.let { return it }
            }
        }

        return url
    }

    private fun applySiteSpecificCleaning(oldUrl: Url): Url {
        val host = oldUrl.host.lowercase()
        val builder = URLBuilder(oldUrl).apply { parameters.clear() }

        return when {
            host.contains("youtube.com") -> cleanYouTubeURL(oldUrl, builder)
            host.contains("amazon") -> cleanAmazonURL(oldUrl, builder)
            host.contains("facebook.com") -> cleanFacebookURL(oldUrl, builder)
            else -> cleanGenericURL(oldUrl, builder)
        }
    }

    private fun cleanYouTubeURL(oldUrl: Url, builder: URLBuilder): Url {
        oldUrl.parameters["v"]?.let { builder.parameters.append("v", it) }
        oldUrl.parameters["t"]?.let { builder.parameters.append("t", it) }
        oldUrl.parameters["list"]?.let { builder.parameters.append("list", it) }
        return builder.build()
    }

    private fun cleanAmazonURL(oldUrl: Url, builder: URLBuilder): Url {
        builder.host = builder.host.removePrefix("www.")

        val path = oldUrl.encodedPath
        val patterns = listOf("\\/dp\\/(\\w+)", "\\/product\\/(\\w+)", "\\/d\\/(\\w+)")

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(path)
            val productId = match?.groupValues?.getOrNull(1)
            if (!productId.isNullOrEmpty()) {
                builder.encodedPath = "/dp/$productId"
                break
            }
        }

        return builder.build()
    }

    private fun cleanFacebookURL(oldUrl: Url, builder: URLBuilder): Url {
        if (oldUrl.encodedPath.contains("story.php")) {
            oldUrl.parameters["story_fbid"]?.let { builder.parameters.append("story_fbid", it) }
            oldUrl.parameters["id"]?.let { builder.parameters.append("id", it) }
        }
        return builder.build()
    }

    private fun cleanGenericURL(oldUrl: Url, builder: URLBuilder): Url {
        oldUrl.parameters["q"]?.let { builder.parameters.append("q", it) }

        for (name in oldUrl.parameters.names()) {
            val lower = name.lowercase()
            if (lower == "q") continue
            if (!trackingParameters.contains(lower)) {
                if (!builder.parameters.contains(name)) {
                    builder.parameters.appendAll(
                        name,
                        oldUrl.parameters.getAll(name) ?: emptyList()
                    )
                }
            }
        }

        return builder.build()
    }

    private fun decodeUrlOrNull(value: String): Url? {
        return runCatching { Url(value) }.getOrNull() ?: runCatching {
            URLBuilder().takeFrom(value).build()
        }.getOrNull()
    }
}
