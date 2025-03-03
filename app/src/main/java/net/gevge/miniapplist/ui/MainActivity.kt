package net.gevge.miniapplist.ui

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_CHANGED
import android.content.Intent.ACTION_PACKAGE_FULLY_REMOVED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.gevge.miniapplist.R
import net.gevge.miniapplist.data.AppLauncherData
import net.gevge.miniapplist.receiver.PackageStateReceiver
import net.gevge.miniapplist.vm.MainViewModel

class MainActivity : AppCompatActivity(), PackageStateReceiver.OnPackageStateChangedListener {
    private val keySearchString = "KeySearchString"
    private val vm: MainViewModel by viewModels()
    private lateinit var launcherApps: LauncherApps
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var rvAppList: RecyclerView
    private lateinit var editSearch: EditText
    private lateinit var adapter: AppListAdapter
    private lateinit var pkgStsReceiver: PackageStateReceiver
    private var searchFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        savedInstanceState?.getString(keySearchString, null)?.let {
            searchFilter = it
        }

        rvAppList = findViewById(R.id.rvAppList)
        editSearch = findViewById(R.id.editSearch)
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        layoutManager = LinearLayoutManager(this)
        adapter = AppListAdapter(
            object : AppListAdapter.ClickListener {
                override fun onClick(appLauncherData: AppLauncherData) {
                    try {
                        val intent =
                            packageManager.getLaunchIntentForPackage(
                                appLauncherData.launcherActivityInfo.applicationInfo.packageName
                            )
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.launch_app_exception, e.message.toString()),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }, object : AppListAdapter.IconLoadCallback {
                override fun onLoadIcon(appLauncherData: AppLauncherData) {
                    vm.getIcon(appLauncherData)
                }
            }
        )
        rvAppList.layoutManager = layoutManager
        rvAppList.adapter = adapter
        rvAppList.addItemDecoration(
            DividerItemDecoration(baseContext, layoutManager.orientation)
        )
        editSearch.addTextChangedListener(afterTextChanged = {
            searchFilter = it?.toString()
            vm.searchApp(searchFilter)
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                vm.uiAppList.collectLatest { list ->
                    adapter.submitList(list.sortedBy { it.name.lowercase() })
                    rvAppList.smoothScrollToPosition(0)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                vm.refresh.collectLatest {
                    val first = layoutManager.findFirstVisibleItemPosition() - 2
                    val last = layoutManager.findLastVisibleItemPosition() + 2
                    for (p in first..last) {
                        adapter.notifyItemChangedIfNoIcon(p)
                    }
                }
            }
        }

        vm.initAppList(launcherApps, searchFilter)

        pkgStsReceiver = PackageStateReceiver(this)
        val filter = IntentFilter(ACTION_PACKAGE_ADDED).apply {
            addAction(ACTION_PACKAGE_CHANGED)
            addAction(ACTION_PACKAGE_FULLY_REMOVED)
            addAction(ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            this,
            pkgStsReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pkgStsReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(keySearchString, searchFilter)
        super.onSaveInstanceState(outState)
    }

    override fun onPackageStateChanged(intent: Intent) {
        vm.onPkgStateChanged(launcherApps, searchFilter, intent)
    }
}