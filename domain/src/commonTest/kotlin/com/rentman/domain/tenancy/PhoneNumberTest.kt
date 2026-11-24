package com.rentman.domain.tenancy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneNumberTest {
    @Test
    fun aTenDigitIndianMobileGetsTheCountryCode() {
        assertEquals("919876543210", PhoneNumber.normalize("98765 43210"))
        assertEquals("919876543210", PhoneNumber.normalize("9876543210"))
    }

    @Test
    fun anAlreadyInternationalIndianNumberIsKept() {
        assertEquals("919876543210", PhoneNumber.normalize("+91 98765 43210"))
        assertEquals("919876543210", PhoneNumber.normalize("919876543210"))
    }

    @Test
    fun aLeadingZeroIsDropped() {
        assertEquals("919876543210", PhoneNumber.normalize("09876543210"))
    }

    @Test
    fun anotherCountryNeedsAPlus() {
        assertEquals("14155552671", PhoneNumber.normalize("+1 (415) 555-2671"))
    }

    @Test
    fun aPlusNineOneNumberMustStillBeARealIndianMobile() {
        assertNull(PhoneNumber.normalize("+91 12345 67890")) // 91 is India, and the number doesn't start with 6-9
        assertNull(PhoneNumber.normalize("+91 98765 4321")) // too short
        assertEquals("919876543210", PhoneNumber.normalize("+91 98765 43210"))
    }

    @Test
    fun rejectsNumbersThatCannotBeRealMobiles() {
        assertNull(PhoneNumber.normalize("12345"))
        assertNull(PhoneNumber.normalize("5876543210")) // Indian mobiles start with 6-9
        assertNull(PhoneNumber.normalize("14155552671")) // 11 digits without a plus
        assertNull(PhoneNumber.normalize("not a number"))
        assertNull(PhoneNumber.normalize(""))
    }
}
