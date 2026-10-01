package com.cy.ktmain.videoedit.select

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.cy.ktmain.videoedit.MediaUtils
import com.cy.ktmain.videoedit.R
import com.cy.ktmain.videoedit.VideoEditActivity
import com.cy.ktmain.videoedit.VideoPreviewActivity
import com.cy.ktmain.videoedit.model.VideoDraft
import com.cy.ktmain.videoedit.media.VideoSourceRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Entry / result screen: empty plus card, or cover + play + delete after edit.
 */
class SelectVideoFragment : Fragment() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var sourceRepository: VideoSourceRepository

    private var emptyContainer: View? = null
    private var resultCard: MaterialCardView? = null
    private var coverImage: ImageView? = null
    private var playButton: View? = null
    private var deleteButton: View? = null
    private var pendingCaptureUri: Uri? = null

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onPicked(it) }
    }

    private val captureVideo =
        registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
            val uri = pendingCaptureUri
            pendingCaptureUri = null
            if (success && uri != null) {
                onPicked(uri)
            } else {
                view?.let {
                    Snackbar.make(it, R.string.videoedit_capture_cancelled, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.videoedit_fragment_select, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sourceRepository = VideoSourceRepository(requireContext().applicationContext)
        emptyContainer = view.findViewById(R.id.emptyContainer)
        resultCard = view.findViewById(R.id.resultCard)
        coverImage = view.findViewById(R.id.coverImage)
        playButton = view.findViewById(R.id.playButton)
        deleteButton = view.findViewById(R.id.deleteButton)

        emptyContainer?.setOnClickListener { showSourcePicker() }
        playButton?.setOnClickListener { openPreview() }
        deleteButton?.setOnClickListener { confirmDelete() }

        view.findViewById<View>(R.id.addButton)?.setOnClickListener { showSourcePicker() }
        bindState()
    }

    override fun onResume() {
        super.onResume()
        bindState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        emptyContainer = null
        resultCard = null
        coverImage = null
        playButton = null
        deleteButton = null
    }

    private fun bindState() {
        val draft = (activity as? VideoEditActivity)?.currentDraft()
        emptyContainer?.visibility = if (draft != null) View.GONE else View.VISIBLE
        resultCard?.visibility = if (draft != null) View.VISIBLE else View.GONE
        if (draft != null) {
            MediaUtils.loadCoverInto(coverImage ?: return, draft.coverPath)
        }
    }

    private fun showSourcePicker() {
        val options = arrayOf(
            getString(R.string.videoedit_source_capture),
            getString(R.string.videoedit_source_gallery)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.videoedit_source_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCapture()
                    1 -> pickVideo.launch("video/*")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun launchCapture() {
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "videoedit_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            view?.let { Snackbar.make(it, R.string.videoedit_open_failed, Snackbar.LENGTH_SHORT).show() }
            return
        }
        pendingCaptureUri = uri
        captureVideo.launch(uri)
    }

    private fun onPicked(uri: Uri) {
        val root = view
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { sourceRepository.copyToLocal(uri) }
                val duration = withContext(Dispatchers.IO) { MediaUtils.readDurationMs(file.absolutePath) }
                if (duration <= 0L) {
                    if (root != null) {
                        Snackbar.make(root, R.string.videoedit_invalid_video, Snackbar.LENGTH_LONG).show()
                    }
                    return@launch
                }
                val draft = VideoDraft(
                    sourceUri = uri.toString(),
                    localPath = file.absolutePath,
                    durationMs = duration
                )
                (activity as? VideoEditActivity)?.openTrim(draft)
            } catch (_: Exception) {
                if (root != null) {
                    Snackbar.make(root, R.string.videoedit_open_failed, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openPreview() {
        val draft = (activity as? VideoEditActivity)?.currentDraft() ?: return
        startActivity(
            VideoPreviewActivity.createIntent(
                requireContext(),
                draft.localPath,
                draft.trimStartMs,
                draft.trimEndMs
            )
        )
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.videoedit_delete_title)
            .setMessage(R.string.videoedit_delete_message)
            .setPositiveButton(R.string.videoedit_delete_confirm) { _, _ ->
                (activity as? VideoEditActivity)?.onDraftCleared()
                bindState()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val TAG = "SelectVideoFragment"
    }
}
