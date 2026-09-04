package com.aichat.assistant.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Holds exactly one secret: the user's API key. Backed by [EncryptedSharedPreferences], whose
 * master key lives in the Android Keystore — the key material never exists in plain form on
 * disk, satisfying master spec §12 ("do not store the API key as plain text").
 *
 * This class is the only place in the app allowed to touch the raw key. It never logs it, never
 * includes it in a thrown exception message, and the file it writes to is excluded from Android
 * backups (see xml/backup_rules.xml and xml/data_extraction_rules.xml).
 */
class SecureKeyStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    /** For UI display only — never renders the real key, per master spec §2/§8. */
    fun maskedApiKey(): String {
        val key = getApiKey()
        if (key.isBlank()) return ""
        val visibleSuffix = key.takeLast(4)
        return "•".repeat(8) + visibleSuffix
    }

    companion object {
        private const val FILE_NAME = "secure_key_store"
        private const val KEY_API_KEY = "api_key"
    }
}
