package dev.ndeso17.camwastegreen.stream

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit test JVM murni untuk [Nv21Transform]. Tidak memakai `android.*` dan
 * tidak membutuhkan perangkat.
 */
class Nv21TransformTest {

    /** Membangun bingkai NV21 dengan luma dan chroma yang semuanya berbeda. */
    private fun buildFrame(width: Int, height: Int): ByteArray {
        val lumaSize = width * height
        val frame = ByteArray(Nv21Transform.nv21Size(width, height))
        for (i in 0 until lumaSize) {
            frame[i] = (i % 200 + 1).toByte()
        }
        val cw = Nv21Transform.chromaWidth(width)
        val ch = Nv21Transform.chromaHeight(height)
        for (cell in 0 until cw * ch) {
            frame[lumaSize + cell * 2] = (10 + cell).toByte()      // V
            frame[lumaSize + cell * 2 + 1] = (100 + cell).toByte() // U
        }
        return frame
    }

    private fun lumaValues(frame: ByteArray, pixelCount: Int): IntArray =
        IntArray(pixelCount) { frame[it].toInt() and 0xFF }

    private fun chromaValues(frame: ByteArray, cellCount: Int): IntArray {
        val lumaSize = frame.size - cellCount * 2
        return IntArray(cellCount * 2) { frame[lumaSize + it].toInt() and 0xFF }
    }

    @Test
    fun rotateZero_isIdentityAndReturnsCopy() {
        val width = 5
        val height = 3
        val frame = buildFrame(width, height)

        val rotated = Nv21Transform.rotateNv21(frame, width, height, 0)

        assertArrayEquals(frame, rotated)
        assertNotSame(frame, rotated)
    }

    @Test
    fun rotateNinety_mapsLumaClockwiseAndSwapsDimensions() {
        // Luma sumber 3x2:
        //   1 2 3
        //   4 5 6
        // Setelah 90 derajat searah jarum jam menjadi 2x3:
        //   4 1
        //   5 2
        //   6 3
        val width = 3
        val height = 2
        val frame = buildFrame(width, height)

        val rotated = Nv21Transform.rotateNv21(frame, width, height, 90)

        assertEquals(frame.size, rotated.size)
        assertArrayEquals(
            intArrayOf(4, 1, 5, 2, 6, 3),
            lumaValues(rotated, width * height)
        )
    }

    @Test
    fun rotateNinety_thenTwoSeventy_returnsOriginalOnOddDimensions() {
        val width = 5
        val height = 3
        val frame = buildFrame(width, height)

        val rotated = Nv21Transform.rotateNv21(frame, width, height, 90)
        val restored = Nv21Transform.rotateNv21(rotated, height, width, 270)

        assertArrayEquals(frame, restored)
    }

    @Test
    fun rotateOneEighty_twiceReturnsOriginalOnOddDimensions() {
        val width = 5
        val height = 3
        val frame = buildFrame(width, height)

        val once = Nv21Transform.rotateNv21(frame, width, height, 180)
        val twice = Nv21Transform.rotateNv21(once, width, height, 180)

        assertArrayEquals(frame, twice)
    }

    @Test
    fun rotateOneEighty_flipsLumaBothAxes() {
        // Luma sumber 3x2:
        //   1 2 3
        //   4 5 6
        // Setelah 180 derajat:
        //   6 5 4
        //   3 2 1
        val width = 3
        val height = 2
        val frame = buildFrame(width, height)

        val rotated = Nv21Transform.rotateNv21(frame, width, height, 180)

        assertArrayEquals(
            intArrayOf(6, 5, 4, 3, 2, 1),
            lumaValues(rotated, width * height)
        )
    }

    @Test
    fun rotateNinety_keepsChromaPairsTogether() {
        // Kisi chroma 2x2 dengan sel p0..p3 berurutan baris.
        // 90 derajat searah jarum jam memindahkan sel menjadi p2,p0,p3,p1.
        val width = 4
        val height = 4
        val frame = buildFrame(width, height)
        val cells = Nv21Transform.chromaWidth(width) * Nv21Transform.chromaHeight(height)

        val rotated = Nv21Transform.rotateNv21(frame, width, height, 90)

        assertArrayEquals(
            intArrayOf(12, 102, 10, 100, 13, 103, 11, 101),
            chromaValues(rotated, cells)
        )
    }

    @Test
    fun oddDimensions_roundChromaGridUp() {
        assertEquals(27, Nv21Transform.nv21Size(5, 3))
        assertEquals(3, Nv21Transform.chromaWidth(5))
        assertEquals(2, Nv21Transform.chromaHeight(3))

        val frame = buildFrame(5, 3)
        val rotated = Nv21Transform.rotateNv21(frame, 5, 3, 270)

        assertEquals(frame.size, rotated.size)
    }

    @Test
    fun invalidDegrees_rejected() {
        val frame = buildFrame(4, 4)
        for (degrees in intArrayOf(45, 360, -90, 1)) {
            assertThrows(IllegalArgumentException::class.java) {
                Nv21Transform.rotateNv21(frame, 4, 4, degrees)
            }
        }
    }

    @Test
    fun invalidDimensions_rejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Nv21Transform.nv21Size(0, 4)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Nv21Transform.rotateNv21(ByteArray(0), 4, 0, 90)
        }
    }

    @Test
    fun bufferTooSmall_rejected() {
        val frame = buildFrame(4, 4)
        assertThrows(IllegalArgumentException::class.java) {
            Nv21Transform.rotateNv21(frame.copyOf(frame.size - 1), 4, 4, 90)
        }
    }
}
