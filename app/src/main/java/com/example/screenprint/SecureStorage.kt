package com.example.screenprint

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

object SecureStorage {
    private const val PREFS_NAME = "biometric_secure_prefs"
    private const val KEY_PROFILE_PREFIX = "saved_biometric_profile_"
    private const val KEY_PASSWORD_HASH = "fallback_password_hash"
    private const val KEY_PASSWORD_SALT = "fallback_password_salt"
    private const val KEY_SAMPLES_COUNT = "samples_count"

    private lateinit var encryptedPrefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context, PREFS_NAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSample(profileJson: String, currentCount: Int) {
        encryptedPrefs.edit()
            .putString("$KEY_PROFILE_PREFIX$currentCount", profileJson)
            .putInt(KEY_SAMPLES_COUNT, currentCount + 1)
            .apply()
    }

    fun getSamples(): List<String> {
        val count = encryptedPrefs.getInt(KEY_SAMPLES_COUNT, 0)
        val list = mutableListOf<String>()
        for (i in 0 until count) {
            encryptedPrefs.getString("$KEY_PROFILE_PREFIX$i", null)?.let { list.add(it) }
        }
        return list
    }

    fun clearSamples() {
        encryptedPrefs.edit().clear().apply()
    }
    
    fun hasRegistered(): Boolean {
        return encryptedPrefs.getInt(KEY_SAMPLES_COUNT, 0) > 0
    }

    fun saveFallbackPassword(password: String) {
        val salt = generateRandomSalt()
        val hashedPassword = hashPassword(password, salt)
        encryptedPrefs.edit()
            .putString(KEY_PASSWORD_HASH, hashedPassword)
            .putString(KEY_PASSWORD_SALT, salt)
            .apply()
    }

    fun checkFallbackPassword(password: String): Boolean {
        val storedHash = encryptedPrefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val storedSalt = encryptedPrefs.getString(KEY_PASSWORD_SALT, null) ?: return false
        return hashPassword(password, storedSalt) == storedHash
    }

    private fun generateRandomSalt(): String {
        val random = java.security.SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val hashedBytes = md.digest(password.toByteArray(Charsets.UTF_8))
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
}
