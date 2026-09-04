package com.aichat.assistant.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for the OpenAI-compatible `/chat/completions` endpoint. Kept deliberately close to
 * the public OpenAI schema (see master spec §2) so most self-hosted/compatible servers work
 * unmodified; a future provider just needs its own mapper into/out of this same shape, or its own
 * sibling file if the shape diverges too much to share.
 */
@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false
)

@Serializable
data class ChatCompletionChoiceDto(
    val index: Int = 0,
    val message: ChatMessageDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatCompletionResponseDto(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatCompletionChoiceDto> = emptyList()
)

/** One `data:` line of a streamed response. */
@Serializable
data class ChatCompletionChunkDto(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatCompletionChunkChoiceDto> = emptyList()
)

@Serializable
data class ChatCompletionChunkChoiceDto(
    val index: Int = 0,
    val delta: ChatCompletionDeltaDto = ChatCompletionDeltaDto(),
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatCompletionDeltaDto(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class ModelInfoDto(
    val id: String,
    @SerialName("object") val objectType: String? = null
)

@Serializable
data class ModelListResponseDto(
    @SerialName("object") val objectType: String? = null,
    val data: List<ModelInfoDto> = emptyList()
)

/** Standard OpenAI-style error envelope: `{"error": {"message", "type", "code"}}`. */
@Serializable
data class ApiErrorEnvelopeDto(
    val error: ApiErrorBodyDto? = null
)

@Serializable
data class ApiErrorBodyDto(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
