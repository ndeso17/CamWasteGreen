package dev.ndeso17.camwastegreen.stream

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Parsing dan serialisasi pilihan interface (WP-34).
 *
 * Objek ini murni Kotlin: tanpa dependensi platform Android. Pembungkus
 * penyimpanan ada di [StreamInterfacePrefs] pada berkas yang sama, dan hanya
 * pembungkus itu yang mengimpor `android.content`.
 */
object StreamInterfaceChoice {

    /** Nilai tersimpan bila kunci belum ada atau isinya tidak dikenal. */
    val DEFAULT: NetworkChoice = NetworkChoice.ALL

    /**
     * Ubah nilai tersimpan menjadi [NetworkChoice].
     *
     * `null`, string kosong, dan nilai yang tidak dikenal semuanya
     * menghasilkan [DEFAULT] (`ALL`). Memilih `ALL` saat ragu lebih aman
     * daripada menolak start: operator selalu bisa menyempitkan pilihan, dan
     * `/status` tetap dapat dihubungi.
     */
    fun parseChoice(raw: String?): NetworkChoice {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return DEFAULT
        return NetworkChoice.values().firstOrNull { it.storageValue == trimmed } ?: DEFAULT
    }

    /** Nilai yang disimpan untuk [choice]. */
    fun serialize(choice: NetworkChoice): String = choice.storageValue
}

/**
 * Penyimpanan pilihan interface pada `SharedPreferences`.
 *
 * Pembungkus tipis: seluruh keputusan parsing ada di
 * [StreamInterfaceChoice], sehingga dapat diuji tanpa perangkat.
 */
class StreamInterfacePrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Pilihan tersimpan, atau [StreamInterfaceChoice.DEFAULT] bila belum ada. */
    fun getChoice(): NetworkChoice =
        StreamInterfaceChoice.parseChoice(prefs.getString(KEY_CHOICE, null))

    /** Simpan [choice] sebagai pilihan yang berlaku saat server distart. */
    fun setChoice(choice: NetworkChoice) {
        prefs.edit { putString(KEY_CHOICE, StreamInterfaceChoice.serialize(choice)) }
    }

    companion object {
        const val PREFS_NAME = "stream_settings"
        const val KEY_CHOICE = "interface_choice"
    }
}
