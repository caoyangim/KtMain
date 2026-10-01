package com.cy.ktmain.videoedit.cover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.cy.ktmain.videoedit.MediaUtils
import com.cy.ktmain.videoedit.R
import com.cy.ktmain.videoedit.VideoEditActivity
import com.cy.ktmain.videoedit.media.FrameExtractor
import com.cy.ktmain.videoedit.model.PreviewFilter
import com.cy.ktmain.videoedit.model.VideoDraft
import com.cy.ktmain.videoedit.widget.FrameStripView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cover picker: film-strip scrubbing under a center marker.
 */
class CoverPublishFragment : Fragment() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val extractor = FrameExtractor()

    private var localPath: String = ""
    private var trimStartMs: Long = 0L
    private var trimEndMs: Long = 0L
    private var durationMs: Long = 0L
    private var filterId: String = PreviewFilter.NONE
    private var selectedCoverMs: Long = 0L

    private var coverPreview: ImageView? = null
    private var frameStrip: FrameStripView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localPath = requireArguments().getString(ARG_PATH).orEmpty()
        trimStartMs = requireArguments().getLong(ARG_TRIM_START)
        trimEndMs = requireArguments().getLong(ARG_TRIM_END)
        durationMs = requireArguments().getLong(ARG_DURATION)
        filterId = requireArguments().getString(ARG_FILTER) ?: PreviewFilter.NONE
        selectedCoverMs = trimStartMs
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.videoedit_fragment_cover, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        coverPreview = view.findViewById(R.id.coverPreview)
        frameStrip = view.findViewById(R.id.frameStrip)
        val doneButton = view.findViewById<MaterialButton>(R.id.coverDone)

        // Reserved for future filter preview on the cover image.
        PreviewFilter.fromId(filterId).applyTo(view)

        frameStrip?.onCoverChangedListener = object : FrameStripView.OnCoverChangedListener {
            override fun onCoverSelected(index: Int, timeMs: Long) {
                selectedCoverMs = timeMs.coerceIn(trimStartMs, if (trimEndMs > 0) trimEndMs else durationMs)
                loadCoverPreview(selectedCoverMs)
            }
        }

        doneButton.setOnClickListener { complete() }
        loadFrames()
    }

    private fun loadFrames() {
        val root = view ?: return
        scope.launch {
            val withBitmaps = withContext(Dispatchers.IO) {
                extractor.extract(localPath, FrameExtractor.DEFAULT_COUNT)
            }
            frameStrip?.submit(
                withBitmaps.map { FrameStripView.FrameItem(it.timeMs, it.bitmap) }
            )
            if (withBitmaps.isEmpty()) {
                Snackbar.make(root, R.string.videoedit_frame_failed, Snackbar.LENGTH_SHORT).show()
            }
            loadCoverPreview(selectedCoverMs)
        }
    }

    private fun loadCoverPreview(timeMs: Long) {
        val target = coverPreview ?: return
        scope.launch {
            val bitmap = extractor.extractCover(localPath, timeMs)
            if (bitmap != null) {
                target.setImageBitmap(bitmap)
            }
        }
    }

    private fun complete() {
        val host = activity as? VideoEditActivity ?: return
        val current = host.currentDraft() ?: return
        scope.launch {
            val bitmap = extractor.extractCover(localPath, selectedCoverMs)
            val coverPath = if (bitmap != null) {
                withContext(Dispatchers.IO) {
                    val out = File(requireContext().filesDir, "videoedit/cover_${System.currentTimeMillis()}.jpg")
                    MediaUtils.saveCover(bitmap, out)
                }
            } else {
                current.coverPath
            }
            val updated = current.copy(
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                filterId = filterId,
                coverTimeMs = selectedCoverMs,
                coverPath = coverPath
            )
            host.setDraft(updated)
            host.onCoverCompleted(updated)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        coverPreview = null
        frameStrip = null
    }

    companion object {
        private const val ARG_PATH = "path"
        private const val ARG_TRIM_START = "trim_start"
        private const val ARG_TRIM_END = "trim_end"
        private const val ARG_DURATION = "duration"
        private const val ARG_FILTER = "filter"

        fun newInstance(
            localPath: String,
            trimStartMs: Long,
            trimEndMs: Long,
            durationMs: Long,
            filterId: String,
        ): CoverPublishFragment = CoverPublishFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PATH, localPath)
                putLong(ARG_TRIM_START, trimStartMs)
                putLong(ARG_TRIM_END, trimEndMs)
                putLong(ARG_DURATION, durationMs)
                putString(ARG_FILTER, filterId)
            }
        }
    }
}
