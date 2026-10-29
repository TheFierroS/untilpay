package io.github.thefierros.untilpay

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `no tags means the app follows the system`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags(""))
    }

    @Test
    fun `a plain language tag is found`() {
        assertEquals(AppLanguage.TURKISH, AppLanguage.fromTags("tr"))
    }

    @Test
    fun `a region is ignored`() {
        assertEquals(AppLanguage.TURKISH, AppLanguage.fromTags("tr-TR"))
    }

    @Test
    fun `only the first language of a list counts`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en-US,tr"))
    }

    @Test
    fun `an unknown language falls back to the system`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags("de"))
    }
}
