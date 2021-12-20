package vib.usbtest

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import vib.usbtest.databinding.ActivityMainBinding
import java.lang.StringBuilder

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
private const val TAG = "usbTest"

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private val aliveMessage: ByteArray = byteArrayOf(2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private val readVersion: ByteArray = byteArrayOf(2, 9, 0, 4, 0, 0, 0, 0, 0, 2, 3, 15, 0, 0)

    private lateinit var binding: ActivityMainBinding
    private lateinit var usbManager: UsbManager

    private var usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when {
                ACTION_USB_PERMISSION == intent.action -> {
                    onActionUsbPermission(this)
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action -> {
                    onActionUsbDeviceAttached()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action -> {
                    onActionUsbDeviceDetached()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        registerReceiver(usbReceiver, IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        })

        binding.testButton.setOnClickListener {
            printStatus()
        }

        binding.doButton.setOnClickListener {
            doSomething()
        }
    }

    private fun onActionUsbPermission(broadcastReceiver: BroadcastReceiver) {
        addToLog("BroadcastReceiver.onActionUsbPermission")
        synchronized(broadcastReceiver) {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                device?.apply {
                    //call method to set up device communication
                }
            } else {
                addToLog("permission denied for device $device")
            }
        }
    }

    private fun onActionUsbDeviceAttached() {
        addToLog("BroadcastReceiver.onActionUsbDeviceAttached")
    }

    private fun onActionUsbDeviceDetached() {
        addToLog("BroadcastReceiver.onActionUsbDeviceDetached")
    }

    private fun addToLog(message: String) {
        val existingText = binding.logText.text
        binding.logText.text = "${existingText}\n${message}"
    }

    private fun printStatus() {
        val sb = StringBuilder()
        sb.append("# of devices: ${usbManager.deviceList.size}\n")

        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        deviceList.forEach { (key, usbDevice) ->
            sb.append("key: ${key}\n")
            sb.append("id: ${usbDevice.deviceId} name: ${usbDevice.deviceName}\n")
            sb.append("manufacturer: ${usbDevice.manufacturerName}\n")
            sb.append("serial #: ${usbDevice.serialNumber}\n")
            sb.append("VID: ${usbDevice.vendorId} PID: ${usbDevice.productId}\n")
            sb.append("class: ${usbDevice.deviceClass} subclass: ${usbDevice.deviceSubclass}\n")
            sb.append("protocol: ${usbDevice.deviceProtocol}\n")
            sb.append("interfaceCount: ${usbDevice.interfaceCount}\n")
        }

        binding.statusText.text = sb.toString()
    }

    private fun printInterfaceAndEndpointsData(device: UsbDevice) {
        device.getInterface(0).also { intf ->
            val sb = StringBuilder()
            sb.append("id: ${intf.id}\n")
            sb.append("name: ${intf.name}\n")
            sb.append("interfaceClass: ${intf.interfaceClass}\n")
            sb.append("interfaceSubclass: ${intf.interfaceSubclass}\n")
            sb.append("interfaceProtocol: ${intf.interfaceProtocol}\n")
            sb.append("alternateSetting: ${intf.alternateSetting}\n")
            sb.append("endpointCount: ${intf.endpointCount}\n")
            sb.append("====\n")

            intf.getEndpoint(0)?.also { endpoint ->
                sb.append("endpoint0 address: ${endpoint.address}\n")
                sb.append("endpoint0 attributes: ${endpoint.attributes}\n")
                sb.append("endpoint0 direction: ${endpoint.direction}\n")
                sb.append("endpoint0 endpointNumber: ${endpoint.endpointNumber}\n")
                sb.append("endpoint0 interval: ${endpoint.interval}\n")
                sb.append("endpoint0 maxPacketSize: ${endpoint.maxPacketSize}\n")
                sb.append("endpoint0 type: ${endpoint.type}\n")
            }
            intf.getEndpoint(1)?.also { endpoint ->
                sb.append("endpoint1 address: ${endpoint.address}\n")
                sb.append("endpoint1 attributes: ${endpoint.attributes}\n")
                sb.append("endpoint1 direction: ${endpoint.direction}\n")
                sb.append("endpoint1 endpointNumber: ${endpoint.endpointNumber}\n")
                sb.append("endpoint1 interval: ${endpoint.interval}\n")
                sb.append("endpoint1 maxPacketSize: ${endpoint.maxPacketSize}\n")
                sb.append("endpoint1 type: ${endpoint.type}\n")
            }
            binding.statusText.text = sb.toString()
        }
    }

    private fun doSomething() {
        if (1 != usbManager.deviceList.size) {
            addToLog("# of devices <> 1")
            return
        }
        val key = usbManager.deviceList.keys.first()
        val device = usbManager.deviceList[key]
        if (device == null) {
            addToLog("usbDevices == null")
            return
        }
        printInterfaceAndEndpointsData(device)

        val inEndpoint = device.getInterface(0).getEndpoint(0)
        val outEndpoint = device.getInterface(0).getEndpoint(1)

        val connection = usbManager.openDevice(device)
        if (connection == null) {
            addToLog("Error: connection is null!")
            return
        }
        val claimResult = connection.claimInterface(device.getInterface(0), true)
        if (!claimResult) {
            addToLog("Error: claimInterface failed!")
            connection.close()
            return
        }

        var result = connection.bulkTransfer(outEndpoint, aliveMessage, aliveMessage.size, 0)
        addToLog("alive result = $result")
        result = connection.bulkTransfer(outEndpoint, readVersion, readVersion.size, 0)
        addToLog("readVersion result = $result")

        val response = ByteArray(64)
        result = connection.bulkTransfer(inEndpoint, response, response.size, 3000)
        addToLog("response result = $result")
    }
}