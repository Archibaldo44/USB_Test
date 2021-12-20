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

    private fun sendSomething() {

    }
}