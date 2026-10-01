package com.cy.ktmain.videoedit.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

/**
 * Copies picked/captured video into app-private storage so later screens
 * can read without holding onto temporary content Uri permissions.
 */
class VideoSourceRepository(private val context: Context) {

    fun copyToLocal(uri: Uri): File {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val name = queryDisplayName(uri) ?: "${UUID.randomUUID()}.mp4"
        val target = File(dir, "${System.currentTimeMillis()}_$name")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to open video stream: $uri")
        return target
    }

    fun clearCache() {
        File(context.filesDir, DIR_NAME).listFiles()?.forEach { it.delete() }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
        }.getOrNull()
    }

    companion object {
        const val DIR_NAME = "videoedit"
    }
}
