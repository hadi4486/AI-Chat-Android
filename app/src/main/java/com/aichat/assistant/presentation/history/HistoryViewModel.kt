package com.aichat.assistant.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.assistant.domain.model.Conversation
import com.aichat.assistant.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val query: String = "",
    val conversations: List<Conversation> = emptyList()
)

class HistoryViewModel(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val debouncedResults = query
        .debounce(150)
        .distinctUntilChanged()
        .flatMapLatest { q -> conversationRepository.observeConversations(q) }

    val uiState: StateFlow<HistoryUiState> = combine(query, debouncedResults) { q, conversations ->
        HistoryUiState(query = q, conversations = conversations)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun togglePin(conversationId: String) {
        viewModelScope.launch { conversationRepository.togglePin(conversationId) }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch { conversationRepository.deleteConversation(conversationId) }
    }
}
