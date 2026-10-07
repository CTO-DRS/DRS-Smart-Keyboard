package com.drs.keyboard.theme

import android.content.Context
import org.json.JSONObject

/**
 * A keyboard theme. Glass-depth rendering layers:
 *   panel (bgColor with opacity, rounded) → top sheen → keys (translucent,
 *   stroked) → accents. Optionally a user-picked background image.
 */
data class KeyboardTheme(
    val id: String,
    val name: String,
    var bgColor: Int = 0xF2161B22.toInt(),       // panel background (ARGB)
    var keyColor: Int = 0x2EFFFFFF.toInt(),      // letter key fill (ARGB)
    var specialKeyColor: Int = 0x1F000000.toInt(),// functional key fill
    var keyTextColor: Int = 0xF2FFFFFF.toInt(),
    var accentColor: Int = 0xFF3D7BFF.toInt(),
    var pressColor: Int = 0x3D3D7BFF.toInt(),
    var strokeColor: Int = 0x26FFFFFF.toInt(),
    var candidateBg: Int = 0x26FFFFFF.toInt(),
    var cornerRadiusDp: Int = 18,
    var keyHeightDp: Int = 54,
    var opacityPct: Int = 92,                    // panel opacity 40..100
    var glass: Boolean = true,                   // top sheen + edge highlights
    var neonRims: Boolean = false,               // per-key multi-hue neon rims + glow
    var tilePattern: Int = 0,                    // 0 none · 1 khatam star · 2 chevron · 3 quatrefoil · 4 lattice
    var bgImagePath: String? = null              // copied into filesDir
) {

    fun toJson(): String {
        val o = JSONObject()
        o.put("id", id)
        o.put("name", name)
        o.put("bgColor", bgColor)
        o.put("keyColor", keyColor)
        o.put("specialKeyColor", specialKeyColor)
        o.put("keyTextColor", keyTextColor)
        o.put("accentColor", accentColor)
        o.put("pressColor", pressColor)
        o.put("strokeColor", strokeColor)
        o.put("candidateBg", candidateBg)
        o.put("cornerRadiusDp", cornerRadiusDp)
        o.put("keyHeightDp", keyHeightDp)
        o.put("opacityPct", opacityPct)
        o.put("glass", glass)
        o.put("neonRims", neonRims)
        o.put("tilePattern", tilePattern)
        o.put("bgImagePath", bgImagePath ?: "")
        return o.toString()
    }

    companion object {
        fun fromJson(s: String): KeyboardTheme? = runCatching {
            val o = JSONObject(s)
            KeyboardTheme(
                id = o.optString("id", "custom"),
                name = o.optString("name", "Custom"),
                bgColor = o.optInt("bgColor", 0xF2161B22.toInt()),
                keyColor = o.optInt("keyColor", 0x2EFFFFFF.toInt()),
                specialKeyColor = o.optInt("specialKeyColor", 0x1F000000.toInt()),
                keyTextColor = o.optInt("keyTextColor", 0xF2FFFFFF.toInt()),
                accentColor = o.optInt("accentColor", 0xFF3D7BFF.toInt()),
                pressColor = o.optInt("pressColor", 0x3D3D7BFF.toInt()),
                strokeColor = o.optInt("strokeColor", 0x26FFFFFF.toInt()),
                candidateBg = o.optInt("candidateBg", 0x26FFFFFF.toInt()),
                cornerRadiusDp = o.optInt("cornerRadiusDp", 18),
                keyHeightDp = o.optInt("keyHeightDp", 54),
                opacityPct = o.optInt("opacityPct", 92),
                glass = o.optBoolean("glass", true),
                neonRims = o.optBoolean("neonRims", false),
                tilePattern = o.optInt("tilePattern", 0),
                bgImagePath = o.optString("bgImagePath", "").ifEmpty { null }
            )
        }.getOrNull()
    }
}
