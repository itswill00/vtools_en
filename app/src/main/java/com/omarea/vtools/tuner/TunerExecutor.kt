package com.omarea.vtools.tuner

import android.content.Context
import com.omarea.common.shell.KeepShellPublic
import com.omarea.store.TunerConfigStorage

class TunerExecutor(private val context: Context) {
    private val storage = TunerConfigStorage(context)

    fun applyAll() {
        applyAnimationScales()
        applyRefreshRate()
        applyTcpCongestion()
        applyVmSettings()
        applyDns(storage.getString(TunerConfigStorage.DNS_PROVIDER, "default"))
        applyExtraSettings()
        applySelinux()
        applyGraphicsSettings()
        applyIoSettings()
        applyLcdDensity()
        applyAdvancedNetwork()
        applyKernelSched()
        applyUsbFastCharge()
        applyDisplayTweaks()
        applyBatteryTweaks()
        applySystemTweaks()
        applyDeveloperTweaks()
    }

    fun applyAnimationScales() {
        val window = storage.getFloat(TunerConfigStorage.ANIMATION_WINDOW, 1.0f)
        val transition = storage.getFloat(TunerConfigStorage.ANIMATION_TRANSITION, 1.0f)
        val duration = storage.getFloat(TunerConfigStorage.ANIMATION_DURATION, 1.0f)
        
        KeepShellPublic.doCmdSync(listOf(
            "settings put global window_animation_scale $window",
            "settings put global transition_animation_scale $transition",
            "settings put global animator_duration_scale $duration"
        ).joinToString("\n"))
    }

    fun applyRefreshRate() {
        val forceMax = storage.getBoolean(TunerConfigStorage.FORCE_MAX_REFRESH_RATE, false)
        if (forceMax) {
            KeepShellPublic.doCmdSync(listOf(
                "settings put system peak_refresh_rate 120.0",
                "settings put system thermal_limit_refresh_rate 120.0",
                "settings put system user_refresh_rate 120.0",
                "settings put system min_refresh_rate 120.0"
            ).joinToString("\n"))
        }
    }

    fun applyTcpCongestion() {
        val tcp = storage.getString(TunerConfigStorage.TCP_CONGESTION, "")
        if (tcp.isNotEmpty() && tcp != "default") {
            KeepShellPublic.doCmdSync("sysctl -w net.ipv4.tcp_congestion_control=$tcp")
        }
    }

    fun applyVmSettings() {
        val swappiness = storage.getInt(TunerConfigStorage.SWAPPINESS, -1)
        if (swappiness != -1) {
            KeepShellPublic.doCmdSync("sysctl -w vm.swappiness=$swappiness")
        }
        
        val vfs = storage.getInt(TunerConfigStorage.VFS_CACHE_PRESSURE, -1)
        if (vfs != -1) {
            KeepShellPublic.doCmdSync("sysctl -w vm.vfs_cache_pressure=$vfs")
        }
    }
    
    fun applyDns(dns: String) {
        if (dns.isNotEmpty() && dns != "default") {
             KeepShellPublic.doCmdSync(listOf(
                "setprop net.dns1 $dns",
                "setprop net.eth0.dns1 $dns",
                "setprop net.pdpbr1.dns1 $dns"
            ).joinToString("\n"))
        }
    }

    fun applyExtraSettings() {
        val volSteps = storage.getInt(TunerConfigStorage.VOLUME_STEPS, -1)
        if (volSteps != -1) {
            KeepShellPublic.doCmdSync("setprop ro.config.vc_call_vol_steps $volSteps")
            KeepShellPublic.doCmdSync("setprop ro.config.media_vol_steps $volSteps")
        }
        
        val vibration = storage.getInt(TunerConfigStorage.VIBRATION_INTENSITY, -1)
        if (vibration != -1) {
            KeepShellPublic.doCmdSync("echo $vibration > /sys/class/timed_output/vibrator/vtg_level")
        }
    }

    fun applySelinux() {
        val mode = storage.getString(TunerConfigStorage.SELINUX_MODE, "enforcing")
        if (mode == "permissive") {
            KeepShellPublic.doCmdSync("setenforce 0")
        } else {
            KeepShellPublic.doCmdSync("setenforce 1")
        }
    }

    fun applyGraphicsSettings() {
        val forceGpu = storage.getBoolean(TunerConfigStorage.FORCE_GPU_RENDERING, false)
        KeepShellPublic.doCmdSync("setprop persist.sys.ui.hw ${if (forceGpu) "true" else "false"}")
        
        val disableOverlays = storage.getBoolean(TunerConfigStorage.DISABLE_HW_OVERLAYS, false)
        KeepShellPublic.doCmdSync("service call SurfaceFlinger 1008 i32 ${if (disableOverlays) 1 else 0}")
    }

