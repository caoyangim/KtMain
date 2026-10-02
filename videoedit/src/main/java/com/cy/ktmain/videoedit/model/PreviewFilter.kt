package com.cy.ktmain.videoedit.model

import android.view.View

/**
 * Extension point for preview-time filters.
 * The demo ships only [NonePreviewFilter]; real filters can be registered later
 * without changing navigation or [com.cy.ktmain.videoedit.player.VideoPlayer].
 */
interface PreviewFilter {
    val id: String
    fun applyTo(target: View)
    fun clear(target: View)

    companion object {
        const val NONE = "NONE"

        fun fromId(id: String): PreviewFilter = when (id) {
            else -> NonePreviewFilter
        }
    }
}

object NonePreviewFilter : PreviewFilter {
    override val id: String = PreviewFilter.NONE
    override fun applyTo(target: View) = Unit
    override fun clear(target: View) = Unit
}
