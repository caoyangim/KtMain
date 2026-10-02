package com.cy.ktmain.videoedit.trim

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cy.ktmain.videoedit.MediaUtils
import com.cy.ktmain.videoedit.PlayerSurfaceController
import com.cy.ktmain.videoedit.R
import com.cy.ktmain.videoedit.VideoEditActivity
import com.cy.ktmain.videoedit.media.FrameExtractor
import com.cy.ktmain.videoedit.model.VideoDraft
import com.cy.ktmain.videoedit.player.VideoPlayerFactory
import com.cy.ktmain.videoedit.widget.RangeSeekBar
import com.google.android.material.button.MaterialButton
import java.io.File

/**
 * Trim range picker. Stores start/end on the draft only — no re-encode.
 */
class TrimVideoFragment : Fragment() {

    private var controller: PlayerSurfaceController? = null
    private var durationMs: Long = 0L
    private var localPath: String = ""
    private var startMs: Long = 0L
    private var endMs: Long = 0L

    private lateinit var timeLabel: TextView
    private lateinit var rangeBar: RangeSeekBar
    private lateinit var nextButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localPath = requireArguments().getString(ARG_PATH).orEmpty()
        durationMs = requireArguments().getLong(ARG_DURATION)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.videoedit_fragment_trim, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        timeLabel = view.findViewById(R.id.trimTimeLabel)
        rangeBar = view.findViewById(R.id.trimRange)
        nextButton = view.findViewById(R.id.trimNext)

        startMs = 0L
        endMs = durationMs.coerceAtLeast(VideoDraft.MIN_TRIM_MS)

        val surface = view.findViewById<SurfaceView>(R.id.trimSurface)
        val player = VideoPlayerFactory.create(requireContext())
        val pc = PlayerSurfaceController(surface, player)
        controller = pc
        pc.attach()
        pc.prepare(Uri.fromFile(File(localPath)), autoPlay = true)

        rangeBar.listener = object : RangeSeekBar.OnRangeChangeListener {
            override fun onRangeChanged(startRatio: Float, endRatio: Float, fromUser: Boolean) {
                startMs = (durationMs * startRatio).toLong()
                endMs = (durationMs * endRatio).toLong()
                updateTimeLabel()
                if (fromUser) {
                    player.seekTo(startMs)
                }
            }
        }
        rangeBar.setRange(0f, 1f, notify = false)
        updateTimeLabel()

        nextButton.setOnClickListener {
            val host = activity as? VideoEditActivity ?: return@setOnClickListener
            val current = host.currentDraft() ?: return@setOnClickListener
            val updated = current.clampedTrim(startMs, endMs)
            host.setDraft(updated)
            host.onTrimConfirmed(updated)
        }

        view.findViewById<View>(R.id.trimPlayPause).setOnClickListener {
            if (player.isPlaying()) pc.pause() else pc.playInRange(startMs, endMs)
        }
    }

    override fun onPause() {
        super.onPause()
        controller?.pause()
    }

    override fun onDestroyView() {
        controller?.release()
        controller = null
        super.onDestroyView()
    }

    private fun updateTimeLabel() {
        timeLabel.text = getString(
            R.string.videoedit_trim_range_label,
            FrameExtractor.formatTime(startMs),
            FrameExtractor.formatTime(endMs),
            FrameExtractor.formatTime(durationMs)
        )
        nextButton.isEnabled = endMs - startMs >= VideoDraft.MIN_TRIM_MS
    }

    companion object {
        private const val ARG_PATH = "path"
        private const val ARG_DURATION = "duration"

        fun newInstance(path: String, durationMs: Long): TrimVideoFragment =
            TrimVideoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PATH, path)
                    putLong(ARG_DURATION, durationMs)
                }
            }
    }
}
