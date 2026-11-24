package com.rentman.domain.tenancy

object PhoneNumber {
    private const val INDIA = "91"
    private val INDIAN_MOBILE_START = '6'..'9'
    private val OTHER_COUNTRY_LENGTH = 11..15

    /**
     * The digits with the country code, ready for a wa.me link (e.g. "919876543210"), or null if the input
     * can't be a mobile number. A plain 10-digit number is taken as Indian; another country needs a leading "+".
     */
    fun normalize(raw: String): String? {
        val digits = raw.filter { it in '0'..'9' }
        return when {
            digits.length == 10 && digits[0] in INDIAN_MOBILE_START -> INDIA + digits
            digits.length == 11 && digits[0] == '0' && digits[1] in INDIAN_MOBILE_START -> INDIA + digits.drop(1)
            digits.length == 12 && digits.startsWith(INDIA) && digits[2] in INDIAN_MOBILE_START -> digits
            // 91 is India, so a "+91" number that failed the Indian rules above is not accepted as "another country".
            raw.trim().startsWith("+") && !digits.startsWith(INDIA) && digits.length in OTHER_COUNTRY_LENGTH -> digits
            else -> null
        }
    }
}
