package com.naveen.callqueue.service

import com.naveen.callqueue.data.EntryStatus
import com.naveen.callqueue.data.QueueEntry

enum class SessionPhase {
    IDLE,           // nothing running
    DIALING,        // waiting/placing the call
    ON_CALL,        // call is active (OFFHOOK)
    AWAITING_TAG,   // call ended, waiting for an outcome tag (or auto-advance)
    HELD,           // user pressed Hold
    FINISHED        // queue exhausted
}

data class DialSessionState(
    val listName: String = "",
    val queue: List<QueueEntry> = emptyList(),
    val currentEntryId: String? = null,
    val phase: SessionPhase = SessionPhase.IDLE,
    val statusMessage: String = "",
    val countdownSeconds: Int? = null,
    val retryMax: Int = 1,
    val outcomeTaggingEnabled: Boolean = true
) {
    val current: QueueEntry? get() = queue.firstOrNull { it.id == currentEntryId }
    val doneCount: Int get() = queue.count { it.status == EntryStatus.DONE || it.status == EntryStatus.SKIPPED }
    val totalCount: Int get() = queue.size
    val waitingCount: Int get() = (totalCount - doneCount - (if (current != null) 1 else 0)).coerceAtLeast(0)

    /** Whether the current entry can be dialed again: [retryMax] is the total number of calls allowed per number. */
    val canRedialCurrent: Boolean get() = (current?.attempts ?: 0) < retryMax
}
