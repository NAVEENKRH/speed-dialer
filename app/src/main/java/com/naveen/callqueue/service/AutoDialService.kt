package com.naveen.callqueue.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.naveen.callqueue.MainActivity
import com.naveen.callqueue.data.AppPreferences
import com.naveen.callqueue.data.EntryStatus
import com.naveen.callqueue.data.QueueEntry
import com.naveen.callqueue.telecom.CallLogHelper
import com.naveen.callqueue.telecom.CallPlacer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Runs the whole auto-dial session as a foreground service so it survives the phone app
 * taking over the screen during a call. Owns the queue, watches call state to know when a
 * call has ended, and hands control back to the user for what happens next: Android has no
 * reliable, permission-available way for a third-party app to know whether the other side
 * actually answered (that needs a privileged permission apps can't hold), so rather than guess,
 * every call end offers a "Call again" window the user can act on or let expire.
 */
class AutoDialService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): AutoDialService = this@AutoDialService
    }

    private val binder = LocalBinder()

    // SupervisorJob + this handler mean an unexpected exception anywhere in the dial loop
    // surfaces as a paused session the user can resume from, instead of crashing the whole app.
    private val crashHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("AutoDialService", "Dial session error", throwable)
        _state.value = _state.value.copy(
            phase = SessionPhase.HELD,
            statusMessage = "Hit an error (${throwable.javaClass.simpleName}) — tap Resume to continue"
        )
    }
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + crashHandler)
    private lateinit var prefs: AppPreferences
    private lateinit var callStateWatcher: CallStateWatcher

    private var lastCallState: Int = CallStateWatcher.STATE_IDLE
    private var retryMax = 1
    private var outcomeTaggingEnabled = true
    private var gapSeconds = 4

    // The countdown after a call ends, offering "Call again" before auto-advancing.
    // Kept cancellable so a manual redial/tag/skip/hold can pre-empt it.
    private var postCallJob: Job? = null
    private var lastDialStartedAtMillis = 0L

    private val _state = MutableStateFlow(DialSessionState())
    val state: StateFlow<DialSessionState> = _state

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(applicationContext)
        callStateWatcher = CallStateWatcher(applicationContext) { newState -> onCallStateChanged(newState) }
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        callStateWatcher.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ---- Public control API, called from the ViewModel ----

    fun startSession(listName: String, entries: List<QueueEntry>) {
        startForeground(NOTIF_ID, buildNotification("Starting…"))
        serviceScope.launch {
            retryMax = prefs.retryMax.first()
            outcomeTaggingEnabled = prefs.outcomeTaggingEnabled.first()
            gapSeconds = prefs.gapSeconds.first()

            callStateWatcher.start()
            _state.value = DialSessionState(
                listName = listName,
                queue = entries,
                phase = SessionPhase.DIALING,
                retryMax = retryMax,
                outcomeTaggingEnabled = outcomeTaggingEnabled
            )
            runNext()
        }
    }

    fun hold() {
        postCallJob?.cancel()
        _state.value = _state.value.copy(phase = SessionPhase.HELD, statusMessage = "Held — tap Resume to continue")
        updateNotification("Held")
    }

    fun resumeSession() {
        if (_state.value.phase != SessionPhase.HELD) return
        serviceScope.launch { runNext() }
    }

    fun skip() {
        postCallJob?.cancel()
        val current = _state.value.current ?: return
        replaceEntry(current.id) { it.copy(status = EntryStatus.SKIPPED) }
        serviceScope.launch { runNext() }
    }

    /** Sends the current lead to the back of the queue and moves on, without marking it done. */
    fun later() {
        postCallJob?.cancel()
        val current = _state.value.current ?: return
        val queue = _state.value.queue.toMutableList()
        val index = queue.indexOfFirst { it.id == current.id }
        if (index == -1) return
        queue.removeAt(index)
        queue.add(current.copy(status = EntryStatus.PENDING))
        _state.value = _state.value.copy(queue = queue, currentEntryId = null)
        serviceScope.launch { runNext() }
    }

    fun submitOutcome(outcome: String) {
        postCallJob?.cancel()
        val current = _state.value.current ?: return
        replaceEntry(current.id) { it.copy(status = EntryStatus.DONE, outcome = outcome) }
        serviceScope.launch { runNext() }
    }

    /** User tapped "Call again" during the post-call window: redial the same number right away. */
    fun redialNow() {
        postCallJob?.cancel()
        val current = _state.value.current ?: return
        if (current.attempts >= retryMax) return
        replaceEntry(current.id) { it.copy(status = EntryStatus.PENDING) }
        serviceScope.launch { runNext() }
    }

    fun stopSession() {
        postCallJob?.cancel()
        callStateWatcher.stop()
        _state.value = DialSessionState()
        // Drop the foreground/notification but keep the service alive and bound, ready for
        // the next session — MainActivity tears it down properly in onDestroy.
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // ---- Core dialing loop ----

    private suspend fun runNext() {
        val next = _state.value.queue.firstOrNull { it.status == EntryStatus.PENDING }
        if (next == null) {
            _state.value = _state.value.copy(
                currentEntryId = null,
                phase = SessionPhase.FINISHED,
                statusMessage = "All done — queue is empty",
                countdownSeconds = null
            )
            updateNotification("Session finished")
            return
        }

        val attempts = next.attempts + 1
        replaceEntry(next.id) { it.copy(status = EntryStatus.CALLING, attempts = attempts) }
        _state.value = _state.value.copy(
            currentEntryId = next.id,
            phase = SessionPhase.DIALING,
            statusMessage = "Calling ${next.name ?: next.number}…",
            countdownSeconds = null
        )
        updateNotification("Calling ${next.name ?: next.number}")

        val simHandle = prefs.getSimAccountHandle()
        lastDialStartedAtMillis = System.currentTimeMillis()
        val placed = CallPlacer.placeCall(applicationContext, next.number, simHandle)
        if (!placed) {
            _state.value = _state.value.copy(
                phase = SessionPhase.HELD,
                statusMessage = "Couldn't place the call — check Phone permission, then Resume"
            )
        }
    }

    private fun onCallStateChanged(newState: Int) {
        val previous = lastCallState
        lastCallState = newState

        when (newState) {
            CallStateWatcher.STATE_OFFHOOK -> {
                if (previous != CallStateWatcher.STATE_OFFHOOK) {
                    _state.value = _state.value.copy(phase = SessionPhase.ON_CALL, statusMessage = "On call…")
                    updateNotification("On call")
                }
            }
            CallStateWatcher.STATE_IDLE -> {
                if (previous == CallStateWatcher.STATE_OFFHOOK) {
                    postCallJob = serviceScope.launch { handleCallEnded() }
                }
            }
        }
    }

    /**
     * Opens the post-call window. Checks the call log to see whether this call actually
     * connected (a "Call again" button is always offered manually regardless, since that lookup
     * can occasionally miss) — if it looks like a genuine no-answer and a retry attempt is still
     * available, the countdown auto-redials on timeout instead of just moving on, which is the
     * automatic "double dialing" behaviour. Tapping Call again or an outcome tag at any point
     * overrides this immediately.
     */
    private suspend fun handleCallEnded() {
        val current = _state.value.current ?: return

        _state.value = _state.value.copy(
            phase = SessionPhase.AWAITING_TAG,
            statusMessage = "Checking call result…",
            countdownSeconds = null
        )

        val durationSeconds = CallLogHelper.lastCallDurationSeconds(
            applicationContext, current.number, lastDialStartedAtMillis
        )
        val notConnected = durationSeconds != null && durationSeconds <= 0
        val canRedial = _state.value.canRedialCurrent
        val willAutoRedial = notConnected && canRedial

        // The entry/phase may have changed while we were awaiting the call-log lookup
        // (user already tapped something) — bail out rather than clobber their action.
        if (_state.value.phase != SessionPhase.AWAITING_TAG || _state.value.currentEntryId != current.id) return

        _state.value = _state.value.copy(
            statusMessage = when {
                willAutoRedial -> "No answer — calling again automatically"
                durationSeconds == null -> "Couldn't read the call result — moving on unless you tap Call again"
                durationSeconds > 0 -> "Connected (${durationSeconds}s) — tag it, or it moves on automatically"
                else -> "No answer, but no redials left — moving on shortly"
            },
            countdownSeconds = gapSeconds
        )

        for (s in gapSeconds downTo 1) {
            _state.value = _state.value.copy(countdownSeconds = s)
            delay(1000)
        }

        // Timed out with no manual action taken.
        if (_state.value.phase == SessionPhase.AWAITING_TAG && _state.value.currentEntryId == current.id) {
            if (willAutoRedial) {
                replaceEntry(current.id) { it.copy(status = EntryStatus.PENDING) }
            } else {
                replaceEntry(current.id) { it.copy(status = EntryStatus.DONE) }
            }
            runNext()
        }
    }

    /** Replaces the entry with [id] in the queue via [transform], producing a genuinely new list/entry so StateFlow emits. */
    private fun replaceEntry(id: String, transform: (QueueEntry) -> QueueEntry) {
        val newQueue = _state.value.queue.map { if (it.id == id) transform(it) else it }
        _state.value = _state.value.copy(queue = newQueue)
    }

    // ---- Notification plumbing (required for a phoneCall-type foreground service) ----

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Auto-dial session", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Speed Dialer")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text))
    }

    companion object {
        private const val CHANNEL_ID = "auto_dial_session"
        private const val NOTIF_ID = 1001
    }
}
