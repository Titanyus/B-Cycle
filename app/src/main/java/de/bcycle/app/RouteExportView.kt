package de.bcycle.app

import android.content.Context
import android.net.Uri
import android.text.InputType
import android.widget.*

/** Draft settings: nothing is applied until Save, including a newly selected folder. */
internal class RouteExportView(context: Context, config: RouteExportConfig, passwordStored: Boolean,
    chooseFolder: () -> Unit, test: () -> Unit,
    private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    private val storedPassword: () -> String = { "" }) : LinearLayout(context) {
    private val save = CheckBox(context).apply { text = "Speichern im Zielverzeichnis"; tag = "export_save"; isChecked = config.save }
    private val dav = CheckBox(context).apply { text = "WebDAV · automatisch zu Nextcloud hochladen"; tag = "export_webdav"; isChecked = config.webdav }
    private val wifi = CheckBox(context).apply { text = "Uploads nur über WLAN ohne Datenlimit"; isChecked = config.wifiOnly }
    private fun text(title: String) = TextView(context).apply { text = title; textSize = BicycleDesign.bodySp }
    private fun input(title: String, value: String, name: String) = EditText(context).apply {
        hint = title; setText(value); tag = name; setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }
    private val server = input("Nextcloud- oder WebDAV-Adresse (https://…)", config.server, "export_server")
    private val user = input("Benutzername", config.user, "export_user")
    val password = input(if (passwordStored) "App-Passwort gespeichert · leer lassen zum Beibehalten" else "Nextcloud-App-Passwort", "", "export_password").apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        isSaveEnabled = false
    }
    private val folder = input("Zielordner (relativ, z. B. B-Cycle)", config.folder, "export_folder")
    private val testButton = Button(context).apply { text = "Verbindung testen"; isAllCaps = false; tag = "export_test"; setOnClickListener { test() } }
    val result = text("").apply { tag = "export_result" }
    var tree: String = config.tree
    val folderButton = Button(context).apply { isAllCaps = false; tag = "export_folder_choose"; setOnClickListener { chooseFolder() } }
    init {
        orientation = VERTICAL; val pad = BicycleDesign.dp(context, BicycleDesign.horizontalPadding); setPadding(pad, pad / 2, pad, pad / 2)
        addView(save); addView(folderButton); refreshFolder()
        addView(BicycleComponents.sectionDivider(context, themes))
        addView(dav); addView(server); addView(user)
        val passwordRow = LinearLayout(context).apply { orientation = HORIZONTAL; isBaselineAligned = false }
        passwordRow.addView(password, LayoutParams(0, -2, 1f))
        var passwordVisible = false
        lateinit var eye: ImageButton
        fun updateEye() {
            eye.contentDescription = if (passwordVisible) "Passwort verbergen" else "Passwort anzeigen"
            eye.setImageDrawable(BicycleIcons.drawable(context, BicycleIcon.ShowOnMap,
                themes.active.tokens.foreground, negative = passwordVisible))
        }
        eye = BicycleComponents.icon(context, themes, BicycleIcon.ShowOnMap, "Passwort anzeigen") {
            if (!passwordVisible && password.text.isEmpty() && passwordStored) {
                val saved = storedPassword()
                if (saved.isEmpty()) return@icon
                password.setText(saved)
            }
            val start = password.selectionStart; val end = password.selectionEnd
            passwordVisible = !passwordVisible
            password.transformationMethod = if (passwordVisible) null else android.text.method.PasswordTransformationMethod.getInstance()
            if (start >= 0 && end >= 0) password.setSelection(start.coerceAtMost(password.length()), end.coerceAtMost(password.length()))
            updateEye()
        }.apply { tag = "export_password_visibility" }
        passwordRow.addView(eye, LayoutParams(BicycleDesign.dp(context, BicycleDesign.touchSize),
            BicycleDesign.dp(context, BicycleDesign.touchSize)).apply { topMargin = BicycleDesign.dp(context, BicycleDesign.verticalPadding) })
        addView(passwordRow)
        addView(folder); addView(wifi)
        addView(BicycleComponents.sectionDivider(context, themes))
        addView(testButton); addView(result)
        BicycleDialogStyle.actionIcon(folderButton, themes, BicycleIcon.Folder)
        BicycleDialogStyle.actionIcon(testButton, themes, BicycleIcon.Connection)
        BicycleDialogStyle.content(this, themes)
    }
    fun read() = RouteExportConfig(save.isChecked, tree, dav.isChecked, server.text.toString().trim().trimEnd('/'),
        user.text.toString().trim(), folder.text.toString().trim().trim('/'), wifi.isChecked)
    fun testing(busy: Boolean) { testButton.isEnabled = !busy; result.text = if (busy) "Verbindung wird geprüft …" else result.text }
    fun refreshFolder() { folderButton.text = if (tree.isEmpty()) "Zielordner: Downloads / B-Cycle · ändern" else "Zielordner: ${Uri.parse(tree).lastPathSegment} · ändern" }
}
