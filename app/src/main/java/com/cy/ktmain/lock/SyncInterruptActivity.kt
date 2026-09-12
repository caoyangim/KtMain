package com.cy.ktmain.lock

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cy.ktmain.R
import com.cy.ktmain.utils.setupEdgeToEdgeInsets
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class SyncInterruptActivity : AppCompatActivity() {

    private val demo = SyncInterruptDemo()
    private lateinit var logView: TextView
    private lateinit var statusView: TextView
    private lateinit var logScroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_interrupt)
        setupEdgeToEdgeInsets(
            rootView = findViewById(R.id.syncRoot),
            toolbar = findViewById(R.id.syncToolbar),
        )

        findViewById<MaterialToolbar>(R.id.syncToolbar).setNavigationOnClickListener {
            finish()
        }

        logView = findViewById(R.id.syncLog)
        statusView = findViewById(R.id.syncStatus)
        logScroll = findViewById(R.id.syncLogScroll)

        demo.setLogger { message -> appendLog(message) }

        findViewById<MaterialButton>(R.id.startSyncDemo).setOnClickListener {
            logView.text = ""
            updateStatus(R.string.sync_interrupt_status_running)
            demo.startLockContention()
        }
        findViewById<MaterialButton>(R.id.interruptSyncWaiter).setOnClickListener {
            updateStatus(R.string.sync_interrupt_status_interrupted)
            demo.interruptWaitingThread()
        }
        findViewById<MaterialButton>(R.id.releaseSyncLock).setOnClickListener {
            updateStatus(R.string.sync_interrupt_status_releasing)
            demo.releaseLock()
        }
        findViewById<MaterialButton>(R.id.stopSyncDemo).setOnClickListener {
            updateStatus(R.string.sync_interrupt_status_idle)
            demo.stop()
            appendLog(getString(R.string.sync_interrupt_log_stopped))
        }

        appendLog(getString(R.string.sync_interrupt_log_ready))
    }

    override fun onDestroy() {
        demo.stop()
        super.onDestroy()
    }

    private fun updateStatus(statusRes: Int) {
        statusView.setText(statusRes)
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            if (logView.text.isEmpty()) {
                logView.text = message
            } else {
                logView.append("\n$message")
            }
            logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }
}
