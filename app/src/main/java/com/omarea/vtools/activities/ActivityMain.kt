package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.omarea.Scene
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.DialogHelper
import com.omarea.permissions.CheckRootStatus
import com.omarea.store.SpfConfig
import com.omarea.ui.TabIconHelper2
import com.omarea.utils.ElectricityUnit
import com.omarea.utils.Update
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogMonitor
import com.omarea.vtools.dialogs.DialogPower
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.omarea.vtools.fragments.FragmentCpuModes
import com.omarea.vtools.fragments.FragmentHome
import com.omarea.vtools.fragments.FragmentNav
import com.omarea.vtools.fragments.FragmentNotRoot
import com.omarea.vtools.databinding.ActivityMainBinding
import java.util.ArrayDeque

class ActivityMain : ActivityBase() {
    companion object {
        const val EXTRA_SELECT_TAB = "select_tab"
        const val TAB_NAV = 0
        const val TAB_HOME = 1
        const val TAB_TUNER = 2
        var lastSelectedTab = TAB_HOME
    }

    private lateinit var globalSPF: SharedPreferences
    private lateinit var binding: ActivityMainBinding
    private val tabHistory = ArrayDeque<Int>()
    private var suppressTabHistory = false

    private class ThermalCheckThread(private var context: Activity) : Thread() {
        private fun deleteThermalCopyWarn(onYes: Runnable) {
            Scene.post {
                if (!context.isFinishing) {
                    val view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_thermal, null)
                    val dialog = DialogHelper.customDialog(context, view)
                    view.findViewById<View>(R.id.btn_no).setOnClickListener {
                        dialog.dismiss()
                    }
                    view.findViewById<View>(R.id.btn_yes).setOnClickListener {
                        dialog.dismiss()
                        onYes.run()
                    }
                    dialog.setCancelable(false)
                }
            }
        }

        override fun run() {
            sleep(500)
            if (
                    MagiskExtend.magiskSupported() &&
                    KernelProrp.getProp("${MagiskExtend.MAGISK_PATH}system/vendor/etc/thermal.current.ini") != ""
            ) {
                when {
                    RootFile.list("/data/thermal/config").size > 0 -> {
                        deleteThermalCopyWarn {
                            KeepShellPublic.doCmdSync(
                                    "chattr -R -i /data/thermal 2> /dev/null\n" +
                                            "rm -rf /data/thermal 2> /dev/null\n" +
                                            "sync;svc power reboot || reboot;"
                            )
                        }
                    }
                    RootFile.list("/data/vendor/thermal/config").size > 0 -> {
                        if (
                                RootFile.fileEquals(
                                        "/data/vendor/thermal/config/thermal-normal.conf",
                                        MagiskExtend.getMagiskReplaceFilePath("/system/vendor/etc/thermal-normal.conf")
                                )
                        ) {
                            // Scene.toast("文件相同，跳过温控清理", Toast.LENGTH_SHORT)
                            return
                        } else {
                            deleteThermalCopyWarn {
                                KeepShellPublic.doCmdSync(
                                        "chattr -R -i /data/vendor/thermal 2> /dev/null\n" +
                                                "rm -rf /data/vendor/thermal 2> /dev/null\n" +
                                                "sync;svc power reboot || reboot;"
                                )
                            }
                        }
                    }
                    else -> return
                }
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ActivityStartSplash.finished) {
            val intent = Intent(this.applicationContext, ActivityStartSplash::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            // intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            startActivity(intent)
            finish()
            return
        }

        /*
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .detectAll()
                .build());
        */

        globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (!globalSPF.contains(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT)) {
            globalSPF.edit().putInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, ElectricityUnit().getDefaultElectricityUnit(this)).apply()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.tabBar) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
            insets
        }

        val tabIconHelper2 = TabIconHelper2(binding.tabList, binding.tabContent, this, R.layout.list_item_tab2)
        tabIconHelper2.newTabSpec(getString(R.string.app_nav), getDrawable(R.drawable.app_menu)!!, FragmentNav.createPage(themeMode))
        tabIconHelper2.newTabSpec(getString(R.string.app_home), getDrawable(R.drawable.app_home)!!, (if (CheckRootStatus.lastCheckResult) {
            FragmentHome()
        } else {
            FragmentNotRoot()
        }))
        tabIconHelper2.newTabSpec(getString(R.string.app_tuner), getDrawable(R.drawable.app_settings)!!, FragmentCpuModes())
        binding.tabContent.adapter = tabIconHelper2.adapter
        binding.tabList.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                if (suppressTabHistory) {
                    return
                }
                val position = tab?.position ?: return
                if (tabHistory.peekLast() != position) {
                    tabHistory.addLast(position)
                }
                lastSelectedTab = position
                initBottomNavigation()
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
        setInitialTab(intent?.getIntExtra(EXTRA_SELECT_TAB, TAB_HOME) ?: TAB_HOME)
        initBottomNavigation()

