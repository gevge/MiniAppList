package net.gevge.miniapplist.data

import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable

data class AppLauncherData(
    var name: String,
    var snapshotTime: Long,
    var launcherActivityInfo: LauncherActivityInfo,
    var icon: Drawable?,
    var isRefreshPending: Boolean = false
)
