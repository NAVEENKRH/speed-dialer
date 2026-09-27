package com.naveen.callqueue.data

import java.util.UUID

enum class EntryStatus { PENDING, CALLING, DONE, SKIPPED }

/**
 * A single lead in the current dialing session. Nothing here is persisted to disk —
 * the queue lives only in memory for the current app session, by design.
 *
 * Immutable on purpose: both the service's StateFlow and Compose's SnapshotStateList decide
 * whether to notify observers by structural equality, so an update must always produce a new
 * QueueEntry (via [copy]) rather than mutating fields on a shared instance in place — otherwise
 * "before" and "after" compare equal and the UI silently misses the change.
 */
data class QueueEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String?,
    val rawInput: String,
    val number: String,
    val status: EntryStatus = EntryStatus.PENDING,
    val outcome: String? = null,
    val attempts: Int = 0
)

/** Quick-tag outcomes shown after a call ends, mirroring common lead-calling workflows. */
object CallOutcomes {
    val OPTIONS = listOf(
        "Interested",
        "Not Interested",
        "No Answer",
        "Busy",
        "Callback Later",
        "Wrong Number"
    )
}
