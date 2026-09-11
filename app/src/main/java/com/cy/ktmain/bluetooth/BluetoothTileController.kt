package com.cy.ktmain.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BluetoothTileController {

    private const val PREFS = "bluetooth_tile_demo"
    private const val KEY_SELECTED_ADDRESS = "selected_address"

    val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }

    fun hasConnectPermission(context: Context): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun adapter(context: Context): BluetoothAdapter? {
        return context.getSystemService(BluetoothManager::class.java)?.adapter
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        return hasConnectPermission(context) && adapter(context)?.isEnabled == true
    }

    fun bondedDevices(context: Context): List<BluetoothDevice> {
        if (!hasConnectPermission(context)) return emptyList()
        return adapter(context)?.bondedDevices?.sortedBy { it.displayName(context) }.orEmpty()
    }

    fun saveSelectedDevice(context: Context, device: BluetoothDevice) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_ADDRESS, device.address)
            .apply()
    }

    fun selectedAddress(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_ADDRESS, null)
    }

    fun selectedDevice(context: Context): BluetoothDevice? {
        val address = selectedAddress(context) ?: return null
        return runCatching { adapter(context)?.getRemoteDevice(address) }.getOrNull()
    }

    fun selectedDeviceLabel(context: Context): String? {
        val device = selectedDevice(context) ?: return null
        return device.displayName(context)
    }

    fun toggleSelectedDevice(context: Context, callback: (BluetoothTileResult) -> Unit) {
        val device = selectedDevice(context)
        when {
            !hasConnectPermission(context) -> callback(BluetoothTileResult.MissingPermission)
            adapter(context)?.isEnabled != true -> callback(BluetoothTileResult.BluetoothOff)
            device == null -> callback(BluetoothTileResult.NoSelectedDevice)
            else -> isDeviceConnected(context, device) { connected ->
                invokeProfiles(
                    context = context,
                    device = device,
                    methodName = if (connected) "disconnect" else "connect",
                    callback = callback
                )
            }
        }
    }

    fun isSelectedDeviceConnected(context: Context, callback: (Boolean) -> Unit) {
        val device = selectedDevice(context)
        if (!hasConnectPermission(context) || adapter(context)?.isEnabled != true || device == null) {
            callback(false)
            return
        }
        isDeviceConnected(context, device, callback)
    }

    private fun isDeviceConnected(
        context: Context,
        device: BluetoothDevice,
        callback: (Boolean) -> Unit
    ) {
        queryProfiles(context, device) { proxies ->
            var connected = false
            proxies.forEach { (profile, proxy) ->
                connected = connected || runCatching {
                    proxy.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
                }.getOrDefault(false)
                closeProfile(context, profile, proxy)
            }
            callback(connected)
        }
    }

    private fun invokeProfiles(
        context: Context,
        device: BluetoothDevice,
        methodName: String,
        callback: (BluetoothTileResult) -> Unit
    ) {
        queryProfiles(context, device) { proxies ->
            val success = proxies.any { (profile, proxy) ->
                runCatching {
                    val method = proxy.javaClass.getMethod(methodName, BluetoothDevice::class.java)
                    method.invoke(proxy, device) == true
                }.getOrDefault(false).also {
                    closeProfile(context, profile, proxy)
                }
            }
            callback(if (success) BluetoothTileResult.Success else BluetoothTileResult.OperationFailed)
        }
    }

    private fun queryProfiles(
        context: Context,
        device: BluetoothDevice,
        callback: (List<Pair<Int, BluetoothProfile>>) -> Unit
    ) {
        val adapter = adapter(context) ?: run {
            callback(emptyList())
            return
        }
        val profiles = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)
        val proxies = mutableListOf<Pair<Int, BluetoothProfile>>()
        var pending = profiles.size

        fun completeOne() {
            pending -= 1
            if (pending == 0) callback(proxies)
        }

        profiles.forEach { profile ->
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(connectedProfile: Int, proxy: BluetoothProfile) {
                    if (runCatching { proxy.getConnectionState(device) }.isSuccess) {
                        proxies += connectedProfile to proxy
                    } else {
                        closeProfile(context, connectedProfile, proxy)
                    }
                    completeOne()
                }

                override fun onServiceDisconnected(disconnectedProfile: Int) = completeOne()
            }

            val started = runCatching {
                adapter.getProfileProxy(context.applicationContext, listener, profile)
            }.getOrDefault(false)
            if (!started) completeOne()
        }
    }

    private fun closeProfile(context: Context, profile: Int, proxy: BluetoothProfile) {
        runCatching { adapter(context)?.closeProfileProxy(profile, proxy) }
    }

    fun BluetoothDevice.displayName(context: Context): String {
        return if (hasConnectPermission(context)) {
            name.orEmpty().ifBlank { address }
        } else {
            address
        }
    }
}

enum class BluetoothTileResult {
    Success,
    MissingPermission,
    BluetoothOff,
    NoSelectedDevice,
    OperationFailed
}
