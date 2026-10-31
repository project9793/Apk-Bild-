package ai.mayuri.assistant.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    var isListening by mutableStateOf(false)
        private set

    var assistantName by mutableStateOf("Mayuri")
        private set

    fun toggleListening() {
        isListening = !isListening
    }

    fun updateAssistantName(name: String) {
        assistantName = name
    }
}
