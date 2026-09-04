package com.aichat.assistant.data.api

import com.aichat.assistant.domain.model.AppError
import com.aichat.assistant.domain.model.ApiProviderSettings
import com.aichat.assistant.domain.model.ConnectionTestResult
import com.aichat.assistant.domain.model.Message
import com.aichat.assistant.domain.model.Sender
import com.aichat.assistant.domain.repository.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * Talks to any OpenAI-compatible `/chat/completions` server. Base URL, key, and model all come
 * from [ApiProviderSettings] at call time — nothing here is hard-coded, per master spec §2/§22.
 *
 * A single [OkHttpClient] is reused across calls (it isn't bound to a host), which also means the
 * only place capable of writing request/response detail to Logcat is [loggingInterceptor], and
 * that interceptor is intentionally scoped to method + response code only (never headers or
 * bodies), so the API key can never end up in device logs even in a debug build.
 */
class ChatApiClient(private val debugLogging: Boolean) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (debugLogging) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private fun normalizedBaseUrl(baseUrl: String): String = baseUrl.trimEnd('/')

    private fun authorizedRequest(url: String, apiKey: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")

    private fun toDto(message: Message): ChatMessageDto = ChatMessageDto(
        role = if (message.sender == Sender.USER) "user" else "assistant",
        content = message.content
    )

    private fun buildRequestPayload(
        settings: ApiProviderSettings,
        history: List<Message>,
        userMessage: String,
        stream: Boolean
    ): ChatCompletionRequestDto {
        val messages = buildList {
            if (settings.systemPrompt.isNotBlank()) {
                add(ChatMessageDto(role = "system", content = settings.systemPrompt))
            }
            addAll(history.map(::toDto))
            add(ChatMessageDto(role = "user", content = userMessage))
        }
        return ChatCompletionRequestDto(
            model = settings.model,
            messages = messages,
            temperature = settings.temperature,
            maxTokens = settings.maxTokens,
            stream = stream
        )
    }

    /**
     * Emits each incremental text delta as it streams in. Terminates normally on `[DONE]` or a
     * finish_reason; emits a single [Outcome.Err] and stops on any failure so the collector never
     * has to guess whether the flow is "done" or "broken".
     */
    fun streamChatCompletion(
        settings: ApiProviderSettings,
        history: List<Message>,
        userMessage: String
    ): Flow<Outcome<String>> = flow {
        val payload = buildRequestPayload(settings, history, userMessage, stream = true)
        val body = json.encodeToString(ChatCompletionRequestDto.serializer(), payload)
            .toRequestBody(jsonMediaType)
        val request = authorizedRequest("${normalizedBaseUrl(settings.baseUrl)}/chat/completions", settings.apiKey)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(Outcome.Err(mapHttpError(response.code, response.body?.string())))
                    return@use
                }
                val source = response.body?.source()
                if (source == null) {
                    emit(Outcome.Err(AppError.EmptyResponse()))
                    return@use
                }
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank() || !line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val chunk = runCatching {
                        json.decodeFromString(ChatCompletionChunkDto.serializer(), data)
                    }.getOrNull() ?: continue
                    val delta = chunk.choices.firstOrNull()?.delta?.content
                    if (!delta.isNullOrEmpty()) {
                        emit(Outcome.Ok(delta))
                    }
                }
            }
        } catch (e: Exception) {
            emit(Outcome.Err(mapException(e)))
        }
    }.flowOn(Dispatchers.IO)

    /** Non-streaming fallback used when the user has streaming turned off in Settings. */
    suspend fun sendChatCompletion(
        settings: ApiProviderSettings,
        history: List<Message>,
        userMessage: String
    ): Outcome<String> = withContext(Dispatchers.IO) {
        val payload = buildRequestPayload(settings, history, userMessage, stream = false)
        val body = json.encodeToString(ChatCompletionRequestDto.serializer(), payload)
            .toRequestBody(jsonMediaType)
        val request = authorizedRequest("${normalizedBaseUrl(settings.baseUrl)}/chat/completions", settings.apiKey)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string()
                if (!response.isSuccessful) {
                    return@withContext Outcome.Err(mapHttpError(response.code, text))
                }
                if (text.isNullOrBlank()) {
                    return@withContext Outcome.Err(AppError.EmptyResponse())
                }
                val parsed = runCatching {
                    json.decodeFromString(ChatCompletionResponseDto.serializer(), text)
                }.getOrNull() ?: return@withContext Outcome.Err(AppError.EmptyResponse())
                val content = parsed.choices.firstOrNull()?.message?.content
                if (content.isNullOrEmpty()) {
                    Outcome.Err(AppError.EmptyResponse())
                } else {
                    Outcome.Ok(content)
                }
            }
        } catch (e: Exception) {
            Outcome.Err(mapException(e))
        }
    }

    suspend fun fetchModels(settings: ApiProviderSettings): Outcome<List<String>> = withContext(Dispatchers.IO) {
        val request = authorizedRequest("${normalizedBaseUrl(settings.baseUrl)}/models", settings.apiKey)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string()
                if (!response.isSuccessful) {
                    return@withContext Outcome.Err(mapHttpError(response.code, text))
                }
                val parsed = runCatching {
                    json.decodeFromString(ModelListResponseDto.serializer(), text.orEmpty())
                }.getOrNull() ?: return@withContext Outcome.Err(AppError.EmptyResponse())
                Outcome.Ok(parsed.data.map { it.id }.sorted())
            }
        } catch (e: Exception) {
            Outcome.Err(mapException(e))
        }
    }

    /**
     * Tries `/models` first (cheap, no token usage); if the provider doesn't support it, falls
     * back to a 1-token chat completion just to prove the key + base URL + model actually work.
     */
    suspend fun testConnection(settings: ApiProviderSettings): ConnectionTestResult {
        val models = fetchModels(settings)
        if (models is Outcome.Ok) {
            return ConnectionTestResult.Success(models.value)
        }
        val probe = sendChatCompletion(
            settings = settings.copy(maxTokens = 1),
            history = emptyList(),
            userMessage = "ping"
        )
        return when (probe) {
            is Outcome.Ok -> ConnectionTestResult.Success(emptyList())
            is Outcome.Err -> ConnectionTestResult.Failure(probe.error)
        }
    }

    private fun mapException(e: Exception): AppError = when (e) {
        is SocketTimeoutException -> AppError.Timeout(e.message)
        is UnknownHostException -> AppError.InvalidBaseUrl(e.message)
        is SSLException -> AppError.InvalidBaseUrl(e.message)
        is IOException -> AppError.NoInternet(e.message)
        else -> AppError.Unknown(e.message)
    }

    private fun mapHttpError(code: Int, bodyText: String?): AppError {
        val parsedMessage = bodyText?.let { text ->
            runCatching { json.decodeFromString(ApiErrorEnvelopeDto.serializer(), text) }
                .getOrNull()?.error
        }
        val lowerMessage = parsedMessage?.message?.lowercase().orEmpty()
        val lowerCode = parsedMessage?.code?.lowercase().orEmpty()

        return when (code) {
            400 -> AppError.BadRequest(parsedMessage?.message)
            401 -> if ("api_key" in lowerCode || "api key" in lowerMessage) {
                AppError.InvalidApiKey(parsedMessage?.message)
            } else {
                AppError.Unauthorized(parsedMessage?.message)
            }
            403 -> AppError.Forbidden(parsedMessage?.message)
            404 -> if ("model" in lowerMessage || "model" in lowerCode) {
                AppError.UnsupportedModel(parsedMessage?.message)
            } else {
                AppError.InvalidBaseUrl(parsedMessage?.message)
            }
            408 -> AppError.Timeout(parsedMessage?.message)
            429 -> AppError.RateLimited(parsedMessage?.message)
            in 500..599 -> AppError.ServerError(code, parsedMessage?.message)
            else -> AppError.Unknown(parsedMessage?.message ?: "HTTP $code")
        }
    }
}
