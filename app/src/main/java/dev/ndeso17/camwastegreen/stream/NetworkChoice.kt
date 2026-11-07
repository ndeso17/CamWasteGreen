package dev.ndeso17.camwastegreen.stream

/**
 * Pilihan interface jaringan untuk jalur streaming.
 *
 * Enumerasi ini murni Kotlin: tidak ada `android.*` dan tidak ada akses
 * `NetworkInterface`. Pengumpulan interface nyata dilakukan pemanggil
 * (WP-35), sehingga pilihan ini dapat diuji di JVM.
 *
 * [label] adalah teks yang ditampilkan di UI; ia bukan nama interface sistem.
 */
enum class NetworkChoice(val label: String, val storageValue: String) {
    /** Bind ke semua interface (`0.0.0.0`). */
    ALL("Semua interface", "all"),

    /** Hanya Wi-Fi/LAN (`wlan*`). */
    WIFI_LAN("Wi-Fi / LAN", "wifi_lan"),

    /** Hanya USB tethering (`usb0`, `rndis0`, `eth0`). */
    USB_TETHERING("USB tethering", "usb_tethering");

    companion object {
        /** Alamat bind untuk [ALL]: seluruh interface IPv4. */
        const val ANY_ADDRESS = "0.0.0.0"

        /**
         * Nama interface yang diperlakukan sebagai USB tethering.
         *
         * Hanya nama yang terbukti dipakai: `usb0` dan `rndis0` pada tethering
         * USB Android, serta `eth0` pada adapter Ethernet USB.
         */
        private val USB_INTERFACE_NAMES = setOf("usb0", "rndis0", "eth0")

        /** Awalan nama interface Wi-Fi. */
        private const val WIFI_PREFIX = "wlan"

        /**
         * Petakan nama interface sistem ke [NetworkChoice].
         *
         * @return pilihan yang cocok, atau `null` bila interface tidak
         *   ditawarkan (mis. `lo`, `rmnet*`, `tun0`).
         */
        fun classify(interfaceName: String): NetworkChoice? {
            if (interfaceName in USB_INTERFACE_NAMES) return USB_TETHERING
            if (interfaceName.startsWith(WIFI_PREFIX)) return WIFI_LAN
            return null
        }
    }
}

/**
 * Satu alamat IPv4 yang dapat dipakai server.
 *
 * @param name nama interface sistem, mis. `wlan0`
 * @param address alamat IPv4 interface tersebut, mis. `192.168.1.10`
 */
data class NetworkCandidate(val name: String, val address: String)

/**
 * Saring kandidat sesuai [choice].
 *
 * [NetworkChoice.ALL] mengembalikan seluruh kandidat apa adanya, karena
 * binding ke `0.0.0.0` tidak memilih satu interface pun.
 */
fun selectCandidates(
    choice: NetworkChoice,
    candidates: List<NetworkCandidate>
): List<NetworkCandidate> = when (choice) {
    NetworkChoice.ALL -> candidates
    else -> candidates.filter { NetworkChoice.classify(it.name) == choice }
}

/**
 * Tentukan alamat bind untuk [choice].
 *
 * @return [NetworkChoice.ANY_ADDRESS] untuk [NetworkChoice.ALL]; alamat
 *   kandidat pertama yang cocok untuk pilihan lain; atau `null` bila tidak ada
 *   kandidat yang cocok. `null` berarti pilihan tidak dapat dipakai dan
 *   pemanggil wajib menampilkan "interface tidak tersedia", bukan diam-diam
 *   jatuh ke `0.0.0.0`.
 */
fun resolveBindAddress(
    choice: NetworkChoice,
    candidates: List<NetworkCandidate>
): String? = when (choice) {
    NetworkChoice.ALL -> NetworkChoice.ANY_ADDRESS
    else -> selectCandidates(choice, candidates).firstOrNull()?.address
}
