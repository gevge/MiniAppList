package net.gevge.miniapplist.vm

import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import androidx.collection.SimpleArrayMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.gevge.miniapplist.data.AppLauncherData

class MainViewModel : ViewModel() {
    private val appLauncherDataCache = SimpleArrayMap<String, AppLauncherData>()
    private val _uiAppList = MutableStateFlow(ArrayList<AppLauncherData>())
    val uiAppList: StateFlow<MutableList<AppLauncherData>> = _uiAppList.asStateFlow()
    private val _refresh = MutableStateFlow(-1)
    val refresh: StateFlow<Int> = _refresh.asStateFlow()
    private var refreshCount = -1

    fun getIcon(appLauncherData: AppLauncherData) = viewModelScope.launch(Dispatchers.Default) {
        val drawable = appLauncherData.launcherActivityInfo.getIcon(128)
        appLauncherData.icon = drawable
        _refresh.emit(refreshCount++)
    }

    fun initAppList(launcherApps: LauncherApps, pm: PackageManager, filter: String?) =
        viewModelScope.launch(Dispatchers.Default) {
            val result = ArrayList<AppLauncherData>()
            for (activityInfo in launcherApps.getActivityList(null, Process.myUserHandle())) {
                val lastUpdatedTime =
                    try {
                        pm.getPackageInfo(
                            activityInfo.applicationInfo.packageName,
                            0
                        ).lastUpdateTime
                    } catch (e: Exception) {
                        -1L
                    }
                val appLauncherData = AppLauncherData(
                    activityInfo.label.toString(),
                    lastUpdatedTime,
                    activityInfo,
                    null
                )
                appLauncherDataCache.put(
                    activityInfo.componentName.toShortString(),
                    appLauncherData
                )
                if (filter.isNullOrEmpty() || appLauncherData.name.contains(filter, true)) {
                    result.add(appLauncherData)
                }
            }
            _uiAppList.emit(result)
        }

    fun searchApp(filter: String?) = viewModelScope.launch(Dispatchers.Default) {
        val result = ArrayList<AppLauncherData>()
        for (i in 0 until appLauncherDataCache.size()) {
            val app = appLauncherDataCache.valueAt(i)
            if (filter.isNullOrEmpty() || app.name.contains(filter, true)) {
                result.add(app)
            }
        }
        _uiAppList.emit(result)
    }
}