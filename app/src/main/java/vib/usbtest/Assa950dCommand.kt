package vib.usbtest

val aliveMessage: ByteArray = byteArrayOf(2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
val ackMessage: ByteArray = byteArrayOf(2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
val readVersion: ByteArray = byteArrayOf(2, 9, 0, 4, 0, 0, 0, 0, 0, 2, 3, 15, 0, 0)
val showErrorLog: ByteArray = byteArrayOf(2, 9, 0, 4, 0, 0, 0, 0, 0, 2, 3, 8, 0, 0)
val showActiveErrors: ByteArray = byteArrayOf(2, 9, 0, 4, 0, 0, 0, 0, 0, 2, 3, 11, 0, 0)
