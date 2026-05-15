package helium314.keyboard.latin.chirp.settings

import android.content.Context
import android.content.SharedPreferences
import helium314.keyboard.latin.utils.protectedPrefs

class ChirpPreferences(context: Context) {
    companion object {
        const val KEY_ENABLED = "chirp_voice_enabled"
        const val KEY_API_KEY = "chirp_api_key"
        const val KEY_MODEL = "chirp_model"
        const val DEFAULT_MODEL = "google/chirp-3"
    }

    // Use credential-protected storage. API keys must not be stored in LeanType's
    // device-protected prefs, which are available before first unlock.
    private val prefs: SharedPreferences = context.protectedPrefs()

    fun isVoiceEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getModel(): String = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL

    fun setModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
    }
}
