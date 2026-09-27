package com.naveen.callqueue.data

import android.content.ComponentName
import android.content.Context
import android.telecom.PhoneAccountHandle
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "callqueue_settings")

/**
 * On-device app settings only: default calling SIM, retry behaviour. This is deliberately
 * separate from call lists/history, which the app does not persist at all.
 */
class AppPreferences(private val context: Context) {

    private object Keys {
        val SIM_COMPONENT = stringPreferencesKey("sim_component")
        val SIM_ID = stringPreferencesKey("sim_id")
        val SIM_LABEL = stringPreferencesKey("sim_label")
        val RETRY_MAX = intPreferencesKey("retry_max")
        val OUTCOME_TAGGING_ENABLED = booleanPreferencesKey("outcome_tagging_enabled")
        val GAP_SECONDS = intPreferencesKey("gap_seconds")
    }

    /** Total calls allowed per number if it doesn't pick up: 1 (Once), 2 (Twice) or 3 (Thrice). */
    val retryMax: Flow<Int> = context.dataStore.data.map { it[Keys.RETRY_MAX] ?: 2 }
    val outcomeTaggingEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.OUTCOME_TAGGING_ENABLED] ?: true }
    val gapSeconds: Flow<Int> = context.dataStore.data.map { it[Keys.GAP_SECONDS] ?: 4 }
    val simLabel: Flow<String?> = context.dataStore.data.map { it[Keys.SIM_LABEL] }
    val simId: Flow<String?> = context.dataStore.data.map { it[Keys.SIM_ID] }

    suspend fun setRetryMax(max: Int) {
        context.dataStore.edit { it[Keys.RETRY_MAX] = max }
    }

    suspend fun setOutcomeTagging(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OUTCOME_TAGGING_ENABLED] = enabled }
    }

    suspend fun setGapSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.GAP_SECONDS] = seconds }
    }

    suspend fun setSimAccount(handle: PhoneAccountHandle, label: String) {
        context.dataStore.edit {
            it[Keys.SIM_COMPONENT] = handle.componentName.flattenToString()
            it[Keys.SIM_ID] = handle.id
            it[Keys.SIM_LABEL] = label
        }
    }

    /** Reconstructs the saved [PhoneAccountHandle], or null if none was chosen yet. */
    suspend fun getSimAccountHandle(): PhoneAccountHandle? {
        val prefs = context.dataStore.data.first()
        val component = prefs[Keys.SIM_COMPONENT]
        val id = prefs[Keys.SIM_ID]
        return if (component != null && id != null) {
            PhoneAccountHandle(ComponentName.unflattenFromString(component)!!, id)
        } else null
    }
}
