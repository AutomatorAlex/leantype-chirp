package helium314.keyboard.latin.chirp.settings

import android.content.Context
import android.content.SharedPreferences
import helium314.keyboard.latin.utils.protectedPrefs

class ChirpPreferences(context: Context) {
    companion object {
        const val KEY_ENABLED = "chirp_voice_enabled"
        const val KEY_PROVIDER = "chirp_provider"
        const val KEY_API_KEY = "chirp_api_key"
        const val KEY_REQUESTY_API_KEY = "chirp_requesty_api_key"
        const val KEY_MODEL = "chirp_model"
        const val KEY_REQUESTY_MODEL = "chirp_requesty_model"
        const val DEFAULT_MODEL = "google/chirp-3"
        const val DEFAULT_REQUESTY_MODEL = "openai/gpt-4o-mini-transcribe"
    }

    enum class SttProvider {
        OPENROUTER,
        REQUESTY;

        companion object {
            fun from(value: String?): SttProvider = try {
                valueOf(value ?: OPENROUTER.name)
            } catch (e: IllegalArgumentException) {
                OPENROUTER
            }
        }
    }

    // Use credential-protected storage. API keys must not be stored in LeanType's
    // device-protected prefs, which are available before first unlock.
    private val prefs: SharedPreferences = context.protectedPrefs()

    fun isVoiceEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getProvider(): SttProvider = SttProvider.from(prefs.getString(KEY_PROVIDER, SttProvider.OPENROUTER.name))

    fun setProvider(provider: SttProvider) {
        prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
    }

    fun getApiKey(provider: SttProvider = getProvider()): String = prefs.getString(apiKeyFor(provider), "") ?: ""

    fun setApiKey(key: String) {
        prefs.edit().putString(apiKeyFor(getProvider()), key).apply()
    }

    fun getModel(): String {
        val provider = getProvider()
        return (prefs.getString(modelKeyFor(provider), "") ?: "").ifBlank { defaultModelFor(provider) }
    }

    fun setModel(model: String) {
        val provider = getProvider()
        prefs.edit().putString(modelKeyFor(provider), model.ifBlank { defaultModelFor(provider) }).apply()
    }

    fun defaultModelFor(provider: SttProvider): String = when (provider) {
        SttProvider.OPENROUTER -> DEFAULT_MODEL
        SttProvider.REQUESTY -> DEFAULT_REQUESTY_MODEL
    }

    private fun apiKeyFor(provider: SttProvider): String = when (provider) {
        SttProvider.OPENROUTER -> KEY_API_KEY
        SttProvider.REQUESTY -> KEY_REQUESTY_API_KEY
    }

    private fun modelKeyFor(provider: SttProvider): String = when (provider) {
        SttProvider.OPENROUTER -> KEY_MODEL
        SttProvider.REQUESTY -> KEY_REQUESTY_MODEL
    }
}
