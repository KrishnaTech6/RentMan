package com.rentman.domain.billing

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeterPhotoNameTest {
    @Test
    fun `a plain jpg name is valid`() {
        assertTrue(MeterPhotoName.isValid("21b608927ab448b58084ed89.jpg"))
        assertTrue(MeterPhotoName.isValid("a-b_C1.jpg"))
    }

    @Test
    fun `anything that could leave the folder or is not a jpg is not valid`() {
        listOf("../x.jpg", "a/b.jpg", "a\\b.jpg", "x.jpg/..", ".jpg", "x.png", "x.jpg\n", "", "a".repeat(65) + ".jpg", "x.jpg.exe")
            .forEach { assertFalse(MeterPhotoName.isValid(it), it) }
    }
}
