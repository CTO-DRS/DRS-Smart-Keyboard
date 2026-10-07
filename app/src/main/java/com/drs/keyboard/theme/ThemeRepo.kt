package com.drs.keyboard.theme

import android.content.Context

/**
 * Theme packages ("pre-made packs") + persistence of the active/custom theme.
 * All glass packs share the depth aesthetic with different moods.
 */
object ThemeRepo {

    private const val PREFS = "drs_theme"
    private const val KEY_CURRENT = "current_theme_json"
    private const val KEY_NIGHT = "night_theme_json"

    /** Pre-made packages — glass depth in eight moods. */
    fun presets(): List<KeyboardTheme> = listOf(
        KeyboardTheme(
            id = "midnight", name = "Midnight Glass",
            bgColor = 0xF2161B22.toInt(), keyColor = 0x2EFFFFFF.toInt(),
            specialKeyColor = 0x1F16202B.toInt(), keyTextColor = 0xF2FFFFFF.toInt(),
            accentColor = 0xFF3D7BFF.toInt(), pressColor = 0x423D7BFF.toInt(),
            strokeColor = 0x24FFFFFF.toInt(), candidateBg = 0x26FFFFFF.toInt(),
            cornerRadiusDp = 18, keyHeightDp = 54, opacityPct = 94
        ),
        KeyboardTheme(
            id = "frost", name = "Frost Glass",
            bgColor = 0xF3EEF3F9.toInt(), keyColor = 0xE6FFFFFF.toInt(),
            specialKeyColor = 0xCCD8DEE8.toInt(), keyTextColor = 0xFF1B2330.toInt(),
            accentColor = 0xFF2E6BFF.toInt(), pressColor = 0x422E6BFF.toInt(),
            strokeColor = 0x3DFFFFFF.toInt(), candidateBg = 0xD9FFFFFF.toInt(),
            cornerRadiusDp = 16, keyHeightDp = 54, opacityPct = 96
        ),
        KeyboardTheme(
            id = "emerald", name = "Emerald Glass",
            bgColor = 0xF20F231D.toInt(), keyColor = 0x2EFFE9D9.toInt(),
            specialKeyColor = 0x2412241C.toInt(), keyTextColor = 0xFFDFFCF1.toInt(),
            accentColor = 0xFF19C2A8.toInt(), pressColor = 0x4219C2A8.toInt(),
            strokeColor = 0x2B19C2A8.toInt(), candidateBg = 0x2619C2A8.toInt(),
            cornerRadiusDp = 20, keyHeightDp = 54, opacityPct = 93
        ),
        KeyboardTheme(
            id = "rose", name = "Rose Quartz",
            bgColor = 0xF2F5E9EE.toInt(), keyColor = 0xE8FFFFFF.toInt(),
            specialKeyColor = 0xCCF0DCE4.toInt(), keyTextColor = 0xFF3A222B.toInt(),
            accentColor = 0xFFD6447C.toInt(), pressColor = 0x40D6447C.toInt(),
            strokeColor = 0x42FFFFFF.toInt(), candidateBg = 0xD9FFFFFF.toInt(),
            cornerRadiusDp = 22, keyHeightDp = 54, opacityPct = 96
        ),
        KeyboardTheme(
            id = "ocean", name = "Deep Ocean",
            bgColor = 0xF20D1E2E.toInt(), keyColor = 0x2E9BE8FF.toInt(),
            specialKeyColor = 0x240D1E2E.toInt(), keyTextColor = 0xFFE3F2FF.toInt(),
            accentColor = 0xFF19A7E0.toInt(), pressColor = 0x4219A7E0.toInt(),
            strokeColor = 0x2B19A7E0.toInt(), candidateBg = 0x2619A7E0.toInt(),
            cornerRadiusDp = 18, keyHeightDp = 54, opacityPct = 94
        ),
        KeyboardTheme(
            id = "ink", name = "Mono Ink",
            bgColor = 0xF4141416.toInt(), keyColor = 0x24FFFFFF.toInt(),
            specialKeyColor = 0x14141414.toInt(), keyTextColor = 0xFFECECEC.toInt(),
            accentColor = 0xFFB9BDC4.toInt(), pressColor = 0x42B9BDC4.toInt(),
            strokeColor = 0x1FFFFFFF.toInt(), candidateBg = 0x1FFFFFFF.toInt(),
            cornerRadiusDp = 10, keyHeightDp = 54, opacityPct = 97, glass = false
        ),
        KeyboardTheme(
            id = "sand", name = "Desert Sand",
            bgColor = 0xF2F1E8DC.toInt(), keyColor = 0xE8FFFFFF.toInt(),
            specialKeyColor = 0xCCE4D6C3.toInt(), keyTextColor = 0xFF3E3226.toInt(),
            accentColor = 0xFFC07A2D.toInt(), pressColor = 0x40C07A2D.toInt(),
            strokeColor = 0x42FFFFFF.toInt(), candidateBg = 0xD9FFFFFF.toInt(),
            cornerRadiusDp = 14, keyHeightDp = 54, opacityPct = 96
        ),
        KeyboardTheme(
            id = "violet", name = "Violet Nebula",
            bgColor = 0xF21B1430.toInt(), keyColor = 0x2EC7AFFF.toInt(),
            specialKeyColor = 0x221B1430.toInt(), keyTextColor = 0xFFF1EBFF.toInt(),
            accentColor = 0xFF8E6BFF.toInt(), pressColor = 0x428E6BFF.toInt(),
            strokeColor = 0x2B8E6BFF.toInt(), candidateBg = 0x268E6BFF.toInt(),
            cornerRadiusDp = 20, keyHeightDp = 54, opacityPct = 94
        ),
        KeyboardTheme(
            id = "cyber", name = "Neon Cyber",
            bgColor = 0xF2120E1E.toInt(), keyColor = 0x2E7DF9FF.toInt(),
            specialKeyColor = 0x24120E1E.toInt(), keyTextColor = 0xFFE4FBFF.toInt(),
            accentColor = 0xFF00E5FF.toInt(), pressColor = 0x4200E5FF.toInt(),
            strokeColor = 0x2B00E5FF.toInt(), candidateBg = 0x2600E5FF.toInt(),
            cornerRadiusDp = 12, keyHeightDp = 54, opacityPct = 93
        ),
        KeyboardTheme(
            id = "sunset", name = "Sunset Glass",
            bgColor = 0xF2260F14.toInt(), keyColor = 0x2EFFB08A.toInt(),
            specialKeyColor = 0x24260F14.toInt(), keyTextColor = 0xFFFFEFE6.toInt(),
            accentColor = 0xFFFF7A45.toInt(), pressColor = 0x42FF7A45.toInt(),
            strokeColor = 0x2BFF7A45.toInt(), candidateBg = 0x26FF7A45.toInt(),
            cornerRadiusDp = 20, keyHeightDp = 54, opacityPct = 94
        ),
        KeyboardTheme(
            id = "matcha", name = "Matcha Cream",
            bgColor = 0xF3EAF2E4.toInt(), keyColor = 0xE6FFFFFF.toInt(),
            specialKeyColor = 0xCCD5E3C8.toInt(), keyTextColor = 0xFF2A331F.toInt(),
            accentColor = 0xFF6BA43A.toInt(), pressColor = 0x406BA43A.toInt(),
            strokeColor = 0x3DFFFFFF.toInt(), candidateBg = 0xD9FFFFFF.toInt(),
            cornerRadiusDp = 18, keyHeightDp = 54, opacityPct = 96
        ),
        KeyboardTheme(
            id = "nordic", name = "Nordic Light",
            bgColor = 0xF3E8EDF4.toInt(), keyColor = 0xE6FFFFFF.toInt(),
            specialKeyColor = 0xCCCFD9E6.toInt(), keyTextColor = 0xFF1E2836.toInt(),
            accentColor = 0xFF3E7CB1.toInt(), pressColor = 0x403E7CB1.toInt(),
            strokeColor = 0x3DFFFFFF.toInt(), candidateBg = 0xD9FFFFFF.toInt(),
            cornerRadiusDp = 14, keyHeightDp = 54, opacityPct = 96
        ),
        KeyboardTheme(
            id = "gold", name = "Royal Gold",
            bgColor = 0xF51A1510.toInt(), keyColor = 0x2EFFE3AE.toInt(),
            specialKeyColor = 0x241A1510.toInt(), keyTextColor = 0xFFFFF4DC.toInt(),
            accentColor = 0xFFE8B44C.toInt(), pressColor = 0x42E8B44C.toInt(),
            strokeColor = 0x2BE8B44C.toInt(), candidateBg = 0x26E8B44C.toInt(),
            cornerRadiusDp = 16, keyHeightDp = 54, opacityPct = 95
        ),
        KeyboardTheme(
            id = "carbon", name = "Carbon Red",
            bgColor = 0xF80B0B0D.toInt(), keyColor = 0x26FFFFFF.toInt(),
            specialKeyColor = 0x170B0B0D.toInt(), keyTextColor = 0xFFEDEDEE.toInt(),
            accentColor = 0xFFFF4757.toInt(), pressColor = 0x42FF4757.toInt(),
            strokeColor = 0x22FFFFFF.toInt(), candidateBg = 0x1FFFFFFF.toInt(),
            cornerRadiusDp = 10, keyHeightDp = 54, opacityPct = 98, glass = false
        ),
        KeyboardTheme(
            id = "lavender", name = "Lavender Mist",
            bgColor = 0xF3F0EDFA.toInt(), keyColor = 0xE6FFFFFF.toInt(),
            specialKeyColor = 0xCCDFD7F2.toInt(), keyTextColor = 0xFF2C2440.toInt(),
            accentColor = 0xFF7A5CD6.toInt(), pressColor = 0x407A5CD6.toInt(),
            strokeColor = 0x3DFFFFFF.toInt(), candidateBg = 0xD9FFFFFF.toInt(),
            cornerRadiusDp = 20, keyHeightDp = 54, opacityPct = 96
        ),
        KeyboardTheme(
            id = "cocoa", name = "Cocoa Cream",
            bgColor = 0xF3F1ECE8.toInt(), keyColor = 0xE8FFFFFF.toInt(),
            specialKeyColor = 0xCCDFD2CA.toInt(), keyTextColor = 0xFF332A24.toInt(),
            accentColor = 0xFF9A6B4F.toInt(), pressColor = 0x409A6B4F.toInt(),
            strokeColor = 0x42FFFFFF.toInt(), candidateBg = 0xD9FFFFFF.toInt(),
            cornerRadiusDp = 16, keyHeightDp = 54, opacityPct = 96
        ),
        KeyboardTheme(
            id = "aurora", name = "Aurora Neon",
            bgColor = 0xFA070B14.toInt(), keyColor = 0xD91E2740.toInt(),
            specialKeyColor = 0xD9161E30.toInt(), keyTextColor = 0xFFFFFFFF.toInt(),
            accentColor = 0xFF2EE56B.toInt(), pressColor = 0x422EE56B.toInt(),
            strokeColor = 0x3D4D8DFF.toInt(), candidateBg = 0x2631C9FF.toInt(),
            cornerRadiusDp = 16, keyHeightDp = 54, opacityPct = 97, neonRims = true
        ),
        KeyboardTheme(
            id = "nebula", name = "Nebula Neon",
            bgColor = 0xFA0C0818.toInt(), keyColor = 0xD9241A3C.toInt(),
            specialKeyColor = 0xD91B1430.toInt(), keyTextColor = 0xFFFFFFFF.toInt(),
            accentColor = 0xFFB44DFF.toInt(), pressColor = 0x42B44DFF.toInt(),
            strokeColor = 0x3DB44DFF.toInt(), candidateBg = 0x26B44DFF.toInt(),
            cornerRadiusDp = 16, keyHeightDp = 54, opacityPct = 97, neonRims = true
        ),
        KeyboardTheme(
            id = "solar", name = "Solar Neon",
            bgColor = 0xFA160D06.toInt(), keyColor = 0xD92E2212.toInt(),
            specialKeyColor = 0xD922180C.toInt(), keyTextColor = 0xFFFFFFFF.toInt(),
            accentColor = 0xFFFF8A3D.toInt(), pressColor = 0x42FF8A3D.toInt(),
            strokeColor = 0x3DFF8A3D.toInt(), candidateBg = 0x26FF8A3D.toInt(),
            cornerRadiusDp = 16, keyHeightDp = 54, opacityPct = 97, neonRims = true
        ),
        KeyboardTheme(
            id = "ion", name = "Ion Neon",
            bgColor = 0xFA061019.toInt(), keyColor = 0xD9122A3A.toInt(),
            specialKeyColor = 0xD90C202C.toInt(), keyTextColor = 0xFFFFFFFF.toInt(),
            accentColor = 0xFF31E9FF.toInt(), pressColor = 0x4231E9FF.toInt(),
            strokeColor = 0x3D31C9FF.toInt(), candidateBg = 0x2631C9FF.toInt(),
            cornerRadiusDp = 16, keyHeightDp = 54, opacityPct = 97, neonRims = true
        )
    )

    fun loadCurrent(context: Context): KeyboardTheme {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CURRENT, null)
        val parsed = json?.let { KeyboardTheme.fromJson(it) }
        return parsed ?: presets().first()
    }

    fun saveCurrent(context: Context, theme: KeyboardTheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CURRENT, theme.toJson()).apply()
    }

    /** Theme reserved for automatic night switching (null = not set yet). */
    fun loadNight(context: Context): KeyboardTheme? {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NIGHT, null)
        return json?.let { KeyboardTheme.fromJson(it) }
    }

    fun saveNight(context: Context, theme: KeyboardTheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_NIGHT, theme.toJson()).apply()
    }

    fun nightThemeName(context: Context): String? = loadNight(context)?.name

    /** True between 19:00 and 06:00 (device time). */
    fun isNightHour(): Boolean {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return h >= 19 || h < 6
    }
}
