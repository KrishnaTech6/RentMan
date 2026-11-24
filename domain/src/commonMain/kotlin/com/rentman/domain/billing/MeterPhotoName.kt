package com.rentman.domain.billing

/** What the app names its meter photos: a plain file name, so one from a backup file can never point outside the photo folder. */
object MeterPhotoName {
    private val PLAIN = Regex("""[A-Za-z0-9_-]{1,64}\.jpg""")

    fun isValid(name: String): Boolean = PLAIN.matches(name)
}
