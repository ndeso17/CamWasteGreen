package dev.ndeso17.camwastegreen

object FrameBuffer {
    @Volatile
    var latestFrame: ByteArray? = null
}