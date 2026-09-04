package com.aichat.assistant.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun `isConfigured is false when any required field is blank`() {
        val base = ApiProviderSettings(
            baseUrl = "https://api.example.com/v1",
            apiKey = "sk-test",
            model = "gpt-4o-mini"
        )
        assertTrue(base.isConfigured)
        assertFalse(base.copy(baseUrl = "").isConfigured)
        assertFalse(base.copy(apiKey = "").isConfigured)
        assertFalse(base.copy(model = "").isConfigured)
    }

    @Test
    fun `default ApiProviderSettings is not configured`() {
        assertFalse(ApiProviderSettings().isConfigured)
    }

    @Test
    fun `message defaults to complete status and current sender`() {
        val message = Message(conversationId = "c1", sender = Sender.USER, content = "hi")
        assertEquals(MessageStatus.COMPLETE, message.status)
        assertEquals(Sender.USER, message.sender)
        assertTrue(message.id.isNotBlank())
    }
}
