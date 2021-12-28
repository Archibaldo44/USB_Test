package vib.usbtest

import android.hardware.usb.*
import android.util.Log
import kotlinx.coroutines.*
import java.lang.Runnable
import java.lang.StringBuilder
import android.hardware.usb.UsbRequest
import java.nio.ByteBuffer

private const val TAG = "Assa950dProtocolHandler"

class Assa950dProtocolHandler(
    private val usbManager: UsbManager
) {
    private var usbDevice: UsbDevice? = null
    private var usbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null
    private var usbConnection: UsbDeviceConnection? = null

    private var aliveJob: Job? = null

    /** Connect to USB device and start sending "alive" message as heartbeat. */
    fun connect(): Boolean {
        if (1 != usbManager.deviceList.size) {
            // fixme: inform user that there are more than 1 device
            Log.e(TAG, "ERROR: UsbHidHandler.connect(), device number != 1.")
            return false
        }
        usbDevice = usbManager.deviceList[usbManager.deviceList.keys.first()]
        if (1 != usbDevice?.interfaceCount) {
            // fixme: inform user that there are more than 1 interface
            Log.e(TAG, "ERROR: UsbHidHandler.connect(), number of interfaces != 1.")
            return false
        }

        usbInterface = usbDevice?.getInterface(0)
        if (UsbConstants.USB_CLASS_HID != usbInterface?.interfaceClass) {
            // fixme: inform user about wrong interface
            Log.e(TAG, "ERROR: UsbHidHandler.connect(), interface class in not HID.")
            return false
        }
        for (i in 0 until (usbInterface?.endpointCount ?: -1)) {
            when (usbInterface?.getEndpoint(i)?.direction) {
                UsbConstants.USB_DIR_IN -> inEndpoint = usbInterface?.getEndpoint(i)
                UsbConstants.USB_DIR_OUT -> outEndpoint = usbInterface?.getEndpoint(i)
            }
        }
        if (inEndpoint == null && outEndpoint == null) {
            // fixme: inform user
            Log.e(TAG, "ERROR: UsbHidHandler.connect(), number of endpoints != 2.")
            return false
        }

        usbConnection = usbManager.openDevice(usbDevice)
        if (usbConnection == null) {
            // fixme: inform user
            Log.e(TAG, "ERROR: UsbHidHandler.connect(), connection in null.")
            return false
        }

        if (usbConnection?.claimInterface(usbInterface, true) == false) {
            // fixme: inform user
            Log.e(TAG, "ERROR: UsbHidHandler.connect(), claimInterface failed.")
            usbConnection?.close()
            return false
        }

        //start sending alive heartbeat
        startHeartbeat()

        Thread(object : Runnable {
            override fun run() {
                val usbRequest = UsbRequest()
                while (true) {
                    val buffer: ByteBuffer = ByteBuffer.allocate(64)
                    usbRequest.initialize(usbConnection, inEndpoint)
                    if (usbRequest.queue(buffer)) {
                        if (usbConnection?.requestWait() === usbRequest) {
                            val result = String(buffer.array())
                            Log.i(TAG, "ASYNC RESULT: ${result} ")
                        }
                    }
                }
            }
        }).start()

        return true
    }

    fun disconnect() {
    }

    private fun startHeartbeat() {
        aliveJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val result =
                    usbConnection?.bulkTransfer(outEndpoint, aliveMessage, aliveMessage.size, 0)
                if (0 > result ?: -1) {
                    Log.i(TAG, "heartbeat failed")
                }
                Log.i(TAG, "heartbeat: ${System.currentTimeMillis()} ")
                delay(1000)
            }
        }
    }

    private fun stopHeartbeat() {
        aliveJob?.let {
            it.cancel()
            aliveJob = null
        }
    }

    /** For testing/debugging. Gets USB device info. */
    fun getUsbDeviceInfo(): String {
        val sb = StringBuilder()
        sb.append("# of devices: ${usbManager.deviceList.size}\n====\n")

        usbDevice?.let {
            sb.append("deviceId: ${it.deviceId} deviceName: ${it.deviceName}\n")
            sb.append("manufacturerName: ${it.manufacturerName}\n")
            sb.append("serialNumber: ${it.serialNumber}\n")
            sb.append("vendorId: ${it.vendorId} PID: ${it.productId}\n")
            sb.append("deviceClass: ${it.deviceClass} deviceSubclass: ${it.deviceSubclass}\n")
            sb.append("deviceProtocol: ${it.deviceProtocol}\n")
            sb.append("interfaceCount: ${it.interfaceCount}\n")
            sb.append("====\n")
        }
        usbInterface?.let {
            sb.append("id: ${it.id}\n")
            sb.append("name: ${it.name}\n")
            sb.append("interfaceClass: ${it.interfaceClass}\n")
            sb.append("interfaceSubclass: ${it.interfaceSubclass}\n")
            sb.append("interfaceProtocol: ${it.interfaceProtocol}\n")
            sb.append("alternateSetting: ${it.alternateSetting}\n")
            sb.append("endpointCount: ${it.endpointCount}\n")
            sb.append("====\n")
        }
        inEndpoint?.let {
            sb.append("inEndpoint address: ${it.address}\n")
            sb.append("inEndpoint attributes: ${it.attributes}\n")
            sb.append("inEndpoint direction: ${it.direction}\n")
            sb.append("inEndpoint endpointNumber: ${it.endpointNumber}\n")
            sb.append("inEndpoint interval: ${it.interval}\n")
            sb.append("inEndpoint maxPacketSize: ${it.maxPacketSize}\n")
            sb.append("inEndpoint type: ${it.type}\n")
            sb.append("====\n")
        }
        outEndpoint?.let {
            sb.append("outEndpoint address: ${it.address}\n")
            sb.append("outEndpoint attributes: ${it.attributes}\n")
            sb.append("outEndpoint direction: ${it.direction}\n")
            sb.append("outEndpoint endpointNumber: ${it.endpointNumber}\n")
            sb.append("outEndpoint interval: ${it.interval}\n")
            sb.append("outEndpoint maxPacketSize: ${it.maxPacketSize}\n")
            sb.append("outEndpoint type: ${it.type}\n")
            sb.append("====\n")
        }
        return sb.toString()
    }

    fun sendCommand(): ByteArray {
        // TODO: check if connection is alive
        // stop heartbeat
        stopHeartbeat()
        // send a command
//        var result = usbConnection?.bulkTransfer(outEndpoint, readVersion, readVersion.size, 0)
        var result =
            usbConnection?.bulkTransfer(outEndpoint, showActiveErrors, showActiveErrors.size, 0)
        if (0 > result ?: -1) {
            Log.i(TAG, "Failed to send 'read version'.")
        }
        // receive a reply
//        val response = ByteArray(64)
//        result = usbConnection?.bulkTransfer(inEndpoint, response, response.size, 3000)
//        if (0 > result ?: -1) {
//            Log.i(TAG, "Failed to read a reply.")
//        }
        // send ACK
//        result = usbConnection?.bulkTransfer(outEndpoint, ackMessage, ackMessage.size, 0)
//        if (0 > result ?: -1) {
//            Log.i(TAG, "Failed to send ACK.")
//        }
        // start heartbeat
        startHeartbeat()

//        return response
        return byteArrayOf(0, 1, 2, 3)
    }
}