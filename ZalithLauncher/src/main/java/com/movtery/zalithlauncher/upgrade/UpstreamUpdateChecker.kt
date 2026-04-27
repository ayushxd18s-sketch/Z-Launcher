package com.movtery.zalithlauncher.upgrade

import com.movtery.zalithlauncher.path.URL_ZALITH_LATEST
import com.movtery.zalithlauncher.path.ZALITH_BASE_VERSION
import com.movtery.zalithlauncher.utils.network.fetchStringFromUrl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

suspend fun checkUpstreamUpdate(): String? {
    return try {
        val response = fetchStringFromUrl(URL_ZALITH_LATEST)
        val json = Json.parseToJsonElement(response).jsonObject
        val latestVersion = json["tag_name"]?.jsonPrimitive?.content ?: return null
        val clean = latestVersion.trimStart('v')
        if (clean != ZALITH_BASE_VERSION) clean else null
    } catch (e: Exception) {
        null
    }
}
