package com.rentman.domain.billing

/** A `wa.me` link that opens the renter's chat with the message already typed. The landlord only has to press send. */
object WhatsAppLink {
    private const val HEX = "0123456789ABCDEF"

    /** [phone] is digits with country code (e.g. "919876543210"). An empty [phone] lets the landlord pick the chat, e.g. a group. */
    fun of(phone: String, message: String): String = "https://wa.me/$phone?text=${encode(message)}"

    /** Percent-encodes UTF-8 bytes, leaving only the characters a URL allows unescaped (RFC 3986). */
    private fun encode(text: String): String = buildString {
        for (byte in text.encodeToByteArray()) {
            val value = byte.toInt() and 0xFF
            val char = value.toChar()
            if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-_.~") {
                append(char)
            } else {
                append('%').append(HEX[value shr 4]).append(HEX[value and 0x0F])
            }
        }
    }
}
