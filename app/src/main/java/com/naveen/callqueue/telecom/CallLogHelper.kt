package com.naveen.callqueue.telecom

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import com.naveen.callqueue.data.NumberParser
import kotlinx.coroutines.delay

object CallLogHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Finds the call log row for the call placed to [number] at or after [sinceMillis] and
     * returns its duration in seconds, or null if it couldn't be found (no permission, row not
     * written yet, query failed) — null means "unknown", never "not connected". Polls a few
     * times since some devices write the row a moment after the call state goes idle.
     * Filtering by [sinceMillis] stops an older call to the same number being mistaken for this one.
     */
    suspend fun lastCallDurationSeconds(context: Context, number: String, sinceMillis: Long): Int? {
        if (!hasPermission(context)) {
            Log.w(TAG, "READ_CALL_LOG not granted, cannot check call outcome")
            return null
        }
        val target = NumberParser.last10(number)
        if (target.length < 10) return null

        repeat(MAX_ATTEMPTS) { attempt ->
            val duration = queryOnce(context, target, sinceMillis)
            if (duration != null) {
                Log.d(TAG, "Found call log entry for ...${target.takeLast(4)}: duration=${duration}s (attempt ${attempt + 1})")
                return duration
            }
            if (attempt < MAX_ATTEMPTS - 1) delay(POLL_DELAY_MS)
        }
        Log.w(TAG, "No call log entry found for ...${target.takeLast(4)} after $MAX_ATTEMPTS attempts")
        return null
    }

    private fun queryOnce(context: Context, target: String, sinceMillis: Long): Int? {
        val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DURATION, CallLog.Calls.DATE)

        return try {
            // No "LIMIT" in the sort order: Android 11+ rejects it ("Invalid token LIMIT"), so
            // just read newest-first and stop after a handful of rows ourselves.
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                if (numberIdx < 0 || durationIdx < 0 || dateIdx < 0) return@use null

                var rowsChecked = 0
                while (rowsChecked < MAX_ROWS && cursor.moveToNext()) {
                    rowsChecked++
                    if (cursor.getLong(dateIdx) < sinceMillis - DATE_SLACK_MS) return@use null
                    val rowNumber = cursor.getString(numberIdx) ?: continue
                    if (NumberParser.last10(rowNumber) == target) {
                        return@use cursor.getInt(durationIdx)
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Call log query failed", e)
            null
        }
    }

    private const val TAG = "CallLogHelper"
    private const val MAX_ATTEMPTS = 5
    private const val POLL_DELAY_MS = 800L
    private const val MAX_ROWS = 10
    private const val DATE_SLACK_MS = 5000L
}
