package com.rentman.domain.billing

import kotlin.test.Test
import kotlin.test.assertEquals

class WhatsAppLinkTest {
    @Test
    fun buildsAWaDotMeLinkWithTheMessagePercentEncoded() {
        assertEquals(
            "https://wa.me/919876543210?text=Hi%20Ravi%2C%0ATotal%3A%20%E2%82%B98%2C380",
            WhatsAppLink.of("919876543210", "Hi Ravi,\nTotal: ₹8,380"),
        )
    }

    @Test
    fun lettersDigitsAndTheSafeSymbolsAreLeftAlone() {
        assertEquals("https://wa.me/1?text=AZaz09-_.~", WhatsAppLink.of("1", "AZaz09-_.~"))
    }

    @Test
    fun spacesBecomePercent20NotPlusAndNonAsciiIsEncodedAsUtf8() {
        assertEquals("https://wa.me/1?text=a%20b", WhatsAppLink.of("1", "a b"))
        assertEquals("https://wa.me/1?text=%E2%86%92", WhatsAppLink.of("1", "→"))
        assertEquals("https://wa.me/1?text=%26%3D%23%25%3F", WhatsAppLink.of("1", "&=#%?")) // would break a URL if left raw
    }

    @Test
    fun anEmptyMessageStillGivesAValidLink() {
        assertEquals("https://wa.me/1?text=", WhatsAppLink.of("1", ""))
    }
}
