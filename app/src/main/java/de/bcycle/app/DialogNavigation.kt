package de.bcycle.app

import android.app.AlertDialog

/** Keep parent dialogs intact, including expanded sections and unsaved selections. */
internal class DialogNavigation {
    private val stack = mutableListOf<AlertDialog>()
    private val cleanup = mutableMapOf<AlertDialog, () -> Unit>()
    val empty get() = stack.isEmpty()
    fun track(dialog: AlertDialog, shown: () -> Unit = {}) {
        dialog.setOnShowListener {
            if (dialog !in stack) { stack.lastOrNull()?.hide(); stack.add(dialog) }
            shown()
        }
        dialog.setOnDismissListener {
            cleanup.remove(dialog)?.invoke()
            val top = stack.lastOrNull() === dialog
            stack.remove(dialog)
            if (top) stack.lastOrNull()?.show()
        }
    }
    fun onDismiss(dialog: AlertDialog, action: () -> Unit) { cleanup[dialog] = action }
    fun closeAll() {
        val dialogs = stack.asReversed().toList()
        stack.clear()
        dialogs.forEach { it.dismiss() }
    }
}
