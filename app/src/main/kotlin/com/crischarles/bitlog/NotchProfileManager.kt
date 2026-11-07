package com.crischarles.bitlog

import android.content.Context
import android.graphics.Color
import org.json.JSONObject

data class SmallPillConfig(
    val minWidthDp: Float,
    val heightDp: Float,
    val cornerRadiusDp: Float,
    val topOffsetDp: Float,
    val paddingHorizontalDp: Float,
    val backgroundColor: Int,
    val borderColor: Int,
    val borderWidthDp: Float,
    val showMiniProgress: Boolean,
)

data class BigIslandConfig(
    val widthDp: Float,
    val heightDp: Float,
    val cornerRadiusDp: Float,
    val topOffsetDp: Float,
    val backgroundColor: Int,
    val borderColor: Int,
    val borderWidthDp: Float,
    val blurBackground: Boolean,
)

data class NotchProfile(
    val name: String,
    val smallPill: SmallPillConfig,
    val bigIsland: BigIslandConfig,
)

object NotchProfileManager {

    private var cachedProfiles: Map<String, NotchProfile>? = null

    fun getProfile(context: Context, flavor: AndroidOsFlavor = OsDetector.currentFlavor): NotchProfile {
        val map = cachedProfiles ?: loadProfiles(context).also { cachedProfiles = it }
        return map[flavor.profileKey] ?: map["generic"] ?: defaultFallbackProfile()
    }

    private fun loadProfiles(context: Context): Map<String, NotchProfile> {
        val result = mutableMapOf<String, NotchProfile>()
        try {
            val jsonString = context.assets.open("os_notch_profiles.json")
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(jsonString).getJSONObject("profiles")
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.getJSONObject(key)
                val name = obj.optString("name", key)

                val spObj = obj.getJSONObject("smallPill")
                val smallPill = SmallPillConfig(
                    minWidthDp = spObj.optDouble("minWidthDp", 170.0).toFloat(),
                    heightDp = spObj.optDouble("heightDp", 38.0).toFloat(),
                    cornerRadiusDp = spObj.optDouble("cornerRadiusDp", 19.0).toFloat(),
                    topOffsetDp = spObj.optDouble("topOffsetDp", 7.0).toFloat(),
                    paddingHorizontalDp = spObj.optDouble("paddingHorizontalDp", 12.0).toFloat(),
                    backgroundColor = Color.parseColor(spObj.optString("backgroundColor", "#0C0C0E")),
                    borderColor = Color.parseColor(spObj.optString("borderColor", "#33FFFFFF")),
                    borderWidthDp = spObj.optDouble("borderWidthDp", 1.2).toFloat(),
                    showMiniProgress = spObj.optBoolean("showMiniProgress", true),
                )

                val biObj = obj.getJSONObject("bigIsland")
                val bigIsland = BigIslandConfig(
                    widthDp = biObj.optDouble("widthDp", 336.0).toFloat(),
                    heightDp = biObj.optDouble("heightDp", 168.0).toFloat(),
                    cornerRadiusDp = biObj.optDouble("cornerRadiusDp", 30.0).toFloat(),
                    topOffsetDp = biObj.optDouble("topOffsetDp", 8.0).toFloat(),
                    backgroundColor = Color.parseColor(biObj.optString("backgroundColor", "#101114")),
                    borderColor = Color.parseColor(biObj.optString("borderColor", "#38FFFFFF")),
                    borderWidthDp = biObj.optDouble("borderWidthDp", 1.4).toFloat(),
                    blurBackground = biObj.optBoolean("blurBackground", true),
                )

                result[key] = NotchProfile(name, smallPill, bigIsland)
            }
        } catch (_: Exception) {
            result["generic"] = defaultFallbackProfile()
        }
        return result
    }

    private fun defaultFallbackProfile(): NotchProfile {
        return NotchProfile(
            name = "Universal Android Notch",
            smallPill = SmallPillConfig(
                minWidthDp = 170f,
                heightDp = 38f,
                cornerRadiusDp = 19f,
                topOffsetDp = 7f,
                paddingHorizontalDp = 12f,
                backgroundColor = Color.parseColor("#0C0C0E"),
                borderColor = Color.parseColor("#33FFFFFF"),
                borderWidthDp = 1.2f,
                showMiniProgress = true,
            ),
            bigIsland = BigIslandConfig(
                widthDp = 336f,
                heightDp = 168f,
                cornerRadiusDp = 30f,
                topOffsetDp = 8f,
                backgroundColor = Color.parseColor("#101114"),
                borderColor = Color.parseColor("#38FFFFFF"),
                borderWidthDp = 1.4f,
                blurBackground = true,
            ),
        )
    }
}

