package net.gevge.miniapplist.vm

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_CHANGED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.content.pm.LauncherApps
import android.net.Uri
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.gevge.miniapplist.data.AppLauncherData
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

class MainViewModelTest {
    private val tag = MainViewModelTest::class.simpleName
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val vm = MainViewModel(Dispatchers.Unconfined)
    private val launcherApps =
        appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun onPkgStateChanged_packageRemoved_successfullyAndUpdateUI() {
        vm.initAppList(launcherApps, null)
        val launchers = vm.uiAppList.value
        val targetPkg = launchers[0].launcherActivityInfo.componentName.packageName
        // One package could have multiple launcher activities
        val launcherCount =
            launchers.count { it.launcherActivityInfo.componentName.packageName == targetPkg }
        assertNotEquals(0, launcherCount)

        Log.i(tag, "targetPkg:$targetPkg, launcherCount:$launcherCount")
        val rmIntent = Intent().apply {
            action = ACTION_PACKAGE_REMOVED
            data = Uri.parse("package:$targetPkg")
        }

        vm.onPkgStateChanged(launcherApps, null, rmIntent)
        assertEquals(launchers.size - launcherCount, vm.uiAppList.value.size)
    }

    @Test
    fun onPkgStateChanged_packageRemoved_successfullyWithoutUpdateUI() {
        vm.initAppList(launcherApps, null)
        val launchers = vm.uiAppList.value
        val targetPkg = launchers[0].launcherActivityInfo.componentName.packageName
        Log.i(tag, "targetPkg:$targetPkg")
        val rmIntent = Intent().apply {
            action = ACTION_PACKAGE_REMOVED
            data = Uri.parse("package:$targetPkg")
        }

        val searchFilter = UUID.randomUUID().toString()
        // Mock user has inputted some text but filter out the target app
        vm.searchApp(searchFilter)
        // Target app is uninstalled when text is on search field
        vm.onPkgStateChanged(launcherApps, searchFilter, rmIntent)
        assertEquals(0, vm.uiAppList.value.size)
    }

    @Test
    fun onPkgStateChanged_packageChanged_successfullyAndUpdateUI() = runBlocking {
        vm.initAppList(launcherApps, null)
        val launchers = vm.uiAppList.value
        val targetPkg = launchers[0].launcherActivityInfo.componentName.packageName
        // One package could have multiple launcher activities
        val targetMap = HashMap<String, AppLauncherData>()
        for (d in launchers) {
            if (d.launcherActivityInfo.applicationInfo.packageName == targetPkg) {
                targetMap[d.launcherActivityInfo.componentName.toShortString()] = d
            }
        }
        Log.i(tag, "targetPkg:$targetPkg")
        val chgIntent = Intent().apply {
            action = ACTION_PACKAGE_CHANGED
            data = Uri.parse("package:$targetPkg")
        }

        // Wait for the time changes
        delay(500)

        vm.onPkgStateChanged(launcherApps, null, chgIntent)
        for (v in vm.uiAppList.value) {
            targetMap[v.launcherActivityInfo.componentName.toShortString()]?.let {
                // Snapshot time should be changed
                assertNotEquals(v.snapshotTime, it.snapshotTime)
            }
        }
    }

    @Test
    fun onPkgStateChanged_packageChanged_successfullyWithoutUpdateUI() = runBlocking {
        vm.initAppList(launcherApps, null)
        val launchers = vm.uiAppList.value
        val targetPkg = launchers[0].launcherActivityInfo.componentName.packageName
        // One package could have multiple launcher activities
        val targetMap = HashMap<String, AppLauncherData>()
        for (d in launchers) {
            if (d.launcherActivityInfo.applicationInfo.packageName == targetPkg) {
                targetMap[d.launcherActivityInfo.componentName.toShortString()] = d
            }
        }
        Log.i(tag, "targetPkg:$targetPkg")

        // Wait for the time changes
        delay(500)

        val chgIntent = Intent().apply {
            action = ACTION_PACKAGE_CHANGED
            data = Uri.parse("package:$targetPkg")
        }

        val searchFilter = UUID.randomUUID().toString()
        // Mock user has inputted some text but filter out the target app
        vm.searchApp(searchFilter)
        // Target app is changed when text is on search field
        vm.onPkgStateChanged(launcherApps, searchFilter, chgIntent)
        assertEquals(0, vm.uiAppList.value.size)
    }
}