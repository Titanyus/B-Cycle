package de.bcycle.app

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/** UI-only draft. Encryption, validation, actual transfer and persistence remain in existing services. */
internal class ComposeExportDraft(initial:RouteExportConfig) {
    var config by mutableStateOf(initial)
    var password by mutableStateOf("") // Deliberately not rememberSaveable / Android saved instance state.
    var visible by mutableStateOf(false)
    var message by mutableStateOf("")
    var testing by mutableStateOf(false)
}
@Composable internal fun ComposeExportSettings(draft:ComposeExportDraft,part:RouteExportPart,hasPassword:Boolean,storedPassword:()->String,
    chooseFolder:()->Unit,test:()->Unit,changed:(RouteExportConfig,String?)->Unit) {
    val context=LocalContext.current;val prefs=context.getSharedPreferences("route-export",0)
    val revision=preferenceRevision(prefs)
    val settings=RouteExportSettings(context)
    val passwordStored=hasPassword || settings.hasPassword()
    val currentTree=settings.read().tree
    fun edit(config:RouteExportConfig,password:String?=draft.password.takeIf { it.isNotEmpty() }) { draft.config=config;changed(config,password) }
    BicycleBody {
        if(part!=RouteExportPart.CLOUD) {
            BicycleCheck(stringResource(R.string.route_export_save),draft.config.save,"export_save") { edit(draft.config.copy(save=it)) }
            BicycleAction(if(currentTree.isEmpty()) stringResource(R.string.route_export_default_folder) else context.getString(R.string.route_export_chosen_folder,Uri.parse(currentTree).lastPathSegment),BicycleIcon.Folder,tag="export_folder_choose",action=chooseFolder)
        }
        if(part!=RouteExportPart.LOCAL) {
            BicycleCheck(stringResource(R.string.route_export_dav),draft.config.webdav,"export_webdav") { edit(draft.config.copy(webdav=it)) }
            BicycleInput(stringResource(R.string.route_export_server),draft.config.server,"export_server",KeyboardOptions(autoCorrectEnabled=false,keyboardType=KeyboardType.Uri)) { edit(draft.config.copy(server=it)) }
            BicycleInput(stringResource(R.string.route_export_user),draft.config.user,"export_user",KeyboardOptions(autoCorrectEnabled=false)) { edit(draft.config.copy(user=it)) }
            BicycleInput(stringResource(if(passwordStored) R.string.route_export_password_saved else R.string.route_export_password),draft.password,"export_password",
                KeyboardOptions(keyboardType=KeyboardType.Password),if(draft.visible) VisualTransformation.None else PasswordVisualTransformation()) {
                draft.password=it;edit(draft.config,it.takeIf { value->value.isNotEmpty() })
            }
            BicycleAction(stringResource(if(draft.visible) R.string.route_export_password_hide else R.string.route_export_password_show),if(draft.visible) BicycleIcon.ShowOnMap else BicycleIcon.HideOnMap,tag="export_password_visibility") {
                if(!draft.visible && draft.password.isEmpty() && passwordStored) draft.password=storedPassword()
                draft.visible=!draft.visible // Revealing the existing password never triggers a storage operation.
            }
            BicycleInput(stringResource(R.string.route_export_folder),draft.config.folder,"export_folder",KeyboardOptions(autoCorrectEnabled=false)) { edit(draft.config.copy(folder=it)) }
            BicycleCheck(stringResource(R.string.route_export_wifi),draft.config.wifiOnly,"export_wifi") { edit(draft.config.copy(wifiOnly=it)) }
            BicycleDivider();BicycleAction(stringResource(R.string.route_export_test),BicycleIcon.Connection,!draft.testing,"export_test",test)
        }
        if(draft.message.isNotEmpty()) BicycleText(draft.message)
    }
}
