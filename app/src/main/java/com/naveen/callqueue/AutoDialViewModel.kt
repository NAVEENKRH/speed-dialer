package com.naveen.callqueue

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.naveen.callqueue.data.AppPreferences
import com.naveen.callqueue.data.NumberParser
import com.naveen.callqueue.data.QueueEntry
import com.naveen.callqueue.service.AutoDialService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AutoDialViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = AppPreferences(application)

    // Text currently in the paste box.
    var pasteText = mutableStateOf("")

    // Editable review list, built from the last parse, before the session starts.
    val reviewEntries = mutableStateListOf<QueueEntry>()
    var listName = mutableStateOf("List — ${todayLabel()}")

    private val _service = MutableStateFlow<AutoDialService?>(null)
    val service: StateFlow<AutoDialService?> = _service

    fun bindService(service: AutoDialService?) {
        _service.value = service
    }

    fun loadIntoReview(parsed: List<NumberParser.ParsedNumber>) {
        reviewEntries.clear()
        reviewEntries.addAll(parsed.map { QueueEntry(name = it.name, rawInput = it.rawInput, number = it.number) })
    }

    fun removeReviewEntry(id: String) {
        reviewEntries.removeAll { it.id == id }
    }

    fun renameReviewEntry(id: String, newName: String) {
        val index = reviewEntries.indexOfFirst { it.id == id }
        if (index == -1) return
        reviewEntries[index] = reviewEntries[index].copy(name = newName)
    }

    fun startSession() {
        _service.value?.startSession(listName.value, reviewEntries.toList())
    }

    private fun todayLabel(): String {
        val fmt = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
        return fmt.format(java.util.Date())
    }
}
