package com.rentman.data.drive

import platform.Foundation.NSUserDefaults

/** The fingerprint of what this iPhone last backed up to Drive. Not private, so the app's own settings store is enough. */
internal class DefaultsUploadLog : UploadLog {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun fingerprint(): String? = defaults.stringForKey(KEY)
    override fun record(fingerprint: String) = defaults.setObject(fingerprint, KEY)
    override fun clear() = defaults.removeObjectForKey(KEY)

    private companion object {
        const val KEY = "google_drive_synced_fingerprint"
    }
}
