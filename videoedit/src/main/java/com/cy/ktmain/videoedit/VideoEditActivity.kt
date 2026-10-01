package com.cy.ktmain.videoedit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.cy.ktmain.videoedit.cover.CoverPublishFragment
import com.cy.ktmain.videoedit.model.PreviewFilter
import com.cy.ktmain.videoedit.model.VideoDraft
import com.cy.ktmain.videoedit.select.SelectVideoFragment
import com.cy.ktmain.videoedit.trim.TrimVideoFragment
import java.io.File

/**
 * Host for the video-edit demo flow: select → trim → cover → back to select.
 * Filter step is intentionally omitted; [onTrimConfirmed] is the extension point.
 */
class VideoEditActivity : AppCompatActivity() {

    private var draft: VideoDraft? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_edit)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.videoEditContainer, SelectVideoFragment())
                .commit()
        }
    }

    fun currentDraft(): VideoDraft? = draft

    fun setDraft(value: VideoDraft?) {
        draft = value
    }

    fun openTrim(source: VideoDraft) {
        draft = source
        push(TrimVideoFragment.newInstance(source.localPath, source.durationMs))
    }

    /** Extension point: insert filter page here later. */
    fun onTrimConfirmed(updated: VideoDraft) {
        draft = updated
        push(
            CoverPublishFragment.newInstance(
                localPath = updated.localPath,
                trimStartMs = updated.trimStartMs,
                trimEndMs = updated.trimEndMs,
                durationMs = updated.durationMs,
                filterId = updated.filterId
            )
        )
    }

    fun onCoverCompleted(updated: VideoDraft) {
        draft = updated
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    fun onDraftCleared() {
        draft?.coverPath?.let { runCatching { File(it).delete() } }
        draft = null
    }

    private fun push(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.videoEditContainer, fragment)
            .addToBackStack(fragment.javaClass.simpleName)
            .commit()
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, VideoEditActivity::class.java)

        fun applyFilter(view: View, filterId: String) {
            PreviewFilter.fromId(filterId).applyTo(view)
        }
    }
}
