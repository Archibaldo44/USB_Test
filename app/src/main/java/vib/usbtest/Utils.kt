package vib.usbtest

/** For testing/debugging. Returns a string of unsigned bytes. */
fun ByteArray.toUByteString(): String {
    val sb = StringBuilder()
    for (b in this) {
        sb.append("${b.toUByte()} ")
    }
    return sb.toString()
}