package de.bcycle.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class RouteExportConfig(val save: Boolean = true, val tree: String = "", val webdav: Boolean = false,
    val server: String = "", val user: String = "", val folder: String = "B-Cycle", val wifiOnly: Boolean = true)

/** Password never enters the route index, jobs, diagnostics or Android backups. */
internal class RouteExportSettings(context: Context, private val secretKey: (() -> SecretKey)? = null) {
    private val prefs = context.getSharedPreferences("route-export", Context.MODE_PRIVATE)
    fun read() = RouteExportConfig(prefs.getBoolean("save", true), prefs.getString("tree", "")!!,
        prefs.getBoolean("webdav", false), prefs.getString("server", "")!!, prefs.getString("user", "")!!,
        prefs.getString("folder", "B-Cycle")!!, prefs.getBoolean("wifi", true))
    fun hasPassword() = prefs.contains("password")
    fun password(): String {
        val stored = prefs.getString("password", null) ?: return ""
        val parts = stored.split(":")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
    }
    fun write(config: RouteExportConfig, newPassword: String? = null) {
        val edit = prefs.edit().putBoolean("save", config.save).putString("tree", config.tree)
            .putBoolean("webdav", config.webdav).putString("server", config.server).putString("user", config.user)
            .putString("folder", config.folder).putBoolean("wifi", config.wifiOnly)
        if (newPassword != null) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            edit.putString("password", Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(cipher.doFinal(newPassword.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
        }
        check(edit.commit()) { "Einstellungen konnten nicht gespeichert werden" }
    }
    private fun key(): SecretKey {
        secretKey?.let { return it() }
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("bcycle-webdav", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("bcycle-webdav", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