        if (CheckRootStatus.lastCheckResult) {
            try {
                if (MagiskExtend.magiskSupported() &&
                        !(MagiskExtend.moduleInstalled() || globalSPF.getBoolean("magisk_dot_show", false))
                ) {
                    DialogHelper.confirm(this,
                            getString(R.string.magisk_install_title),
                            getString(R.string.magisk_install_desc),
                            {
                                MagiskExtend.magiskModuleInstall(this)
                            })
                    // 不再提示 globalSPF.edit().putBoolean("magisk_dot_show", true).apply()
                }
            } catch (ex: Exception) {
                DialogHelper.alert(
                        this,
                        getString(R.string.sorry),
                        "Failed to start app\n" + ex.message
                ) {
                    recreate()
                }
            }
            ThermalCheckThread(this).start()
        }
    }

    private fun initBottomNavigation() {
        binding.bottomNavigationView.setContent {
            MiuixTheme {
                val selectedTab = lastSelectedTab
                
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                ) {
                    // Floating Container
                    Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(34.dp))
                            .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val items = listOf(
                            Triple(TAB_NAV, R.drawable.app_menu, R.string.app_nav),
                            Triple(TAB_HOME, R.drawable.app_home, R.string.app_home),
                            Triple(TAB_TUNER, R.drawable.app_settings, R.string.app_tuner)
                        )

                        items.forEach { (tabId, iconRes, labelRes) ->
                            val isSelected = selectedTab == tabId
                            val contentColor = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                            Column(
                                modifier = androidx.compose.ui.Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .combinedClickable(
                                        onClick = {
                                            binding.tabList.getTabAt(tabId)?.select()
                                            initBottomNavigation() // Refresh UI
                                        },
                                        onLongClick = null
                                    )
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = androidx.compose.ui.Modifier.size(24.dp)
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                                Text(
                                    text = getString(labelRes),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun actionGraph() {
        if (!CheckRootStatus.lastCheckResult) {
            Toast.makeText(this, getString(R.string.not_root_disabled), Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if (Settings.canDrawOverlays(this)) {
                DialogMonitor(this).show()
            } else {
                //若没有权限，提示获取
                //val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                //startActivity(intent);
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                intent.data = Uri.fromParts("package", this.packageName, null)
                Toast.makeText(applicationContext, getString(R.string.permission_float), Toast.LENGTH_LONG).show()
            }
        } else {
            DialogMonitor(this).show()
        }
    }

    override fun onResume() {
        super.onResume()

        // 如果距离上次检查更新超过 24 小时
        if (globalSPF.getLong(SpfConfig.GLOBAL_SPF_LAST_UPDATE, 0) + (3600 * 24 * 1000) < System.currentTimeMillis()) {
            Update().checkUpdate(this)
            globalSPF.edit().putLong(SpfConfig.GLOBAL_SPF_LAST_UPDATE, System.currentTimeMillis()).apply()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setInitialTab(intent?.getIntExtra(EXTRA_SELECT_TAB, TAB_HOME) ?: TAB_HOME)
    }

    private fun setInitialTab(index: Int) {
        if (!::binding.isInitialized) {
            return
        }
        val tabCount = binding.tabList.tabCount
        val tabIndex = index.coerceIn(0, (tabCount - 1).coerceAtLeast(0))
        suppressTabHistory = true
        binding.tabList.getTabAt(tabIndex)?.select()
        suppressTabHistory = false
        tabHistory.clear()
        tabHistory.addLast(tabIndex)
        lastSelectedTab = tabIndex
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    }

    //返回键事件
    override fun onBackPressed() {
        try {
            when {
                supportFragmentManager.backStackEntryCount > 0 -> {
                    supportFragmentManager.popBackStack()
                }
                tabHistory.size > 1 -> {
                    tabHistory.removeLast()
                    val previous = tabHistory.peekLast()
                    if (previous != null) {
                        suppressTabHistory = true
                        binding.tabList.getTabAt(previous)?.select()
                        suppressTabHistory = false
                        return
                    }
                    excludeFromRecent()
                    super.onBackPressed()
                }
                else -> {
                    excludeFromRecent()
                    super.onBackPressed()
                }
            }
        } catch (ex: Exception) {
            ex.stackTrace
        }
    }

    public override fun onPause() {
        super.onPause()
        if (!CheckRootStatus.lastCheckResult) {
            finish()
        }
    }

    override fun onDestroy() {
        val fragmentManager = supportFragmentManager
        fragmentManager.fragments.clear()
        super.onDestroy()
    }
}
