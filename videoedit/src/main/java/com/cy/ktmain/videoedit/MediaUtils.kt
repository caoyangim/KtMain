package com.cy.ktmain.videoedit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadataRetriever
import android.widget.ImageView
import java.io.File
import java.io.FileOutputStream

internal object MediaUtils {

    fun readDurationMs(path: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun readRotation(path: String): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun saveCover(bitmap: Bitmap, target: File): String {
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return target.absolutePath
    }

    fun placeholderBitmap(color: Int = Color.DKGRAY, width: Int = 320, height: Int = 180): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)
        return bitmap
    }

    fun loadCoverInto(imageView: ImageView, path: String?) {
        if (path.isNullOrBlank()) {
            imageView.setImageDrawable(ColorDrawable(Color.DKGRAY))
            return
        }
        runCatching {
            imageView.setImageBitmap(android.graphics.BitmapFactory.decodeFile(path))
        }.onFailure {
            imageView.setImageDrawable(ColorDrawable(Color.DKGRAY))
        }
    }
}
