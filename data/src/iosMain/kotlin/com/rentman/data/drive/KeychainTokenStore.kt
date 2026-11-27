package com.rentman.data.drive

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * The long-lived Google access in the iPhone Keychain, readable once the phone was unlocked after a restart, so a backup can run
 * then too. Keychain items outlive an uninstall, which is why Disconnect deletes it.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class KeychainTokenStore : TokenStore {

    override fun read(): String? = memScoped {
        val query = dictionary(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        if (status != errSecSuccess) return null
        val data = CFBridgingRelease(result.value) as? NSData ?: return null
        NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
    }

    override fun write(refreshToken: String) {
        clear()
        val data = CFBridgingRetain(NSString.create(string = refreshToken).dataUsingEncoding(NSUTF8StringEncoding))
        val item = dictionary(kSecValueData to data, kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock)
        SecItemAdd(item, null)
        CFRelease(item)
        CFRelease(data)
    }

    override fun clear() {
        val query = dictionary()
        SecItemDelete(query)
        CFRelease(query)
    }

    /** This app's one Keychain entry, with [extra] keys and values. The caller releases it. */
    private fun dictionary(vararg extra: Pair<CFTypeRef?, CFTypeRef?>): CFMutableDictionaryRef? {
        val service = CFBridgingRetain(NSString.create(string = SERVICE))
        val account = CFBridgingRetain(NSString.create(string = ACCOUNT))
        val entries = listOf(kSecClass to kSecClassGenericPassword, kSecAttrService to service, kSecAttrAccount to account) + extra
        val dictionary = CFDictionaryCreateMutable(null, entries.size.convert(), kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        entries.forEach { (key, value) -> CFDictionaryAddValue(dictionary, key, value) }
        CFRelease(service)
        CFRelease(account)
        return dictionary
    }

    private companion object {
        const val SERVICE = "com.rentman.drive"
        const val ACCOUNT = "google-refresh-token"
    }
}
