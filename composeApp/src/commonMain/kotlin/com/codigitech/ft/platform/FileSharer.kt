package com.codigitech.ft.platform

/**
 * Hands a generated text file to the OS share sheet (Android chooser / iOS activity view) so the
 * user can save it to Files, Drive, mail it, etc. Nothing leaves the device unless the user picks a target.
 */
interface FileSharer {
    fun shareText(fileName: String, mimeType: String, content: String)
}
