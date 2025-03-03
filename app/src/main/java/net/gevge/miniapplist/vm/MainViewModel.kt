package net.gevge.miniapplist.vm

import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_CHANGED
import android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.content.pm.LauncherApps
import android.os.Process
import androidx.collection.SimpleArrayMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.gevge.miniapplist.data.AppLauncherData

class MainViewModel(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) :
    ViewModel() {
    private val appLauncherDataCache = SimpleArrayMap<String, AppLauncherData>()
    private val _uiAppList = MutableStateFlow(ArrayList<AppLauncherData>())
    val uiAppList: StateFlow<MutableList<AppLauncherData>> = _uiAppList.asStateFlow()
    private val _refresh = MutableStateFlow(-1)
    val refresh: StateFlow<Int> = _refresh.asStateFlow()
    private var refreshCount = -1

    fun getIcon(appLauncherData: AppLauncherData) = viewModelScope.launch(dispatcher) {
        val drawable = appLauncherData.launcherActivityInfo.getIcon(128)
        appLauncherData.icon = drawable
        _refresh.emit(refreshCount++)
    }

    fun initAppList(launcherApps: LauncherApps, filter: String?) =
        viewModelScope.launch(dispatcher) {
            val result = ArrayList<AppLauncherData>()
            for (activityInfo in launcherApps.getActivityList(null, Process.myUserHandle())) {
                val appLauncherData = AppLauncherData(
                    activityInfo.label.toString(),
                    System.currentTimeMillis(),
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

    fun searchApp(filter: String?) = viewModelScope.launch(dispatcher) {
        val result = ArrayList<AppLauncherData>()
        for (i in 0 until appLauncherDataCache.size()) {
            val app = appLauncherDataCache.valueAt(i)
            if (filter.isNullOrEmpty() || app.name.contains(filter, true)) {
                result.add(app)
            }
        }
        _uiAppList.emit(result)
    }

    fun onPkgStateChanged(
        launcherApps: LauncherApps,
        filter: String?,
        intent: Intent
    ) = viewModelScope.launch(dispatcher) {
        val pkgName = intent.data?.schemeSpecificPart ?: return@launch
        var refresh = false
        when (intent.action) {
            ACTION_PACKAGE_ADDED, ACTION_PACKAGE_CHANGED -> {
                for (activityInfo in launcherApps.getActivityList(
                    pkgName,
                    Process.myUserHandle()
                )) {
                    val appData = AppLauncherData(
                        activityInfo.label.toString(),
                        System.currentTimeMillis(), activityInfo, null
                    )
                    appLauncherDataCache.put(
                        appData.launcherActivityInfo.componentName.toShortString(),
                        appData
                    )
                    if (filter.isNullOrEmpty() || appData.name.contains(filter, true)) {
                        refresh = true
                    }
                }
            }

            ACTION_PACKAGE_FULLY_REMOVED, ACTION_PACKAGE_REMOVED -> {
                var i = 0
                while (i < appLauncherDataCache.size()) {
                    val launcher = appLauncherDataCache.valueAt(i)
                    val testPkg = launcher.launcherActivityInfo.componentName.packageName
                    if (pkgName != testPkg) {
                        i++
                        continue
                    }
                    appLauncherDataCache.remove(launcher.launcherActivityInfo.componentName.toShortString())
                    if (filter.isNullOrEmpty() || launcher.name.contains(filter, true)) {
                        refresh = true
                    }
                }
            }

            else -> {}
        }
        if (!refresh) {
            return@launch
        }
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