    fun applyIoSettings() {
        val scheduler = storage.getString(TunerConfigStorage.IO_SCHEDULER, "")
        if (scheduler.isNotEmpty() && scheduler != "default") {
            KeepShellPublic.doCmdSync("echo $scheduler > /sys/block/mmcblk0/queue/scheduler")
            KeepShellPublic.doCmdSync("echo $scheduler > /sys/block/sda/queue/scheduler")
        }
        
        val readAhead = storage.getInt(TunerConfigStorage.READ_AHEAD_KB, -1)
        if (readAhead != -1) {
            KeepShellPublic.doCmdSync("echo $readAhead > /sys/block/mmcblk0/queue/read_ahead_kb")
            KeepShellPublic.doCmdSync("echo $readAhead > /sys/block/sda/queue/read_ahead_kb")
        }
    }

    fun applyLcdDensity() {
        val density = storage.getInt(TunerConfigStorage.LCD_DENSITY, -1)
        if (density != -1) {
            KeepShellPublic.doCmdSync("wm density $density")
        }
    }

    fun applyAdvancedNetwork() {
        val tfo = storage.getBoolean(TunerConfigStorage.TCP_FAST_OPEN, false)
        KeepShellPublic.doCmdSync("sysctl -w net.ipv4.tcp_fastopen=${if (tfo) 3 else 0}")
        
        val lowLatency = storage.getBoolean(TunerConfigStorage.TCP_LOW_LATENCY, false)
        KeepShellPublic.doCmdSync("sysctl -w net.ipv4.tcp_low_latency=${if (lowLatency) 1 else 0}")
    }

    fun applyKernelSched() {
        val latency = storage.getInt(TunerConfigStorage.SCHED_LATENCY, -1)
        if (latency != -1) {
            KeepShellPublic.doCmdSync("sysctl -w kernel.sched_latency_ns=$latency")
        }
        
        val cost = storage.getInt(TunerConfigStorage.SCHED_MIGRATION_COST, -1)
        if (cost != -1) {
            KeepShellPublic.doCmdSync("sysctl -w kernel.sched_migration_cost_ns=$cost")
        }
    }

    fun applyUsbFastCharge() {
        val fastCharge = storage.getBoolean(TunerConfigStorage.USB_FAST_CHARGE, false)
        KeepShellPublic.doCmdSync("echo ${if (fastCharge) 1 else 0} > /sys/kernel/fast_charge/force_fast_charge")
    }

    fun applyDisplayTweaks() {
        val saturation = storage.getFloat(TunerConfigStorage.COLOR_SATURATION, 1.0f)
        if (saturation != 1.0f) {
            KeepShellPublic.doCmdSync("service call SurfaceFlinger 1022 f $saturation")
        }
        
        val highContrast = storage.getBoolean(TunerConfigStorage.HIGH_TEXT_CONTRAST, false)
        KeepShellPublic.doCmdSync("settings put secure high_text_contrast_enabled ${if (highContrast) 1 else 0}")
        
        val refreshRateOverlay = storage.getBoolean(TunerConfigStorage.REFRESH_RATE_OVERLAY, false)
        KeepShellPublic.doCmdSync("service call SurfaceFlinger 1034 i32 ${if (refreshRateOverlay) 1 else 0}")
    }

    fun applyBatteryTweaks() {
        val appFreezer = storage.getBoolean(TunerConfigStorage.APP_FREEZER, false)
        KeepShellPublic.doCmdSync("device_config put activity_manager_native_boot use_freezer ${if (appFreezer) "true" else "false"}")
    }

    fun applySystemTweaks() {
        val usapPool = storage.getBoolean(TunerConfigStorage.USAP_POOL, false)
        KeepShellPublic.doCmdSync("setprop persist.device_config.runtime_native.usap_pool_enabled ${if (usapPool) "true" else "false"}")
        KeepShellPublic.doCmdSync("setprop persist.sys.usap_pool_enabled ${if (usapPool) "true" else "false"}")
        
        val showTouches = storage.getBoolean(TunerConfigStorage.SHOW_TOUCHES, false)
        KeepShellPublic.doCmdSync("settings put system show_touches ${if (showTouches) 1 else 0}")
    }

    fun applyDeveloperTweaks() {
        val remoteAdb = storage.getBoolean(TunerConfigStorage.REMOTE_ADB, false)
        if (remoteAdb) {
            KeepShellPublic.doCmdSync("setprop service.adb.tcp.port 5555 && stop adbd && start adbd")
        } else {
            KeepShellPublic.doCmdSync("setprop service.adb.tcp.port -1 && stop adbd && start adbd")
        }
    }

    fun runFsTrim() {
        KeepShellPublic.doCmdSync("fstrim -v /data && fstrim -v /system && fstrim -v /cache")
    }

    fun dropCaches() {
        KeepShellPublic.doCmdSync("sync && echo 3 > /proc/sys/vm/drop_caches")
    }
}
