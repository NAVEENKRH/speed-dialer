package com.naveen.callqueue.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

object CallPlacer {

    fun hasCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Places a call directly (no dialer confirmation tap) via [TelecomManager.placeCall].
     * When [simHandle] is provided, the call goes out on that SIM silently; otherwise, on a
     * dual-SIM phone, Android will show its own "choose SIM" prompt.
     */
    fun placeCall(context: Context, number: String, simHandle: PhoneAccountHandle?): Boolean {
        if (!hasCallPermission(context)) return false
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return false
        val extras = Bundle().apply {
            if (simHandle != null) {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, simHandle)
            }
        }
        return try {
            telecomManager.placeCall(Uri.fromParts("tel", number, null), extras)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
