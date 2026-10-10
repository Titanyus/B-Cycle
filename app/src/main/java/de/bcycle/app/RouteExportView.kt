package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.InputType
import android.widget.*

/** Draft settings: nothing is applied until Save, including a newly selected folder. */
internal class RouteExportView(context: Context, config: RouteExportConfig, passwordStored: Boolean,
    chooseFolder: () -> Unit, test: () -> Unit) : LinearLayout(context) {
    private val save = CheckBox(context).apply { text = "Speichern im Zielverzeichnis"; tag = "export_save"; isChecked = config.save }
    private val dav = CheckBox(context).apply { text = "WebDAV · automatisch zu Nextcloud hochladen"; tag = "export_webdav"; isChecked = config.webdav }
    private val wifi = CheckBox(context).apply { text = "Uploads nur über WLAN ohne Datenlimit"; isChecked = config.wifiOnly }
    private fun text(title: String) = TextView(context).apply { text = title; setTextColor(Color.WHITE); textSize = 14f }
    private fun input(title: String, value: String, name: String) = EditText(context).apply {
        hint = title; setText(value); tag = name; setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }
    private val server = input("Nextcloud-Adresse (https://…)", config.server, "export_server")
    private val user = input("Benutzername", config.user, "export_user")
    val password = input(if (passwordStored) "App-Passwort gespeichert · leer lassen zum Beibehalten" else "Nextcloud-App-Passwort", "", "export_password").apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        isSaveEnabled = false
    }
    private val folder = input("Nextcloud-Zielverzeichnis", config.folder, "export_folder")
    private val testButton = Button(context).apply { text = "Verbindung testen"; isAllCaps = false; tag = "export_test"; setOnClickListener { test() } }
    val result = text("").apply { tag = "export_result" }
    var tree: String = config.tree
    val folderButton = Button(context).apply { isAllCaps = false; tag = "export_folder_choose"; setOnClickListener { chooseFolder() } }
    init {
        orientation = VERTICAL; val pad = (16 * resources.displayMetrics.density).toInt(); setPadding(pad, pad / 2, pad, pad / 2)
        addView(text("Die fertige FIT-Datei bleibt immer in der Streckensammlung. Beide Ausgaben sind unabhängig und starten nach STOPP."))
        addView(save); addView(folderButton); refreshFolder()
        addView(Button(context).apply { text = "Downloads / B-Cycle verwenden"; isAllCaps = false; setOnClickListener { tree = ""; refreshFolder() } })
        addView(dav); addView(server); addView(user); addView(password); addView(folder); addView(wifi)
        addView(text("Serveradresse ohne /remote.php/dav/… eingeben. Der Verbindungstest prüft den Zugang und legt das Zielverzeichnis bei Bedarf an. Verwende ein separates Nextcloud-App-Passwort."))
        addView(testButton); addView(result)
    }
    fun read() = RouteExportConfig(save.isChecked, tree, dav.isChecked, server.text.toString().trim().trimEnd('/'),
        user.text.toString().trim(), folder.text.toString().trim().trim('/'), wifi.isChecked)
    fun testing(busy: Boolean) { testButton.isEnabled = !busy; result.text = if (busy) "Verbindung wird geprüft …" else result.text }
    fun refreshFolder() { folderButton.text = if (tree.isEmpty()) "Zielordner: Downloads / B-Cycle · ändern" else "Zielordner: ${Uri.parse(tree).lastPathSegment} · ändern" }
}
