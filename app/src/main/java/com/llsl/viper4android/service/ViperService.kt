package com.llsl.viper4android.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.llsl.viper4android.R
import com.llsl.viper4android.SERVICE_CHANNEL_ID
import com.llsl.viper4android.audio.AudioDevice
import com.llsl.viper4android.audio.AudioOutputDetector
import com.llsl.viper4android.audio.AudioSessionInfo
import com.llsl.viper4android.audio.AudioSessionMonitor
import com.llsl.viper4android.data.model.DeviceSettings
import com.llsl.viper4android.data.repository.ViperRepository
import com.llsl.viper4android.effect.EffectState
import com.llsl.viper4android.effect.deserializeEffectPrefs
import com.llsl.viper4android.effect.loadEffectStateFromPrefs
import com.llsl.viper4android.effect.serializeEffectPrefs
import com.llsl.viper4android.utils.FileLogger
import com.llsl.viper4android.utils.WavDecoder
import com.llsl.viper4android.viper.AidlTransport
import com.llsl.viper4android.viper.BulkApplyResult
import com.llsl.viper4android.viper.BulkKind
import com.llsl.viper4android.viper.DriverStatus
import com.llsl.viper4android.viper.EffectRegistry
import com.llsl.viper4android.viper.HidlTransport
import com.llsl.viper4android.viper.ParamValue
import com.llsl.viper4android.viper.ViperEffect
import com.llsl.viper4android.viper.ViperParams
import com.llsl.viper4android.viper.ViperTransport
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class ViperService : LifecycleService() {
    @Inject
    lateinit var repository: ViperRepository

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_CODE_OPEN_APP = 0
        private const val REQUEST_CODE_TOGGLE_MASTER = 1
        private const val DDC_DRAIN_DELAY_MS = 50L
        private val NOTIFICATION_INITIAL_STREAM_PROBE_DELAY = 400.milliseconds
        private val NOTIFICATION_POLL_INTERVAL = 1500.milliseconds

        const val ACTION_START = "com.llsl.viper4android.service.START"
        const val ACTION_STOP = "com.llsl.viper4android.service.STOP"
        const val ACTION_TOGGLE_MASTER = "com.llsl.viper4android.service.TOGGLE_MASTER"
        const val EXTRA_MASTER_ENABLED = "com.llsl.viper4android.service.EXTRA_MASTER_ENABLED"
        const val EXTRA_MUST_START_FOREGROUND = "com.llsl.viper4android.service.EXTRA_MUST_START_FOREGROUND"

        fun startService(
            context: Context,
            fromBackground: Boolean = false,
        ) {
            val intent =
                Intent(context, ViperService::class.java).apply {
                    action = ACTION_START
                    if (fromBackground) {
                        putExtra(EXTRA_MUST_START_FOREGROUND, true)
                    }
                }
            if (fromBackground) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun toggleMaster(
            context: Context,
            enabled: Boolean,
        ) {
            val intent =
                Intent(context, ViperService::class.java).apply {
                    action = ACTION_TOGGLE_MASTER
                    putExtra(EXTRA_MASTER_ENABLED, enabled)
                    putExtra(EXTRA_MUST_START_FOREGROUND, true)
                }
            context.startForegroundService(intent)
        }

        fun parseVdc(file: File): Pair<List<FloatArray>, List<FloatArray>>? {
            try {
                var coeffs44100: FloatArray? = null
                var coeffs48000: FloatArray? = null
                for (line in file.readLines()) {
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("SR_44100:") -> {
                            coeffs44100 =
                                trimmed
                                    .removePrefix("SR_44100:")
                                    .split(",")
                                    .mapNotNull { token -> token.trim().takeIf { it.isNotEmpty() }?.toFloat() }
                                    .toFloatArray()
                        }

                        trimmed.startsWith("SR_48000:") -> {
                            coeffs48000 =
                                trimmed
                                    .removePrefix("SR_48000:")
                                    .split(",")
                                    .mapNotNull { token -> token.trim().takeIf { it.isNotEmpty() }?.toFloat() }
                                    .toFloatArray()
                        }
                    }
                }

                val a = coeffs44100
                val b = coeffs48000
                if (a == null || b == null || a.isEmpty() || a.size != b.size || a.size % 5 != 0) {
                    FileLogger.w(
                        "Service",
                        "DDC coefficient parse failure: ${file.name} 44100=${a?.size} 48000=${b?.size}",
                    )
                    return null
                }
                val sec44 = a.toList().chunked(5).map { it.toFloatArray() }
                val sec48 = b.toList().chunked(5).map { it.toFloatArray() }
                sec44.forEachIndexed { idx, s ->
                    if (!isStableBiquadSection(s)) {
                        FileLogger.e(
                            "Service",
                            "Unstable DDC section in ${file.name} (SR_44100[$idx]): a1=${s[3]} a2=${s[4]}",
                        )
                        return null
                    }
                }
                sec48.forEachIndexed { idx, s ->
                    if (!isStableBiquadSection(s)) {
                        FileLogger.e(
                            "Service",
                            "Unstable DDC section in ${file.name} (SR_48000[$idx]): a1=${s[3]} a2=${s[4]}",
                        )
                        return null
                    }
                }
                return sec44 to sec48
            } catch (e: Exception) {
                FileLogger.e("Service", "Failed to parse DDC: ${file.name}", e)
                return null
            }
        }

        internal fun isStableBiquadSection(section: FloatArray): Boolean {
            if (section.size != 5 || section.any { !it.isFinite() }) return false
            val a1 = section[3].toDouble()
            val a2 = section[4].toDouble()
            return a2 > -1.0 && a2 < 1.0 && (1.0 - a1 - a2) > 0.0 && (1.0 + a1 - a2) > 0.0
        }
    }

    inner class LocalBinder : Binder() {
        val service: ViperService get() = this@ViperService
    }

    private val binder = LocalBinder()
    private val effectRegistry = EffectRegistry()
    private lateinit var transport: ViperTransport
    private var useAidlTypeUuid: Boolean = true
    private var globalMode: Boolean = false
    private var audioOutputDetector: AudioOutputDetector? = null
    private var sessionMonitor: AudioSessionMonitor? = null
    private var stateProvider: (() -> EffectState)? = null
    private var lastUiState: EffectState? = null
    private val configMutex = Mutex()
    private val statePushMutex = Mutex()
    private var stateApplyJob: Job? = null

    @Volatile
    private var excludedApps: Set<String> = emptySet()

    private data class TrackedSession(
        val sessionId: Int,
        val packageName: String,
        val openedAt: Long,
        val usage: Int,
        val muted: Boolean = false,
    )

    private val sessionLock = Any()
    private val trackedSessions = LinkedHashMap<Int, TrackedSession>()
    private val _activeSessions = MutableStateFlow<List<AudioSessionInfo>>(emptyList())
    val activeSessions: StateFlow<List<AudioSessionInfo>> = _activeSessions.asStateFlow()

    private data class DecodedKernel(
        val samples: FloatArray,
        val rawBytes: ByteArray,
        val totalFloats: Int,
        val channelCount: Int,
        val crc: Int,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DecodedKernel

            if (totalFloats != other.totalFloats) return false
            if (channelCount != other.channelCount) return false
            if (crc != other.crc) return false
            if (!samples.contentEquals(other.samples)) return false
            if (!rawBytes.contentEquals(other.rawBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = totalFloats
            result = 31 * result + channelCount
            result = 31 * result + crc
            result = 31 * result + samples.contentHashCode()
            result = 31 * result + rawBytes.contentHashCode()
            return result
        }
    }

    private val decodedKernelCache =
        object : LinkedHashMap<String, DecodedKernel>(4, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DecodedKernel>?) = size > 3
        }

    private data class NotificationSnapshot(
        val installed: Boolean?,
        val masterEnabled: Boolean,
        val streaming: Boolean,
        val aidlMode: Boolean,
        val globalMode: Boolean,
        val deviceName: String,
        val enabledSessionCount: Int,
    )

    private var bootMasterEnabled: Boolean = false
    private var pendingExternalMaster: Boolean? = null
    private val masterEnabled: Boolean
        get() {
            val provided = stateProvider?.invoke()?.masterEnable
            val pending = pendingExternalMaster
            if (pending != null) {
                if (provided == null || provided == pending) {
                    pendingExternalMaster = null
                } else {
                    return pending
                }
            }
            return provided ?: lastUiState?.masterEnable ?: bootMasterEnabled
        }

    private var persistentNotificationEnabled: Boolean = false
    private var inForeground: Boolean = false
    private var screenReceiverRegistered: Boolean = false
    private var isScreenInteractive: Boolean = true
    private var notificationMonitorJob: Job? = null
    private var hasProbedDriverOnce: Boolean = false
    private var lastNotifDriverFrames: Long = -1L
    private var cachedDriverInstalled: Boolean = false
    private var cachedStreaming: Boolean = false
    private var lastNotificationSnapshot: NotificationSnapshot? = null

    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenInteractive = false
                        stopNotificationMonitor()
                    }

                    Intent.ACTION_SCREEN_ON -> {
                        isScreenInteractive = true
                        if (persistentNotificationEnabled) {
                            lastNotifDriverFrames = -1L
                            startNotificationMonitor()
                        }
                    }
                }
            }
        }

    fun setStateProvider(provider: () -> EffectState) {
        stateProvider = provider
    }

    override fun onCreate() {
        super.onCreate()
        isScreenInteractive = getSystemService(PowerManager::class.java)?.isInteractive != false
        FileLogger.i("Service", "Service created")
        lifecycleScope.launch {
            ensureConfigLoaded()
            syncNotificationState()
            launch {
                repository.getStringSetPreference(ViperRepository.PREF_EXCLUDED_APPS).collect { next ->
                    excludedApps = next
                    closeExcludedPackageSessions(next)
                }
            }
            launch {
                repository
                    .getBooleanPreference(ViperRepository.PREF_PERSISTENT_NOTIFICATION, false)
                    .distinctUntilChanged()
                    .collect { enabled ->
                        setPersistentNotificationEnabled(enabled)
                    }
            }
            startAudioOutputMonitor()
        }
    }

    @Volatile
    private var configLoaded = false

    private suspend fun ensureConfigLoaded() {
        if (configLoaded) return
        configMutex.withLock {
            if (configLoaded) return
            useAidlTypeUuid = repository.aidlMode
            effectRegistry.aidlTypeUuid = useAidlTypeUuid
            transport =
                if (useAidlTypeUuid) {
                    AidlTransport()
                } else {
                    HidlTransport(
                        { effectRegistry.liveEffects() },
                        activeEffect = { effectRegistry.activeEffect() },
                    )
                }.also { installBulkSideChannels(it) }
            globalMode = repository.resolveEffectiveGlobalMode(this)
            bootMasterEnabled = repository.getBooleanPreference(ViperRepository.PREF_MASTER_ENABLE).first()
            persistentNotificationEnabled =
                repository
                    .getBooleanPreference(ViperRepository.PREF_PERSISTENT_NOTIFICATION, false)
                    .first()
            configLoaded = true
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private fun initGlobalEffect(): ViperEffect? = effectRegistry.ensureGlobal()

    private fun installBulkSideChannels(t: ViperTransport) {
        t.bulkSideChannels = { ddc, kernel, clearDdc, clearConvolver, force, skipDrainDelay, send ->
            val ddcApplied =
                when {
                    ddc.isNotEmpty() -> {
                        streamDdcInternal(
                            name = ddc,
                            wasEnabled = !skipDrainDelay,
                            reEnable = false,
                            force = force,
                            skipDrainDelay = skipDrainDelay,
                            send = send,
                        )
                    }

                    clearDdc && t.shouldStream(BulkKind.DDC, "", force = false) -> {
                        streamDdcInternal(
                            name = "",
                            wasEnabled = !skipDrainDelay,
                            reEnable = false,
                            force = false,
                            skipDrainDelay = skipDrainDelay,
                            send = send,
                        )
                        false
                    }

                    else -> false
                }
            when {
                kernel.isNotEmpty() -> applyConvolverKernel(kernel, force, send)
                clearConvolver && t.shouldStream(BulkKind.CONVOLVER, "", force = false) -> {
                    applyConvolverKernel("", force = false, send = send)
                }
            }
            BulkApplyResult(ddcApplied = ddcApplied)
        }
    }

    private suspend fun applyState(
        state: EffectState,
        masterOn: Boolean,
    ) {
        if (!masterOn) {
            withContext(Dispatchers.Main.immediate) {
                stopSessionMonitor()
            }
            withContext(Dispatchers.IO) {
                statePushMutex.withLock {
                    releaseAllSessions()
                    effectRegistry.releaseGlobal()
                    if (::transport.isInitialized) {
                        transport.clearBulkCache()
                    }
                }
            }
            withContext(Dispatchers.Main.immediate) {
                cachedStreaming = false
                lastNotifDriverFrames = -1L
                notifyStatusChanged(restartMonitor = false)
            }
            return
        }
        val newGlobalEffect =
            if (globalMode) {
                val hadGlobal = effectRegistry.globalEffect() != null
                val effect = initGlobalEffect()
                !hadGlobal && effect != null
            } else {
                withContext(Dispatchers.Main.immediate) {
                    if (sessionMonitor == null) startSessionMonitor()
                }
                false
            }
        if (globalMode && effectRegistry.globalEffect() == null) {
            FileLogger.w("Service", "Skipping full state push: global effect unavailable")
            return
        }
        effectRegistry.liveEffects().forEach { it.enabled = true }
        withContext(Dispatchers.IO) {
            statePushMutex.withLock {
                if (!masterEnabled) return@withLock
                transport.pushFullState(state, forceBulk = newGlobalEffect)
            }
        }
        withContext(Dispatchers.Main.immediate) {
            lastNotifDriverFrames = -1L
            notifyStatusChanged(restartMonitor = true)
        }
    }

    private var currentServiceDeviceId: String = ""
    private var currentServiceDeviceName: String = ""

    private fun startAudioOutputMonitor() {
        val detector = AudioOutputDetector(this)
        audioOutputDetector = detector
        lifecycleScope.launch {
            detector.activeDevice.collect { device ->
                if (device.id != currentServiceDeviceId) {
                    FileLogger.i(
                        "Service",
                        "Device changed: $currentServiceDeviceId -> ${device.id} (${device.name})",
                    )
                    currentServiceDeviceId = device.id
                    currentServiceDeviceName = device.name
                    lastUiState = null
                    reapplyForDevice(device)
                }
            }
        }
    }

    private suspend fun reapplyForDevice(device: AudioDevice) {
        val saved = repository.getDeviceSettings(device.id)
        val state: EffectState =
            if (saved != null) {
                FileLogger.i("Service", "Loading device settings from DB for ${device.id}")
                currentServiceDeviceName = saved.deviceName.ifEmpty { device.name }
                val baseState = loadEffectStateFromPrefs(repository)
                val json = JSONObject(saved.settingsJson)
                deserializeEffectPrefs(json, baseState).also {
                    repository.updateDeviceLastConnected(device.id)
                }
            } else {
                FileLogger.i("Service", "No DB entry for ${device.id}, using DataStore defaults")
                currentServiceDeviceName = device.name
                val s = loadEffectStateFromPrefs(repository)
                val json = serializeEffectPrefs(s)
                repository.saveDeviceSettings(
                    DeviceSettings(
                        deviceId = device.id,
                        deviceName = device.name,
                        isHeadphone = device.isHeadphone,
                        settingsJson = json.toString(),
                    ),
                )
                s
            }
        lastUiState = state
        applyState(state, masterEnabled)
    }

    private suspend fun currentDeviceState(): EffectState {
        stateProvider?.invoke()?.let { state ->
            if (state.activeDeviceId == currentServiceDeviceId) return state
        }
        lastUiState?.let { return it }
        val device = audioOutputDetector?.activeDevice?.value ?: AudioDevice.DEFAULT_SPEAKER
        val baseState = loadEffectStateFromPrefs(repository)
        val saved = repository.getDeviceSettings(device.id) ?: return baseState
        return deserializeEffectPrefs(JSONObject(saved.settingsJson), baseState)
    }

    private suspend fun dispatchFullStateToEffect(targetEffect: ViperEffect? = null) {
        if (!masterEnabled) return
        val state = currentDeviceState()
        lastUiState = state
        withContext(Dispatchers.IO) {
            statePushMutex.withLock {
                if (!masterEnabled) return@withLock
                transport.pushFullState(
                    state = state,
                    forceBulk = true,
                    targetEffect = targetEffect,
                )
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)

        // 通过 startForegroundService 拉起时必须立即调用 startForeground 满足系统 FGS 契约
        if (intent?.getBooleanExtra(EXTRA_MUST_START_FOREGROUND, false) == true && !inForeground) {
            enterForeground(buildStatusNotification(buildNotificationSnapshot()))
        }

        when (intent?.action) {
            ACTION_START -> {
                FileLogger.i("Service", "Service started")
                lifecycleScope.launch {
                    ensureConfigLoaded()
                    syncNotificationState()
                }
            }

            ACTION_STOP -> {
                stateApplyJob?.cancel()
                stopNotificationMonitor()
                unregisterScreenReceiver()
                stopSessionMonitor()
                releaseAllSessions()
                effectRegistry.releaseGlobal()
                if (::transport.isInitialized) {
                    transport.clearBulkCache()
                }
                exitForeground()
                stopSelf()
            }

            ACTION_TOGGLE_MASTER -> {
                val next = intent.getBooleanExtra(EXTRA_MASTER_ENABLED, false)
                stateApplyJob?.cancel()
                stateApplyJob =
                    lifecycleScope.launch {
                        ensureConfigLoaded()
                        pendingExternalMaster = next
                        bootMasterEnabled = next
                        repository.setBooleanPreference(ViperRepository.PREF_MASTER_ENABLE, next)
                        val state = currentDeviceState().copy(masterEnable = next)
                        lastUiState = state
                        applyState(state, next)
                        syncNotificationState()
                        requestMasterTileUpdate()
                    }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stateApplyJob?.cancel()
        stopNotificationMonitor()
        unregisterScreenReceiver()
        lastNotificationSnapshot = null
        stopSessionMonitor()
        audioOutputDetector?.stop()
        audioOutputDetector = null
        effectRegistry.releaseGlobal()
        releaseAllSessions()
        if (::transport.isInitialized) {
            transport.clearBulkCache()
        }
        FileLogger.i("Service", "Service destroyed")
        super.onDestroy()
    }

    private fun startSessionMonitor() {
        stopSessionMonitor()
        val monitor =
            AudioSessionMonitor(
                context = this,
                onSessionOpen = { sessionId, pkg, usage -> openSession(sessionId, pkg, usage) },
                onSessionClose = { sessionId -> closeSession(sessionId) },
                onSessionUpdate = { sessionId, pkg, usage -> updateSessionMetadata(sessionId, pkg, usage) },
            )
        monitor.start()
        sessionMonitor = monitor
    }

    private fun stopSessionMonitor() {
        sessionMonitor?.stop()
        sessionMonitor = null
    }

    private fun publishSessions() {
        val snapshot =
            synchronized(sessionLock) {
                trackedSessions.values.map { record ->
                    AudioSessionInfo(
                        sessionId = record.sessionId,
                        packageName = record.packageName,
                        enabled = !record.muted,
                        openedAt = record.openedAt,
                        usage = record.usage,
                    )
                }
            }
        _activeSessions.value = snapshot
        val restartMonitor = snapshot.any { it.enabled }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notifyStatusChanged(restartMonitor = restartMonitor)
        } else {
            lifecycleScope.launch {
                notifyStatusChanged(restartMonitor = restartMonitor)
            }
        }
    }

    private fun openSession(
        sessionId: Int,
        packageName: String,
        usage: Int,
    ): Boolean {
        if (!masterEnabled) {
            FileLogger.d(
                "Service",
                "Master off: skipping per-app session $sessionId ($packageName)",
            )
            return true
        }
        if (globalMode) {
            FileLogger.d(
                "Service",
                "Global mode: skipping per-app session $sessionId ($packageName)",
            )
            return true
        }
        if (packageName in excludedApps) {
            FileLogger.i(
                "Service",
                "Excluded app: skipping session $sessionId ($packageName)",
            )
            return true
        }
        synchronized(sessionLock) {
            val existing = trackedSessions[sessionId]
            if (existing != null && existing.muted) {
                FileLogger.i(
                    "Service",
                    "Manually disabled session $sessionId ($packageName): keeping muted",
                )
                return true
            }
        }
        if (effectRegistry.isSessionOpen(sessionId)) {
            FileLogger.w("Service", "Session $sessionId already open")
            return true
        }

        val (effect, alreadyExisted) = effectRegistry.openSession(sessionId, packageName) ?: return false

        synchronized(sessionLock) {
            trackedSessions[sessionId] =
                TrackedSession(
                    sessionId = sessionId,
                    packageName = packageName,
                    openedAt = System.currentTimeMillis(),
                    usage = usage,
                    muted = false,
                )
        }
        publishSessions()

        lifecycleScope.launch {
            ensureConfigLoaded()
            dispatchFullStateToEffect(targetEffect = if (alreadyExisted) null else effect)
            FileLogger.i("Service", "Applied full state to session $sessionId")
        }
        return true
    }

    private fun closeSession(sessionId: Int) {
        effectRegistry.closeSession(sessionId)
        if (::transport.isInitialized && effectRegistry.liveEffects().isEmpty()) {
            transport.clearBulkCache()
        }
        val removed = synchronized(sessionLock) { trackedSessions.remove(sessionId) != null }
        if (removed) {
            publishSessions()
        }
    }

    private fun updateSessionMetadata(
        sessionId: Int,
        packageName: String,
        usage: Int,
    ) {
        if (packageName.isNotEmpty() && packageName in excludedApps) {
            FileLogger.i("Service", "Resolved excluded package for session $sessionId ($packageName): closing")
            closeSession(sessionId)
            return
        }
        val updated =
            synchronized(sessionLock) {
                val current = trackedSessions[sessionId] ?: return@synchronized false
                val isNewRealPkg = packageName.isNotEmpty() && !packageName.all(Char::isDigit)
                val nextPkg =
                    when {
                        isNewRealPkg -> packageName
                        current.packageName.isEmpty() && packageName.isNotEmpty() -> packageName
                        else -> current.packageName
                    }
                val nextUsage =
                    if (usage != AudioAttributes.USAGE_UNKNOWN && current.usage == AudioAttributes.USAGE_UNKNOWN) {
                        usage
                    } else {
                        current.usage
                    }
                if (nextPkg == current.packageName && nextUsage == current.usage) {
                    return@synchronized false
                }
                trackedSessions[sessionId] = current.copy(packageName = nextPkg, usage = nextUsage)
                true
            }
        if (updated) {
            FileLogger.i("Service", "Updated session $sessionId metadata (pkg=$packageName, usage=$usage)")
            publishSessions()
        }
    }

    private fun releaseAllSessions() {
        effectRegistry.releaseAll()
        val cleared =
            synchronized(sessionLock) {
                val hadAny = trackedSessions.isNotEmpty()
                trackedSessions.clear()
                hadAny
            }
        if (cleared) {
            publishSessions()
        }
    }

    fun setSessionEnabled(
        sessionId: Int,
        enabled: Boolean,
    ) {
        setSessionsEnabled(setOf(sessionId), enabled)
    }

    fun setSessionsEnabled(
        sessionIds: Set<Int>,
        enabled: Boolean,
    ) {
        if (sessionIds.isEmpty()) return
        val reopenedEffects = mutableListOf<Pair<ViperEffect, Boolean>>()
        var changed = false

        for (sessionId in sessionIds) {
            if (!enabled) {
                effectRegistry.closeSession(sessionId)
                synchronized(sessionLock) {
                    val record = trackedSessions[sessionId]
                    if (record != null && !record.muted) {
                        trackedSessions[sessionId] = record.copy(muted = true)
                        changed = true
                        FileLogger.i("Service", "Manually disabled session $sessionId (${record.packageName})")
                    }
                }
            } else {
                if (globalMode || !masterEnabled) continue
                val record = synchronized(sessionLock) { trackedSessions[sessionId] } ?: continue
                if (!record.muted || record.packageName in excludedApps) continue
                val opened = effectRegistry.openSession(sessionId, record.packageName) ?: continue
                synchronized(sessionLock) {
                    trackedSessions[sessionId]?.let { current ->
                        trackedSessions[sessionId] = current.copy(muted = false)
                    }
                }
                reopenedEffects.add(opened)
                changed = true
                FileLogger.i("Service", "Re-enabled session $sessionId (${record.packageName})")
            }
        }

        if (!enabled && ::transport.isInitialized && effectRegistry.liveEffects().isEmpty()) {
            transport.clearBulkCache()
        }
        if (changed) {
            publishSessions()
        }
        if (reopenedEffects.isNotEmpty()) {
            val singleTarget =
                if (reopenedEffects.size == 1 && !reopenedEffects[0].second) {
                    reopenedEffects[0].first
                } else {
                    null
                }
            lifecycleScope.launch {
                ensureConfigLoaded()
                dispatchFullStateToEffect(targetEffect = singleTarget)
            }
        }
    }

    fun excludePackageSessions(packageName: String) {
        if (packageName.isEmpty()) return
        closeExcludedPackageSessions(setOf(packageName))
    }

    private fun closeExcludedPackageSessions(packages: Set<String>) {
        if (packages.isEmpty()) return
        val toClose =
            synchronized(sessionLock) {
                trackedSessions.values
                    .filter { it.packageName in packages }
                    .map { it.sessionId }
            }
        if (toClose.isEmpty()) return
        for (sid in toClose) {
            effectRegistry.closeSession(sid)
        }
        if (::transport.isInitialized && effectRegistry.liveEffects().isEmpty()) {
            transport.clearBulkCache()
        }
        synchronized(sessionLock) {
            for (sid in toClose) {
                trackedSessions.remove(sid)
            }
        }
        FileLogger.i("Service", "Closed ${toClose.size} session(s) for excluded package(s)")
        publishSessions()
    }

    fun refreshSessions(onComplete: (() -> Unit)? = null) {
        val monitor = sessionMonitor
        if (!globalMode && masterEnabled && monitor != null) {
            monitor.refresh(onComplete)
        } else {
            onComplete?.invoke()
        }
    }

    fun setParam(
        param: Int,
        value: ParamValue,
    ): Boolean = ::transport.isInitialized && transport.set(param, value)

    fun dispatchFullState(state: EffectState) {
        pendingExternalMaster = null
        lastUiState = state
        val masterOn = masterEnabled
        stateApplyJob?.cancel()
        stateApplyJob =
            lifecycleScope.launch {
                ensureConfigLoaded()
                applyState(state, masterOn)
            }
        requestMasterTileUpdate()
    }

    fun updateActiveDeviceName(name: String) {
        if (name.isNotEmpty() && name != currentServiceDeviceName) {
            currentServiceDeviceName = name
            notifyStatusChanged(restartMonitor = false)
        }
    }

    fun setPersistentNotificationEnabled(enabled: Boolean) {
        persistentNotificationEnabled = enabled
        syncNotificationState()
    }

    fun applyConvolverKernel(
        fileName: String,
        force: Boolean = false,
        send: (Int, ParamValue) -> Boolean = transport::set,
    ) {
        if (!::transport.isInitialized) return
        if (!transport.shouldStream(BulkKind.CONVOLVER, fileName, force)) return
        val sendInts: (Int, Int, Int, Int) -> Boolean =
            { p, a, b, c -> send(p, ParamValue.Ints(intArrayOf(a, b, c))) }
        val sendFloats: (Int, FloatArray) -> Boolean =
            { p, v -> send(p, ParamValue.Floats(v)) }
        if (fileName.isEmpty()) {
            if (sendInts(ViperParams.PARAM_CONVOLVER_PREPARE_BUFFER, 0, 0, 1)) {
                transport.markStreamed(BulkKind.CONVOLVER, "")
            }
            return
        }
        val src = File(File(getExternalFilesDir(null), "Kernel"), fileName)
        if (!src.exists()) {
            FileLogger.w("Service", "Kernel file missing: $fileName")
            return
        }

        try {
            val decoded = synchronized(decodedKernelCache) { decodedKernelCache[src.absolutePath] }
            val samples: FloatArray
            val totalFloats: Int
            val channelCount: Int
            val crc: Int
            if (decoded != null) {
                samples = decoded.samples
                totalFloats = decoded.totalFloats
                channelCount = decoded.channelCount
                crc = decoded.crc
            } else {
                val d = WavDecoder.decode(src.readBytes())
                samples = d.samples
                totalFloats = d.samples.size
                channelCount = d.channels
                FileLogger.i("Service", "Kernel decoded: $fileName samples=$totalFloats ch=$channelCount")
                val rawBytes =
                    ByteBuffer
                        .allocate(totalFloats * 4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .also { for (f in samples) it.putFloat(f) }
                        .array()
                crc = CRC32().apply { update(rawBytes) }.value.toInt()
                synchronized(decodedKernelCache) {
                    decodedKernelCache[src.absolutePath] =
                        DecodedKernel(samples, rawBytes, totalFloats, channelCount, crc)
                }
            }

            if (!sendInts(ViperParams.PARAM_CONVOLVER_PREPARE_BUFFER, totalFloats, channelCount, 0)) {
                FileLogger.w("Service", "Kernel prepare failed: $fileName")
                return
            }

            val maxFloatsPerChunk = 2046
            var offset = 0
            var chunkIndex = 0
            while (offset < totalFloats) {
                val floatsInChunk = minOf(totalFloats - offset, maxFloatsPerChunk)
                if (!sendFloats(
                        ViperParams.PARAM_CONVOLVER_SET_BUFFER,
                        samples.copyOfRange(offset, offset + floatsInChunk),
                    )
                ) {
                    FileLogger.w("Service", "Kernel chunk $chunkIndex dispatch failed: $fileName")
                    return
                }
                offset += floatsInChunk
                chunkIndex++
            }

            val kernelId = fileName.hashCode()
            if (!sendInts(ViperParams.PARAM_CONVOLVER_COMMIT_BUFFER, totalFloats, crc, kernelId)) {
                FileLogger.w("Service", "Kernel commit failed: $fileName")
                return
            }
            FileLogger.i("Service", "Kernel streamed: $fileName chunks=$chunkIndex crc=0x${crc.toUInt().toString(16)}")
            transport.markStreamed(BulkKind.CONVOLVER, fileName)
        } catch (e: Exception) {
            FileLogger.e("Service", "Failed to stream kernel: $fileName", e)
        }
    }

    fun needsDdcStream(name: String): Boolean =
        ::transport.isInitialized && transport.shouldStream(BulkKind.DDC, name, force = false)

    private fun loadDdcCoeffs(name: String): FloatArray? {
        if (name.isEmpty()) return FloatArray(0)
        val file = File(File(getExternalFilesDir(null), "DDC"), "$name.vdc")
        if (!file.exists()) {
            FileLogger.w("Service", "DDC file missing: $name")
            return null
        }
        val parsed = parseVdc(file) ?: return null
        val sec44100 = parsed.first
        val sec48000 = parsed.second
        val floatsPerRate = sec44100.size * 5
        val coeffs = FloatArray(floatsPerRate * 2)
        var i = 0
        for (s in sec44100) for (v in s) coeffs[i++] = v
        for (s in sec48000) for (v in s) coeffs[i++] = v
        return coeffs
    }

    private fun isDdcStillWanted(name: String): Boolean {
        val state = stateProvider?.invoke() ?: lastUiState ?: return true
        return state.masterEnable && state.ddc.enable && state.ddc.device == name
    }

    private suspend fun streamDdcInternal(
        name: String,
        wasEnabled: Boolean,
        reEnable: Boolean,
        force: Boolean,
        skipDrainDelay: Boolean = false,
        send: (Int, ParamValue) -> Boolean = transport::set,
    ): Boolean {
        if (!::transport.isInitialized) return false
        if (!transport.shouldStream(BulkKind.DDC, name, force)) {
            if (reEnable && name.isNotEmpty() && isDdcStillWanted(name)) {
                send(ViperParams.PARAM_DDC_ENABLE, ParamValue.Bool(true))
            }
            return name.isNotEmpty()
        }
        val coeffs = loadDdcCoeffs(name)
        if (coeffs == null) {
            if (wasEnabled) {
                send(ViperParams.PARAM_DDC_ENABLE, ParamValue.Bool(false))
                if (!skipDrainDelay) {
                    delay(DDC_DRAIN_DELAY_MS.milliseconds)
                }
            }
            if (send(ViperParams.PARAM_DDC_COEFFICIENTS, ParamValue.Floats(FloatArray(0)))) {
                transport.markStreamed(BulkKind.DDC, "")
            }
            return false
        }
        if (wasEnabled) {
            send(ViperParams.PARAM_DDC_ENABLE, ParamValue.Bool(false))
            if (!skipDrainDelay) {
                delay(DDC_DRAIN_DELAY_MS.milliseconds)
            }
        }
        if (!send(ViperParams.PARAM_DDC_COEFFICIENTS, ParamValue.Floats(coeffs))) {
            return false
        }
        transport.markStreamed(BulkKind.DDC, name)
        if (reEnable && name.isNotEmpty() && isDdcStillWanted(name) && currentCoroutineContext().isActive) {
            send(ViperParams.PARAM_DDC_ENABLE, ParamValue.Bool(true))
        }
        return name.isNotEmpty()
    }

    suspend fun switchDdcDeviceSafely(
        name: String,
        wasEnabled: Boolean,
        reEnable: Boolean,
        force: Boolean = false,
    ): Boolean =
        withContext(Dispatchers.IO) {
            statePushMutex.withLock {
                streamDdcInternal(
                    name = name,
                    wasEnabled = wasEnabled,
                    reEnable = reEnable,
                    force = force,
                )
            }
        }

    fun probeDriverStatus(): DriverStatus? =
        if (::transport.isInitialized) transport.probeStatus() else null

    fun setGlobalMode(enabled: Boolean) {
        globalMode = enabled
        if (!masterEnabled) {
            stateApplyJob?.cancel()
            stateApplyJob =
                lifecycleScope.launch {
                    ensureConfigLoaded()
                    applyState(EffectState(), false)
                }
            return
        }
        if (enabled) {
            stopSessionMonitor()
            releaseAllSessions()
        } else {
            effectRegistry.releaseGlobal()
        }
        if (::transport.isInitialized) {
            transport.clearBulkCache()
        }
        stateApplyJob?.cancel()
        stateApplyJob =
            lifecycleScope.launch {
                ensureConfigLoaded()
                applyState(currentDeviceState(), true)
            }
    }

    private fun canPostNotifications(): Boolean {
        val granted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

    private fun syncNotificationState() {
        if (persistentNotificationEnabled && canPostNotifications()) {
            registerScreenReceiver()
            val snapshot = buildNotificationSnapshot()
            val notification = buildStatusNotification(snapshot)
            if (!inForeground) {
                enterForeground(notification)
                lastNotificationSnapshot = snapshot
            } else {
                postNotificationIfChanged(snapshot)
            }
            if (isScreenInteractive && notificationMonitorJob?.isActive != true) {
                startNotificationMonitor()
            }
        } else {
            stopNotificationMonitor()
            unregisterScreenReceiver()
            if (inForeground) {
                exitForeground()
            } else {
                getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
            }
            lastNotificationSnapshot = null
        }
    }

    private fun notifyStatusChanged(restartMonitor: Boolean) {
        if (!persistentNotificationEnabled || !canPostNotifications()) return
        if (!inForeground) {
            syncNotificationState()
            return
        }
        postNotificationIfChanged()
        if (restartMonitor && isScreenInteractive) {
            startNotificationMonitor()
        }
    }

    private fun enterForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            inForeground = true
        } catch (e: Exception) {
            FileLogger.w("Service", "Unable to enter foreground for status notification: ${e.message}")
        }
    }

    private fun exitForeground() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            FileLogger.w("Service", "stopForeground failed: ${e.message}")
        }
        inForeground = false
        getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        ContextCompat.registerReceiver(this, screenStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        screenReceiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: IllegalArgumentException) {
            // 忽略未注册异常
        }
        screenReceiverRegistered = false
    }

    private fun startNotificationMonitor() {
        if (!persistentNotificationEnabled || !isScreenInteractive || !canPostNotifications()) return
        notificationMonitorJob?.cancel()
        notificationMonitorJob =
            lifecycleScope.launch {
                if (!configLoaded) ensureConfigLoaded()
                var firstTick = true
                while (persistentNotificationEnabled && isScreenInteractive) {
                    if (shouldProbeDriverForNotification()) {
                        probeDriverForNotification()
                    } else {
                        cachedStreaming = false
                        lastNotifDriverFrames = -1L
                    }
                    postNotificationIfChanged()

                    if (!masterEnabled || (!useAidlTypeUuid && !globalMode && enabledSessionCount() == 0)) {
                        break
                    }
                    val waitDuration =
                        if (firstTick && lastNotifDriverFrames >= 0L && !cachedStreaming) {
                            NOTIFICATION_INITIAL_STREAM_PROBE_DELAY
                        } else {
                            NOTIFICATION_POLL_INTERVAL
                        }
                    firstTick = false
                    delay(waitDuration)
                }
            }
    }

    private fun stopNotificationMonitor() {
        notificationMonitorJob?.cancel()
        notificationMonitorJob = null
    }

    private fun shouldProbeDriverForNotification(): Boolean {
        if (!::transport.isInitialized) return false
        if (!hasProbedDriverOnce) return true
        if (!masterEnabled) return false
        // HIDL 会话模式无活跃会话时避免周期性创建临时 probe effect
        if (!useAidlTypeUuid && !globalMode && enabledSessionCount() == 0) return false
        return true
    }

    private suspend fun probeDriverForNotification() {
        val status =
            withContext(Dispatchers.IO) {
                runCatching { transport.probeStatus() }.getOrNull()
            }
        hasProbedDriverOnce = true
        if (status != null && status.versionCode > 0) {
            cachedDriverInstalled = true
            cachedStreaming =
                masterEnabled &&
                lastNotifDriverFrames >= 0L &&
                status.processedFrames != lastNotifDriverFrames
            lastNotifDriverFrames = status.processedFrames
        } else {
            if (!cachedDriverInstalled) {
                cachedDriverInstalled = false
            }
            cachedStreaming = false
        }
    }

    private fun enabledSessionCount(): Int = _activeSessions.value.count { it.enabled }

    private fun resolveDisplayDeviceName(): String {
        val uiName = stateProvider?.invoke()?.activeDeviceName.orEmpty()
        if (uiName.isNotEmpty()) return uiName
        if (currentServiceDeviceName.isNotEmpty()) return currentServiceDeviceName
        val detected = audioOutputDetector?.activeDevice?.value?.name.orEmpty()
        return detected.ifEmpty { getString(R.string.device_mode_speaker) }
    }

    private fun buildNotificationSnapshot(): NotificationSnapshot {
        val masterOn = masterEnabled
        return NotificationSnapshot(
            installed = if (hasProbedDriverOnce) cachedDriverInstalled else null,
            masterEnabled = masterOn,
            streaming = masterOn && cachedStreaming,
            aidlMode = useAidlTypeUuid,
            globalMode = globalMode,
            deviceName = resolveDisplayDeviceName(),
            enabledSessionCount = if (!globalMode && masterOn) enabledSessionCount() else 0,
        )
    }

    private fun postNotificationIfChanged(snapshot: NotificationSnapshot = buildNotificationSnapshot()) {
        if (!inForeground || !persistentNotificationEnabled) return
        if (snapshot == lastNotificationSnapshot) return
        lastNotificationSnapshot = snapshot
        getSystemService(NotificationManager::class.java)?.notify(
            NOTIFICATION_ID,
            buildStatusNotification(snapshot),
        )
    }

    private fun requestMasterTileUpdate() {
        runCatching {
            TileService.requestListeningState(
                this,
                ComponentName(this, MasterTileService::class.java),
            )
        }
    }

    private fun buildStatusNotification(snapshot: NotificationSnapshot): Notification {
        val launchIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val openAppIntent =
            launchIntent?.let {
                PendingIntent.getActivity(
                    this,
                    REQUEST_CODE_OPEN_APP,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val notInstalled = snapshot.installed == false
        val statusLabel =
            when {
                notInstalled -> getString(R.string.overview_driver_not_installed)
                !snapshot.masterEnabled -> getString(R.string.notification_status_disabled)
                snapshot.streaming -> getString(R.string.overview_driver_working)
                else -> getString(R.string.overview_status_standby)
            }
        val title =
            getString(
                R.string.notification_title_format,
                getString(R.string.notification_title),
                statusLabel,
            )

        val aidlLabel =
            getString(
                if (snapshot.aidlMode) R.string.overview_mode_aidl else R.string.overview_mode_legacy,
            )
        val scopeLabel =
            when {
                snapshot.globalMode -> getString(R.string.overview_mode_global)
                snapshot.enabledSessionCount > 0 ->
                    getString(R.string.notification_session_count, snapshot.enabledSessionCount)
                else -> getString(R.string.overview_mode_session)
            }

        val contentText =
            if (notInstalled) {
                getString(R.string.driver_not_found)
            } else {
                listOf(snapshot.deviceName, scopeLabel).joinToString(" · ")
            }

        val expandedText =
            if (notInstalled) {
                contentText
            } else {
                listOf(snapshot.deviceName, aidlLabel, scopeLabel).joinToString(" · ")
            }

        val toggleIntent =
            Intent(this, ViperService::class.java).apply {
                action = ACTION_TOGGLE_MASTER
                putExtra(EXTRA_MASTER_ENABLED, !snapshot.masterEnabled)
                putExtra(EXTRA_MUST_START_FOREGROUND, false)
            }
        val togglePendingIntent =
            PendingIntent.getService(
                this,
                REQUEST_CODE_TOGGLE_MASTER,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val toggleLabel =
            getString(
                if (snapshot.masterEnabled) R.string.notification_action_disable else R.string.notification_action_enable,
            )

        return NotificationCompat
            .Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setSmallIcon(R.drawable.ic_qs_master)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent)
            .addAction(0, toggleLabel, togglePendingIntent)
            .build()
    }
}
