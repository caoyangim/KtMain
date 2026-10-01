package com.cy.ktmain.videoedit.player

import android.content.Context

/**
 * Creation entry point so screens stay decoupled from the concrete player.
 * Replace [create] to swap implementations (e.g. a test double or another engine).
 */
object VideoPlayerFactory {

    @Volatile
    var creator: (Context) -> VideoPlayer = { context -> Media3VideoPlayer(context) }

    fun create(context: Context): VideoPlayer = creator(context.applicationContext)
}
