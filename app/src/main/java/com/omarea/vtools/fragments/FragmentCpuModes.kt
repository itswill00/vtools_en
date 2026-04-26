package com.omarea.vtools.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.omarea.common.shell.KeepShellPublic
import com.omarea.store.TunerConfigStorage
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityBase
import com.omarea.vtools.databinding.FragmentCpuModesContentBinding
import com.omarea.vtools.tuner.TunerExecutor
import com.omarea.vtools.ui.common.MiuixSectionHeader
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.math.BigDecimal
import java.math.RoundingMode

class FragmentCpuModes : Fragment() {
    private var _binding: FragmentCpuModesContentBinding? = null
    private val binding get() = _binding!!

    private lateinit var storage: TunerConfigStorage
    private lateinit var executor: TunerExecutor

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCpuModesContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        storage = TunerConfigStorage(requireContext())
        executor = TunerExecutor(requireContext())

        binding.tunerComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.tunerComposeView.setContent {
            val themeMode = (activity as? ActivityBase)?.themeMode
            val controller = ThemeController(
                if (themeMode?.isDarkMode == true) {
                    ColorSchemeMode.Dark
                } else {
                    ColorSchemeMode.Light
                }
            )
            MiuixTheme(controller = controller) {
                TunerScreenAtomic()
            }
        }
    }

    @Composable
    fun TunerScreenAtomic() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TunerOverviewDashboard()

            // Group: Visual
            TunerCategorySection("Visual & display") {
                TunerSliderCard("Window animation", TunerConfigStorage.ANIMATION_WINDOW, 0f, 2f, 0.25f, icon = R.drawable.ic_menu_img) { executor.applyAnimationScales() }
                TunerSliderCard("Transition anim", TunerConfigStorage.ANIMATION_TRANSITION, 0f, 2f, 0.25f, icon = R.drawable.ic_menu_img) { executor.applyAnimationScales() }
                TunerSliderCard("Color saturation", TunerConfigStorage.COLOR_SATURATION, 0f, 2f, 0.1f, icon = R.drawable.ic_menu_img) { executor.applyDisplayTweaks() }
                TunerGridRow {
                    TunerAtomicSwitch("Force 120Hz", TunerConfigStorage.FORCE_MAX_REFRESH_RATE, R.drawable.ic_menu_hot) { executor.applyRefreshRate() }
                    TunerAtomicSwitch("Hz overlay", TunerConfigStorage.REFRESH_RATE_OVERLAY, R.drawable.ic_menu_img) { executor.applyDisplayTweaks() }
                }
                TunerGridRow {
                    TunerAtomicSwitch("Text contrast", TunerConfigStorage.HIGH_TEXT_CONTRAST, R.drawable.ic_menu_img) { executor.applyDisplayTweaks() }
                    TunerAtomicSwitch("Show touches", TunerConfigStorage.SHOW_TOUCHES, R.drawable.ic_menu_hot) { executor.applySystemTweaks() }
                }
            }

            // Group: System
            TunerCategorySection("System workbench") {
                TunerDropdownCard("SELinux mode", TunerConfigStorage.SELINUX_MODE, listOf("enforcing", "permissive"), icon = R.drawable.tab_security) { executor.applySelinux() }
                TunerGridRow {
                    TunerAtomicSwitch("GPU force", TunerConfigStorage.FORCE_GPU_RENDERING, R.drawable.ic_menu_hot) { executor.applyGraphicsSettings() }
                    TunerAtomicSwitch("Disable HW", TunerConfigStorage.DISABLE_HW_OVERLAYS, R.drawable.ic_menu_cpu) { executor.applyGraphicsSettings() }
                }
                TunerGridRow {
                    TunerAtomicSwitch("USAP pool", TunerConfigStorage.USAP_POOL, R.drawable.ic_menu_cpu) { executor.applySystemTweaks() }
                    TunerAtomicSwitch("Remote ADB", TunerConfigStorage.REMOTE_ADB, R.drawable.ic_menu_shell) { executor.applyDeveloperTweaks() }
                }
                TunerSliderCard("Screen density", TunerConfigStorage.LCD_DENSITY, 320f, 640f, 10f, isInt = true, icon = R.drawable.ic_menu_digital) { executor.applyLcdDensity() }
            }

            // Group: Memory
            TunerCategorySection("Memory & storage") {
                TunerGridRow {
                    TunerAtomicAction("Trim storage", R.drawable.app_options_dex2oat) { executor.runFsTrim() }
                    TunerAtomicAction("Clean cache", R.drawable.app_options_clear) { executor.dropCaches() }
                }
                TunerSliderCard("Swap agres.", TunerConfigStorage.SWAPPINESS, 0f, 100f, 1f, isInt = true, icon = R.drawable.ic_menu_swap) { executor.applyVmSettings() }
                TunerDropdownCard("I/O scheduler", TunerConfigStorage.IO_SCHEDULER, listOf("default", "cfq", "deadline", "noop", "mq-deadline"), icon = R.drawable.ic_menu_modules) { executor.applyIoSettings() }
                TunerGridRow {
                    TunerAtomicSwitch("App freezer", TunerConfigStorage.APP_FREEZER, R.drawable.ic_menu_freeze) { executor.applyBatteryTweaks() }
                    TunerAtomicSwitch("Fast charge", TunerConfigStorage.USB_FAST_CHARGE, R.drawable.charge) { executor.applyUsbFastCharge() }
                }
            }

            // Group: Network
            TunerCategorySection("Network workbench") {
                TunerGridRow {
                    TunerAtomicSwitch("TCP fast open", TunerConfigStorage.TCP_FAST_OPEN, R.drawable.ic_menu_shell) { executor.applyAdvancedNetwork() }
                    TunerAtomicSwitch("Low latency", TunerConfigStorage.TCP_LOW_LATENCY, R.drawable.ic_menu_shell) { executor.applyAdvancedNetwork() }
                }
                TunerDropdownCard("Congestion control", TunerConfigStorage.TCP_CONGESTION, listOf("default", "cubic", "bbr", "reno"), icon = R.drawable.icon_global) { executor.applyTcpCongestion() }
                TunerDropdownCard("DNS provider", TunerConfigStorage.DNS_PROVIDER, listOf("default", "1.1.1.1", "8.8.8.8", "9.9.9.9"), icon = R.drawable.icon_global) { executor.applyDns(it) }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    @Composable
    fun TunerCategorySection(title: String, content: @Composable ColumnScope.() -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MiuixSectionHeader(title)
            content()
        }
    }

    @Composable
    fun RowScope.TunerAtomicSwitch(label: String, key: String, icon: Int, onChange: () -> Unit) {
        var checked by remember { mutableStateOf(storage.getBoolean(key, false)) }
        Card(
            modifier = Modifier.weight(1f).height(66.dp).clickable {
                checked = !checked
                storage.saveBoolean(key, checked)
                onChange()
            },
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(),
            insideMargin = PaddingValues(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = if (checked) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = label, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
                androidx.compose.material3.Switch(
                    checked = checked,
                    onCheckedChange = { checked = it; storage.saveBoolean(key, it); onChange() },
                    modifier = Modifier.scale(0.65f),
                    colors = SwitchDefaults.colors(checkedThumbColor = MiuixTheme.colorScheme.primary)
                )
            }
        }
    }

    @Composable
    fun RowScope.TunerAtomicAction(label: String, icon: Int, onClick: () -> Unit) {
        Card(
            modifier = Modifier.weight(1f).height(66.dp).clickable { onClick() },
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(),
            insideMargin = PaddingValues(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }

    @Composable
    fun TunerSliderCard(label: String, key: String, min: Float, max: Float, step: Float, isInt: Boolean = false, icon: Int, onChange: () -> Unit) {
        var value by remember { mutableFloatStateOf(if (isInt) storage.getInt(key, min.toInt()).toFloat() else storage.getFloat(key, 1.0f)) }
        Card(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(),
            insideMargin = PaddingValues(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = label, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.width(95.dp), maxLines = 1)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = {
                        if (isInt) storage.saveInt(key, value.toInt()) else storage.saveFloat(key, value)
                        onChange()
                    },
                    valueRange = min..max,
                    steps = if (step > 0) ((max - min) / step).toInt() - 1 else 0,
                    modifier = Modifier.weight(1f).scale(0.9f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isInt) value.toInt().toString() else formatFloat(value), style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }

    @Composable
    fun TunerDropdownCard(label: String, key: String, options: List<String>, icon: Int, onChange: (String) -> Unit = {}) {
        var selected by remember { mutableStateOf(storage.getString(key, options.getOrNull(0) ?: "default")) }
        var expanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth().height(56.dp).clickable { expanded = true },
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(),
            insideMargin = PaddingValues(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = label, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(text = selected, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Icon(painter = painterResource(R.drawable.icon_more), contentDescription = null, modifier = Modifier.size(12.dp).alpha(0.4f).padding(start = 4.dp))
                androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MiuixTheme.colorScheme.surface)) {
                    options.forEach { option ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(option, color = MiuixTheme.colorScheme.onSurface) }, onClick = { selected = option; storage.saveString(key, option); onChange(option); expanded = false })
                    }
                }
            }
        }
    }

    @Composable
    fun TunerOverviewDashboard() {
        val selinuxStatus = remember { mutableStateOf("...") }
        val tcpStatus = remember { mutableStateOf("...") }
        LaunchedEffect(Unit) {
            selinuxStatus.value = KeepShellPublic.doCmdSync("getenforce")
            tcpStatus.value = KeepShellPublic.doCmdSync("sysctl -n net.ipv4.tcp_congestion_control")
        }
        Card(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, colors = CardDefaults.defaultColors(), insideMargin = PaddingValues(0.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                DashboardMetric("SELinux", selinuxStatus.value, if (selinuxStatus.value == "Enforcing") MiuixTheme.colorScheme.primary else Color.Red)
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
                DashboardMetric("TCP", tcpStatus.value, MiuixTheme.colorScheme.primary)
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
                DashboardMetric("Root", "Active", MiuixTheme.colorScheme.primary)
            }
        }
    }

    @Composable
    fun TunerGridRow(content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }

    @Composable
    fun DashboardMetric(label: String, value: String, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
            Text(text = value, style = MiuixTheme.textStyles.body2, color = color, fontWeight = FontWeight.Bold)
        }
    }

    private fun formatFloat(value: Float): String {
        return try { BigDecimal(value.toDouble()).setScale(2, RoundingMode.HALF_UP).toString() } catch (e: Exception) { value.toString() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
