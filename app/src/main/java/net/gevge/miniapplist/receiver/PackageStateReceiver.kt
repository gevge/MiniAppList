package net.gevge.miniapplist.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_CHANGED
import android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED
import android.content.Intent.ACTION_PACKAGE_REMOVED

class PackageStateReceiver(private val stsListener: OnPackageStateChangedListener) :
    BroadcastReceiver() {
    interface OnPackageStateChangedListener {
        fun onPackageStateChanged(intent: Intent)
    }

    override fun onReceive(context: Context?, intent: Intent) {
        when (intent.action) {
            ACTION_PACKAGE_ADDED, ACTION_PACKAGE_CHANGED, ACTION_PACKAGE_FULLY_REMOVED,
            ACTION_PACKAGE_REMOVED -> {
                stsListener.onPackageStateChanged(intent)
            }

            else -> {}
        }
    }
}