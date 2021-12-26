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
import androidx.lifecycle.ViewModelProvider
import vib.usbtest.databinding.ActivityMainBinding
import java.lang.StringBuilder

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
private const val TAG = "usbTest"

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private val aliveMessage: ByteArray = byteArrayOf(2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private val ackMessage: ByteArray = byteArrayOf(2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private val readVersion: ByteArray = byteArrayOf(2, 9, 0, 4, 0, 0, 0, 0, 0, 2, 3, 15, 0, 0)

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }
    private lateinit var usbManager: UsbManager
    private lateinit var protocolHandler: Assa950dProtocolHandler

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
        protocolHandler = Assa950dProtocolHandler(usbManager)

        registerReceiver(usbReceiver, IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        })

        binding.printButton.setOnClickListener {
            binding.statusText.text = protocolHandler.getUsbDeviceInfo()
        }
        binding.connectButton.setOnClickListener { connect() }
        binding.doButton.setOnClickListener { doSomething() }
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

    private fun connect() {
        val result = protocolHandler.connect()
        addToLog(if (result) "Connected" else "Connection failed")
    }

    private fun doSomething() {
        val response = protocolHandler.sendCommand()
        addToLog("response = ${response.toUByteString()}")
    }
}