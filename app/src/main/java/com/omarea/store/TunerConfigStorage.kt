package com.omarea.store

import android.content.Context
import android.content.SharedPreferences

class TunerConfigStorage(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("tuner_config", Context.MODE_PRIVATE)

    companion object {
        const val ANIMATION_WINDOW = "animation_window"
        const val ANIMATION_TRANSITION = "animation_transition"
        const val ANIMATION_DURATION = "animation_duration"
        const val FORCE_MAX_REFRESH_RATE = "force_max_refresh_rate"
        
        const val VIBRATION_INTENSITY = "vibration_intensity"
        const val VOLUME_STEPS = "volume_steps"
        
        const val TCP_CONGESTION = "tcp_congestion"
        const val DNS_PROVIDER = "dns_provider"
        
        const val SWAPPINESS = "swappiness"
        const val VFS_CACHE_PRESSURE = "vfs_cache_pressure"

        const val SELINUX_MODE = "selinux_mode"
        const val FORCE_GPU_RENDERING = "force_gpu_rendering"
        const val DISABLE_HW_OVERLAYS = "disable_hw_overlays"
        const val IO_SCHEDULER = "io_scheduler"
        const val READ_AHEAD_KB = "read_ahead_kb"
        const val LCD_DENSITY = "lcd_density"

        const val TCP_FAST_OPEN = "tcp_fast_open"
        const val TCP_LOW_LATENCY = "tcp_low_latency"
        const val SCHED_LATENCY = "sched_latency"
        const val SCHED_MIGRATION_COST = "sched_migration_cost"
        const val USB_FAST_CHARGE = "usb_fast_charge"

        const val COLOR_SATURATION = "color_saturation"
        const val HIGH_TEXT_CONTRAST = "high_text_contrast"
        const val REFRESH_RATE_OVERLAY = "refresh_rate_overlay"
        const val APP_FREEZER = "app_freezer"
        const val USAP_POOL = "usap_pool"
        const val SHOW_TOUCHES = "show_touches"
        const val REMOTE_ADB = "remote_adb"
    }

    fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun saveInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun saveFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
}
