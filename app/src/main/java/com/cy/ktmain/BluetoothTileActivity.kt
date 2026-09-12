package com.cy.ktmain

import android.app.StatusBarManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cy.ktmain.bluetooth.BluetoothDeviceTileService
import com.cy.ktmain.bluetooth.BluetoothTileController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class BluetoothTileActivity : AppCompatActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        render()
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        render()
    }

    private lateinit var statusView: TextView
    private lateinit var deviceGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_tile)
        setupEdgeToEdgeInsets(
            rootView = findViewById(R.id.bluetoothRoot),
            toolbar = findViewById(R.id.bluetoothToolbar)
        )

        statusView = findViewById(R.id.bluetoothStatus)
        deviceGroup = findViewById(R.id.deviceGroup)

        findViewById<MaterialToolbar>(R.id.bluetoothToolbar).setNavigationOnClickListener {
            finish()
        }
        findViewById<MaterialButton>(R.id.permissionButton).setOnClickListener {
            requestBluetoothPermission()
        }
        findViewById<MaterialButton>(R.id.enableButton).setOnClickListener {
            requestEnableBluetooth()
        }
        findViewById<MaterialButton>(R.id.addTileButton).setOnClickListener {
            requestAddTile()
        }
        findViewById<MaterialButton>(R.id.refreshButton).setOnClickListener {
            render()
        }

        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun requestBluetoothPermission() {
        val permissions = BluetoothTileController.requiredPermissions
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions)
        } else {
            render()
        }
    }

    private fun requestEnableBluetooth() {
        if (!BluetoothTileController.hasConnectPermission(this)) {
            Toast.makeText(this, R.string.bluetooth_tile_missing_permission, Toast.LENGTH_SHORT).show()
            requestBluetoothPermission()
            return
        }
        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private fun requestAddTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.bluetooth_tile_add_unsupported, Toast.LENGTH_LONG).show()
            return
        }

        val statusBarManager = getSystemService(StatusBarManager::class.java)
        val componentName = ComponentName(this, BluetoothDeviceTileService::class.java)
        statusBarManager.requestAddTileService(
            componentName,
            getString(R.string.bluetooth_tile_label),
            Icon.createWithResource(this, R.drawable.ic_qs_bluetooth),
            mainExecutor
        ) {
            Toast.makeText(this, R.string.bluetooth_tile_add_success, Toast.LENGTH_SHORT).show()
        }
    }

    private fun render() {
        val permissionText = if (BluetoothTileController.hasConnectPermission(this)) {
            getString(R.string.bluetooth_tile_status_granted)
        } else {
            getString(R.string.bluetooth_tile_status_denied)
        }
        val bluetoothText = if (BluetoothTileController.adapter(this)?.isEnabled == true) {
            getString(R.string.bluetooth_tile_status_on)
        } else {
            getString(R.string.bluetooth_tile_status_off)
        }
        val deviceText = BluetoothTileController.selectedDeviceLabel(this)
            ?: getString(R.string.bluetooth_tile_status_no_device)

        statusView.text = getString(
            R.string.bluetooth_tile_status_template,
            permissionText,
            bluetoothText,
            deviceText
        )

        renderDevices(BluetoothTileController.bondedDevices(this))
    }

    private fun renderDevices(devices: List<BluetoothDevice>) {
        deviceGroup.removeAllViews()
        if (devices.isEmpty()) {
            TextView(this).apply {
                text = getString(R.string.bluetooth_tile_empty_devices)
                setTextColor(getColor(R.color.lab_text_secondary))
                textSize = 14f
                deviceGroup.addView(this)
            }
            return
        }

        val selectedAddress = BluetoothTileController.selectedAddress(this)
        devices.forEachIndexed { index, device ->
            val radioButton = RadioButton(this).apply {
                id = index + 1
                text = device.displayLabel()
                tag = device
                textSize = 15f
                setTextColor(getColor(R.color.lab_text_primary))
                isChecked = device.address == selectedAddress
            }
            deviceGroup.addView(radioButton)
        }

        deviceGroup.setOnCheckedChangeListener { group, checkedId ->
            val device = group.findViewById<RadioButton>(checkedId)?.tag as? BluetoothDevice
                ?: return@setOnCheckedChangeListener
            BluetoothTileController.saveSelectedDevice(this, device)
            render()
        }
    }

    private fun BluetoothDevice.displayLabel(): String {
        val name = if (BluetoothTileController.hasConnectPermission(this@BluetoothTileActivity)) {
            name
        } else {
            null
        }
        return if (name.isNullOrBlank()) address else "$name\n$address"
    }
}
