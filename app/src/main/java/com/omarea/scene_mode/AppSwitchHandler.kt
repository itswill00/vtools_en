package com.omarea.scene_mode

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.Scene
import com.omarea.common.shell.KeepShellPublic
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.data.IEventReceiver
import com.omarea.library.basic.InputMethodApp
import com.omarea.library.basic.ScreenState
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.utils.CommonCmds
import com.omarea.vtools.AccessibilityScenceMode
import com.omarea.vtools.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

/**
 * AppSwitchHandler - Cleaned up version
 */
@OptIn(DelicateCoroutinesApi::class)
class AppSwitchHandler(private var context: AccessibilityScenceMode, override val isAsync: Boolean = false) : ModeSwitcher(), IEventReceiver {
    private var lastPackage: String? = null
    private var lastModePackage: String? = "com.system.ui"
    private var lastMode = ""
    private var spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var sceneBlackList = context.getSharedPreferences(SpfConfig.SCENE_BLACK_LIST, Context.MODE_PRIVATE)
    private val spfGlobal: SharedPreferences
        get() = Scene.globalConfig
    
    private var ignoredList = ArrayList<String>()
    private var firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
    private var screenOn = false
    private var lastScreenOnOff: Long = 0

    private val SCREEN_OFF_SWITCH_NETWORK_DELAY: Long = 25000
    private var handler = Handler(Looper.getMainLooper())
    private val sceneMode = SceneMode.getNewInstance(context, SceneConfigStore(context))!!
    private var timer: Timer? = null
    private var screenState = ScreenState(context)

    private fun updateConfig() {
        clearInitedState()
        lastMode = ""
        firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)

        initConfig()
        stopTimer()
        startTimer()
    }

    private fun startTimer() {
        if (timer == null && screenOn && screenState.isScreenOn()) {
            timer = Timer(true).apply {
                val interval = 6
                scheduleAtFixedRate(object : TimerTask() {
                    private var ticks = 0
                    override fun run() {
                        ticks += interval
                        ticks %= 60
                        if (ticks == 0) {
                            sceneMode.clearFreezeAppTimeLimit()
                        }
                    }
                }, 0, interval * 1000L)
            }
        }
    }

    private fun stopTimer() {
        try {
            if (timer != null) {
                timer!!.cancel()
                timer!!.purge()
                timer = null
            }
        } catch (ex: Exception) {}
    }

    private fun onScreenOff() {
        if (!screenOn) return

        screenOn = false
        lastScreenOnOff = System.currentTimeMillis()
        sceneMode.onScreenOff()

        handler.postDelayed({
            if (!screenOn) {
                if (System.currentTimeMillis() - lastScreenOnOff >= SCREEN_OFF_SWITCH_NETWORK_DELAY) {
                    sceneMode.onScreenOffDelay()
                    System.gc()
                }
            }
        }, SCREEN_OFF_SWITCH_NETWORK_DELAY + 1000)

        handler.postDelayed({
            if (!screenOn) {
                stopTimer()
                SceneMode.FreezeAppThread(context.applicationContext, true, 30).start()
            }
        }, 10000)
    }

    private fun onScreenOn() {
        lastScreenOnOff = System.currentTimeMillis()

        handler.postDelayed({
            if (lastMode.isNotEmpty()) {
                lastPackage = null
                lastModePackage = null
                EventBus.publish(EventType.STATE_RESUME)
                sceneMode.cancelFreezeAppThread()
            }
        }, 1000)
        sceneMode.onScreenOn()

        if (!screenOn) {
            screenOn = true
            startTimer()
        }
    }

    private fun autoToggleMode(packageName: String?) {
        if (packageName != null && packageName != lastModePackage) {
            lastModePackage = packageName
            setCurrentPowercfgApp(packageName)
        }
    }

    override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
        when (eventType) {
            EventType.APP_SWITCH -> onFocusedAppChanged(GlobalStatus.lastPackageName)
            EventType.SCREEN_ON -> onScreenOn()
            EventType.SCREEN_OFF -> {
                if (ScreenState(context).isScreenLocked()) {
                    onScreenOff()
                }
            }
            EventType.SCENE_CONFIG -> updateConfig()
            EventType.SCENE_APP_CONFIG -> {
                data?.run {
                    sceneMode.updateAppConfig()
                }
            }
            else -> return
        }
    }

    override fun eventFilter(eventType: EventType): Boolean {
        return when (eventType) {
            EventType.APP_SWITCH, EventType.SCREEN_OFF, EventType.SCREEN_ON, EventType.SCENE_CONFIG, EventType.SCENE_APP_CONFIG -> true
            else -> false
        }
    }

    override fun onSubscribe() {}

    override fun onUnsubscribe() {
        sceneMode.clearState()
        stopTimer()
        EventBus.unsubscribe(this)
    }

    private fun onFocusedAppChanged(packageName: String) {
        if (!screenOn && screenState.isScreenOn()) {
            onScreenOn()
        }

        if (lastPackage == packageName || ignoredList.contains(packageName) || sceneBlackList.contains(packageName)) return
        if (lastPackage == null) lastPackage = "com.android.systemui"

        autoToggleMode(packageName)
        sceneMode.onAppEnter(packageName)
        lastPackage = packageName
    }

    @SuppressLint("ApplySharedPref")
    private fun initConfig() {
        ignoredList.clear()
        ignoredList.addAll(context.resources.getStringArray(R.array.powercfg_force_igoned))
        ignoredList.addAll(InputMethodApp(context).getInputMethods())

        if (spfPowercfg.all.isEmpty()) {
            val modes = mapOf(
                R.array.powercfg_igoned to IGONED,
                R.array.powercfg_fast to FAST,
                R.array.powercfg_game to PERFORMANCE,
                R.array.powercfg_powersave to POWERSAVE
            )
            modes.forEach { (res, mode) ->
                context.resources.getStringArray(res).forEach { item ->
                    spfPowercfg.edit().putString(item, mode).apply()
                }
            }
        }
        
        spfGlobal.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, "").commit()
    }

    init {
        screenState = ScreenState(context)
        screenOn = screenState.isScreenOn()
        if (screenOn) {
            lastScreenOnOff = System.currentTimeMillis()
            startTimer()
        }

        if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)) {
            KeepShellPublic.doCmdSync(CommonCmds.DisableSELinux)
        }

        GlobalScope.launch(Dispatchers.IO) {
            initConfig()
        }

        EventBus.subscribe(this)
    }
}
