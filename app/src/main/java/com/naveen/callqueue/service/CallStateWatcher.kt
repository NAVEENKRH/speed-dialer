package com.naveen.callqueue.service

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

/**
 * Wraps the two Android APIs for observing call state (RINGING/OFFHOOK/IDLE) behind one
 * listener interface — TelephonyCallback on API 31+, the older PhoneStateListener below that.
 */
class CallStateWatcher(private val context: Context, private val onStateChanged: (Int) -> Unit) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var modernCallback: TelephonyCallback? = null
    private var legacyListener: PhoneStateListener? = null

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = onStateChanged(state)
            }
            modernCallback = callback
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    onStateChanged(state)
                }
            }
            legacyListener = listener
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modernCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        } else {
            @Suppress("DEPRECATION")
            legacyListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
    }

    companion object {
        const val STATE_IDLE = TelephonyManager.CALL_STATE_IDLE
        const val STATE_RINGING = TelephonyManager.CALL_STATE_RINGING
        const val STATE_OFFHOOK = TelephonyManager.CALL_STATE_OFFHOOK
    }
}
