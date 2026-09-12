package com.cy.ktmain.bluetooth

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.cy.ktmain.R

class BluetoothDeviceTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (!BluetoothTileController.hasConnectPermission(this) ||
            BluetoothTileController.selectedDevice(this) == null ||
            BluetoothTileController.adapter(this)?.isEnabled != true
        ) {
            openConfigPage()
            return
        }

        qsTile?.apply {
            state = Tile.STATE_UNAVAILABLE
            label = getString(R.string.bluetooth_tile_label)
            updateTile()
        }
        BluetoothTileController.toggleSelectedDevice(this) {
            refreshTile()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_bluetooth)
        tile.label = getString(R.string.bluetooth_tile_label)

        when {
            !BluetoothTileController.hasConnectPermission(this) -> {
                tile.state = Tile.STATE_INACTIVE
                setSubtitle(tile, getString(R.string.bluetooth_tile_status_denied))
                tile.updateTile()
            }
            BluetoothTileController.selectedDevice(this) == null -> {
                tile.state = Tile.STATE_INACTIVE
                setSubtitle(tile, getString(R.string.bluetooth_tile_status_no_device))
                tile.updateTile()
            }
            BluetoothTileController.adapter(this)?.isEnabled != true -> {
                tile.state = Tile.STATE_INACTIVE
                setSubtitle(tile, getString(R.string.bluetooth_tile_status_off))
                tile.updateTile()
            }
            else -> BluetoothTileController.isSelectedDeviceConnected(this) { connected ->
                tile.state = if (connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                setSubtitle(tile, BluetoothTileController.selectedDeviceLabel(this).orEmpty())
                tile.updateTile()
            }
        }
    }

    private fun setSubtitle(tile: Tile, subtitle: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle
        }
    }

    private fun openConfigPage() {
        val intent = Intent(this, BluetoothTileActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
