package com.omarea.vtools.fragments

import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.omarea.common.shell.KeepShellPublic
import com.omarea.permissions.CheckRootStatus
import com.omarea.vtools.R
import com.omarea.vtools.databinding.FragmentSpecsBinding
import com.omarea.vtools.ui.common.MiuixSectionHeader
import com.omarea.vtools.ui.common.MiuixPropertyRow
import com.omarea.vtools.ui.common.MiuixSectionCard
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.io.File
import java.util.UUID

class FragmentSpecs : Fragment() {
    private var _binding: FragmentSpecsBinding? = null
    private val binding get() = _binding!!

    data class SpecsUiState(
        val manufacturer: String = Build.MANUFACTURER,
        val model: String = Build.MODEL,
        val codename: String = Build.DEVICE,
        val board: String = Build.BOARD,
        val hardware: String = Build.HARDWARE,
        val androidVer: String = Build.VERSION.RELEASE,
        val sdkLevel: Int = Build.VERSION.SDK_INT,
        val securityPatch: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown",
        val kernelVer: String = System.getProperty("os.version") ?: "Unknown",
        val arch: String = System.getProperty("os.arch") ?: "Unknown",
        val compiler: String = "Unknown",
        val resolution: String = "",
        val dpi: String = "",
        val refreshRate: String = "",
        val abi: String = Build.SUPPORTED_ABIS.joinToString(", "),
        val rootStatus: Boolean = CheckRootStatus.lastCheckResult,
        val selinux: String = "Unknown",
        val uptime: String = "",
        val storageInfo: String = "",
        val widevine: String = "Unknown"
    )

    private val uiState = mutableStateOf(SpecsUiState())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpecsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        updateSystemInfo()
        
        binding.specsComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.specsComposeView.setContent {
            val themeMode = (activity as? com.omarea.vtools.activities.ActivityBase)?.themeMode
            val controller = remember {
                ThemeController(if (themeMode?.isDarkMode == true) ColorSchemeMode.Dark else ColorSchemeMode.Light)
            }
            
            MiuixTheme(controller = controller) {
                Surface(modifier = Modifier.fillMaxSize(), color = MiuixTheme.colorScheme.background) {
                    SpecsScreen(uiState.value)
                }
            }
        }
    }

    private fun updateSystemInfo() {
        val metrics = getDisplayMetrics()
        uiState.value = uiState.value.copy(
            resolution = "${metrics.widthPixels} x ${metrics.heightPixels}",
            dpi = "${metrics.densityDpi} DPI",
            refreshRate = "${getRefreshRate()} Hz",
            compiler = getKernelCompiler(),
            selinux = getSELinuxStatus(),
            uptime = getUptime(),
            storageInfo = getStorageInfo(),
            widevine = getWidevineLevel()
        )
    }

    private fun getSELinuxStatus(): String {
        return try {
            val status = KeepShellPublic.doCmdSync("getenforce")
            if (status.isNullOrBlank()) "Unknown" else status.trim()
        } catch (e: Exception) { "Unknown" }
    }

    private fun getUptime(): String {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val days = uptimeMillis / (24 * 3600 * 1000)
        val hours = (uptimeMillis % (24 * 3600 * 1000)) / (3600 * 1000)
        val minutes = (uptimeMillis % (3600 * 1000)) / (60 * 1000)
        return if (days > 0) "${days}d ${hours}h ${minutes}m" else "${hours}h ${minutes}m"
    }

    private fun getStorageInfo(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024 * 1024.0)
            val free = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024.0)
            "%.1f GB / %.1f GB free".format(free, total)
        } catch (e: Exception) { "Unknown" }
    }

    private fun getWidevineLevel(): String {
        return try {
            val mediaDrm = MediaDrm(UUID(-0x12107456885b06d3L, -0x39839b2323ef513L))
            val level = mediaDrm.getPropertyString("securityLevel")
            mediaDrm.release()
            level ?: "Unknown"
        } catch (e: Exception) { "N/A" }
    }

    @Composable
    fun SpecsScreen(state: SpecsUiState) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile("Root Status", if (state.rootStatus) "Granted" else "None", R.drawable.ic_menu_magisk, Modifier.weight(1f))
                    SummaryTile("SELinux", state.selinux, R.drawable.linux, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile("Android", state.androidVer, R.drawable.icon_android, Modifier.weight(1f))
                    SummaryTile("Uptime", state.uptime, R.drawable.ic_settings, Modifier.weight(1f))
                }
            }

            item {
                Column {
                    MiuixSectionHeader(title = "Device Identity")
                    MiuixSectionCard {
                        MiuixPropertyRow("Manufacturer", state.manufacturer, R.drawable.app_home)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        MiuixPropertyRow("Model", state.model, R.drawable.app_home)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        MiuixPropertyRow("Codename", state.codename, R.drawable.app_home)
                    }
                }
            }

            item {
                Column {
                    MiuixSectionHeader(title = "Security & Storage")
                    MiuixSectionCard {
                        MiuixPropertyRow("Widevine DRM", state.widevine, R.drawable.ic_menu_freeze)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        MiuixPropertyRow("Data Storage", state.storageInfo, R.drawable.ic_menu_digital)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        MiuixPropertyRow("Security Patch", state.securityPatch, R.drawable.ic_menu_freeze)
                    }
                }
            }

            item {
                Column {
                    MiuixSectionHeader(title = "Platform Hardware")
                    MiuixSectionCard {
                        MiuixPropertyRow("Processor", state.hardware, R.drawable.ic_menu_cpu)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        MiuixPropertyRow("Instruction Sets", state.abi, R.drawable.ic_menu_cpu)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    @Composable
    fun SummaryTile(label: String, value: String, iconRes: Int, modifier: Modifier = Modifier) {
        Card(
            modifier = modifier.height(72.dp),
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(),
            insideMargin = PaddingValues(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(iconRes), contentDescription = null, tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = label, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = value, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.onSurface, maxLines = 1)
            }
        }
    }

    private fun getKernelCompiler(): String {
        return try {
            val content = File("/proc/version").readText()
            when {
                content.contains("clang version") -> content.substringAfter("clang version ").substringBefore(" ")
                content.contains("gcc version") -> content.substringAfter("gcc version ").substringBefore(" ")
                else -> "Unknown"
            }
        } catch (e: Exception) { "Unknown" }
    }

    private fun getDisplayMetrics(): DisplayMetrics = DisplayMetrics().apply {
        (context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.getRealMetrics(this)
    }

    private fun getRefreshRate(): Int = (context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.refreshRate?.toInt() ?: 60

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
