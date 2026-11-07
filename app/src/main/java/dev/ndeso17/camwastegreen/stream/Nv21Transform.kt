package dev.ndeso17.camwastegreen.stream

/**
 * Transformasi bingkai NV21 tanpa dependensi Android.
 *
 * Susunan NV21:
 *  - `width * height` byte luma (Y), urutan baris.
 *  - kemudian pasangan byte chroma (V, U) yang berselang-seling untuk kisi
 *    chroma berukuran `chromaWidth x chromaHeight` dengan subsampling 2x2.
 *
 * Rotasi memakai arah berjam: 90 derajat memutar gambar searah jarum jam,
 * 270 derajat berlawanan arah jarum jam. Dimensi hasil bertukar pada 90 dan
 * 270 derajat.
 *
 * Kelas ini murni Kotlin tanpa `android.*` supaya bisa diuji di JVM.
 */
object Nv21Transform {

    /** Lebar kisi chroma untuk lebar luma tertentu. */
    fun chromaWidth(width: Int): Int = (width + 1) / 2

    /** Tinggi kisi chroma untuk tinggi luma tertentu. */
    fun chromaHeight(height: Int): Int = (height + 1) / 2

    /** Jumlah byte NV21 yang dibutuhkan untuk `width x height`. */
    fun nv21Size(width: Int, height: Int): Int {
        requireDimension(width, height)
        return width * height + chromaWidth(width) * chromaHeight(height) * 2
    }

    /**
     * Memutar bingkai NV21 sebesar [degrees] derajat searah jarum jam.
     *
     * @param yuv buffer NV21 sumber
     * @param width lebar luma sumber
     * @param height tinggi luma sumber
     * @param degrees sudut rotasi; hanya 0, 90, 180, atau 270 yang sah
     * @return buffer NV21 baru dengan ukuran yang sama
     * @throws IllegalArgumentException bila dimensi atau sudut tidak sah,
     *   atau buffer sumber lebih kecil dari ukuran NV21 yang dibutuhkan
     */
    fun rotateNv21(yuv: ByteArray, width: Int, height: Int, degrees: Int): ByteArray {
        requireDimension(width, height)
        require(degrees == 0 || degrees == 90 || degrees == 180 || degrees == 270) {
            "Sudut rotasi harus 0, 90, 180, atau 270 derajat, bukan $degrees"
        }

        val cw = chromaWidth(width)
        val ch = chromaHeight(height)
        val lumaSize = width * height
        val expectedSize = lumaSize + cw * ch * 2
        require(yuv.size >= expectedSize) {
            "Buffer NV21 terlalu kecil: ${yuv.size} byte, dibutuhkan $expectedSize byte"
        }

        if (degrees == 0) return yuv.copyOf(expectedSize)

        val outWidth = if (degrees == 180) width else height
        val outHeight = if (degrees == 180) height else width
        val outCw = if (degrees == 180) cw else ch
        val outCh = if (degrees == 180) ch else cw
        val out = ByteArray(expectedSize)

        // Luma.
        for (iy in 0 until height) {
            for (ix in 0 until width) {
                val src = iy * width + ix
                val (ox, oy) = mapLuma(ix, iy, width, height, degrees)
                out[oy * outWidth + ox] = yuv[src]
            }
        }

        // Chroma: pasangan V,U tetap berpasangan di setiap sel kisi.
        for (cy in 0 until ch) {
            for (cx in 0 until cw) {
                val srcPair = lumaSize + 2 * (cy * cw + cx)
                val (ox, oy) = mapChroma(cx, cy, cw, ch, degrees)
                val dstPair = lumaSize + 2 * (oy * outCw + ox)
                out[dstPair] = yuv[srcPair]
                out[dstPair + 1] = yuv[srcPair + 1]
            }
        }

        return out
    }

    private fun mapLuma(ix: Int, iy: Int, width: Int, height: Int, degrees: Int): Pair<Int, Int> =
        when (degrees) {
            90 -> Pair(height - 1 - iy, ix)
            180 -> Pair(width - 1 - ix, height - 1 - iy)
            270 -> Pair(iy, width - 1 - ix)
            else -> Pair(ix, iy)
        }

    private fun mapChroma(cx: Int, cy: Int, cw: Int, ch: Int, degrees: Int): Pair<Int, Int> =
        when (degrees) {
            90 -> Pair(ch - 1 - cy, cx)
            180 -> Pair(cw - 1 - cx, ch - 1 - cy)
            270 -> Pair(cy, cw - 1 - cx)
            else -> Pair(cx, cy)
        }

    private fun requireDimension(width: Int, height: Int) {
        require(width > 0 && height > 0) {
            "Dimensi tidak sah: ${width}x$height"
        }
    }
}
