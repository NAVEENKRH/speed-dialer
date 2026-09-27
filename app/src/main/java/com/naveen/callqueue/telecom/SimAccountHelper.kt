package com.naveen.callqueue.telecom

import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import android.Manifest

data class SimOption(val handle: PhoneAccountHandle, val label: String)

object SimAccountHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    /** Lists the SIMs (call-capable phone accounts) available for placing a call, e.g. Jio + Vi on a dual-SIM phone. */
    fun listSims(context: Context): List<SimOption> {
        if (!hasPermission(context)) return emptyList()
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return emptyList()
        return try {
            telecomManager.callCapablePhoneAccounts.mapNotNull { handle ->
                val account = telecomManager.getPhoneAccount(handle) ?: return@mapNotNull null
                SimOption(handle, account.label?.toString() ?: "SIM")
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }
}
