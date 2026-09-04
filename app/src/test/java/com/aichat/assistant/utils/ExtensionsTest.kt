package com.aichat.assistant.utils

import com.aichat.assistant.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `greeting matches expected time-of-day bucket`() {
        assertEquals(R.string.home_greeting_morning, greetingStringRes(hourOfDay = 8))
        assertEquals(R.string.home_greeting_noon, greetingStringRes(hourOfDay = 13))
        assertEquals(R.string.home_greeting_afternoon, greetingStringRes(hourOfDay = 18))
        assertEquals(R.string.home_greeting_evening, greetingStringRes(hourOfDay = 23))
        assertEquals(R.string.home_greeting_evening, greetingStringRes(hourOfDay = 2))
    }
}
