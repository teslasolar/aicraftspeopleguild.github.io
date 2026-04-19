package com.aicraftspeopleguild.acg.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * EncryptedSharedPreferences wrapper for the GitHub PAT. The prefs file
 * (acg_secure.xml) is excluded from both cloud backup and device-transfer
 * (see res/xml/backup_rules.xml + data_extraction_rules.xml) so the
 * ciphertext doesn't leave the device.
 */
class TokenStore(ctx: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        ctx.applicationContext,
        "acg_secure",
        MasterKey.Builder(ctx.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getToken(): String? = prefs.getString("gh_pat", null)?.takeIf { it.isNotBlank() }
    fun setToken(value: String?) {
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove("gh_pat") else putString("gh_pat", value)
        }.apply()
    }
    fun hasToken(): Boolean = getToken() != null
}
