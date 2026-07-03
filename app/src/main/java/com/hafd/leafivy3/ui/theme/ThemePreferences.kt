package com.hafd.leafivy3.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ── Supported color seeds ──────────────────────────────────────────────────

enum class AppColorSeed(val label: String, val primary: Long) {
    BLUE     ("Blue",      0xFF1558C0),
    INDIGO   ("Indigo",    0xFF3949AB),
    PURPLE   ("Purple",    0xFF7B1FA2),
    TEAL     ("Teal",      0xFF00796B),
    GREEN    ("Green",     0xFF2E7D32),
    OLIVE    ("Olive",     0xFF558B2F),
    ORANGE   ("Orange",    0xFFE65100),
    RED      ("Red",       0xFFC62828),
    PINK     ("Pink",      0xFFAD1457),
    BROWN    ("Brown",     0xFF4E342E),
    SLATE    ("Slate",     0xFF37474F),
}

enum class AppDarkMode(val label: String) {
    SYSTEM ("Follow system"),
    LIGHT  ("Always light"),
    DARK   ("Always dark"),
}

enum class AppLanguage(val code: String, val labelRes: Int) {
    INDONESIAN ("id", com.hafd.leafivy3.R.string.settings_lang_id),
    ENGLISH    ("en", com.hafd.leafivy3.R.string.settings_lang_en),
}

data class ThemePrefs(
    val colorSeed: AppColorSeed = AppColorSeed.GREEN,
    val darkMode: AppDarkMode   = AppDarkMode.SYSTEM,
    val dynamicColor: Boolean   = false,
    val language: AppLanguage   = AppLanguage.INDONESIAN,
)

// ── Singleton preferences store ────────────────────────────────────────────

object ThemePreferences {

    private const val PREF_FILE  = "theme_prefs"
    private const val KEY_SEED   = "color_seed"
    private const val KEY_DARK   = "dark_mode"
    private const val KEY_DYN    = "dynamic_color"
    private const val KEY_LANG   = "language"

    private lateinit var prefs: SharedPreferences

    private val _flow = MutableStateFlow(ThemePrefs())
    val flow: StateFlow<ThemePrefs> = _flow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        _flow.value = load()
    }

    fun update(prefs: ThemePrefs) {
        _flow.value = prefs
        save(prefs)
    }

    private fun load(): ThemePrefs = ThemePrefs(
        colorSeed    = AppColorSeed.entries.find { it.name == prefs.getString(KEY_SEED, null) } ?: AppColorSeed.BLUE,
        darkMode     = AppDarkMode.entries.find  { it.name == prefs.getString(KEY_DARK, null) } ?: AppDarkMode.SYSTEM,
        dynamicColor = prefs.getBoolean(KEY_DYN, false),
        language     = AppLanguage.entries.find   { it.name == prefs.getString(KEY_LANG, null) } ?: AppLanguage.INDONESIAN,
    )

    private fun save(p: ThemePrefs) {
        prefs.edit()
            .putString(KEY_SEED, p.colorSeed.name)
            .putString(KEY_DARK, p.darkMode.name)
            .putBoolean(KEY_DYN, p.dynamicColor)
            .putString(KEY_LANG, p.language.name)
            .apply()
    }
}
