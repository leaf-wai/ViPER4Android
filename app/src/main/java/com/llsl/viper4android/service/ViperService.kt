package com.llsl.viper4android.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.llsl.viper4android.R
import com.llsl.viper4android.SERVICE_CHANNEL_ID
import com.llsl.viper4android.audio.AudioDevice
import com.llsl.viper4android.audio.AudioOutputDetector
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
        private const val DDC_DRAIN_DELAY_MS = 50L

        const val ACTION_START = "com.llsl.viper4android.service.START"
        const val ACTION_STOP = "com.llsl.viper4android.service.STOP"
        const val ACTION_TOGGLE_MASTER = "com.llsl.viper4android.service.TOGGLE_MASTER"
        const val EXTRA_MASTER_ENABLED = "com.llsl.viper4android.service.EXTRA_MASTER_ENABLED"

        fun startService(context: Context) {
            val intent =
                Intent(context, ViperService::class.java).apply {
                    action = ACTION_START
                }
            context.startForegroundService(intent)
        }

        fun toggleMaster(
            context: Context,
            enabled: Boolean,
        ) {
            val intent =
                Intent(context, ViperService::class.java).apply {
                    action = ACTION_TOGGLE_MASTER
                    putExtra(EXTRA_MASTER_ENABLED, enabled)
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

    private var bootMasterEnabled: Boolean = false
    private val masterEnabled: Boolean
        get() = stateProvider?.invoke()?.masterEnable ?: lastUiState?.masterEnable ?: bootMasterEnabled

    fun setStateProvider(provider: () -> EffectState) {
        stateProvider = provider
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        FileLogger.i("Service", "Service created")
        lifecycleScope.launch {
            ensureConfigLoaded()
            launch {
                repository.getStringSetPreference(ViperRepository.PREF_EXCLUDED_APPS).collect { excludedApps = it }
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
            globalMode = repository.getBooleanPreference(ViperRepository.PREF_GLOBAL_MODE).first()
            bootMasterEnabled = repository.getBooleanPreference(ViperRepository.PREF_MASTER_ENABLE).first()
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
    }

    private var currentServiceDeviceId: String = ""

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
                val baseState = loadEffectStateFromPrefs(repository)
                val json = JSONObject(saved.settingsJson)
                deserializeEffectPrefs(json, baseState).also {
                    repository.updateDeviceLastConnected(device.id)
                }
            } else {
                FileLogger.i("Service", "No DB entry for ${device.id}, using DataStore defaults")
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

        when (intent?.action) {
            ACTION_START -> {
                FileLogger.i("Service", "Service started")
            }

            ACTION_STOP -> {
                stateApplyJob?.cancel()
                stopSessionMonitor()
                releaseAllSessions()
                effectRegistry.releaseGlobal()
                if (::transport.isInitialized) {
                    transport.clearBulkCache()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_TOGGLE_MASTER -> {
                val next = intent.getBooleanExtra(EXTRA_MASTER_ENABLED, false)
                stateApplyJob?.cancel()
                stateApplyJob =
                    lifecycleScope.launch {
                        ensureConfigLoaded()
                        bootMasterEnabled = next
                        val state = currentDeviceState().copy(masterEnable = next)
                        stateProvider = null
                        lastUiState = state
                        applyState(state, next)
                    }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stateApplyJob?.cancel()
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
                onSessionOpen = { sessionId, pkg -> openSession(sessionId, pkg) },
                onSessionClose = { sessionId -> closeSession(sessionId) },
            )
        monitor.start()
        sessionMonitor = monitor
    }

    private fun stopSessionMonitor() {
        sessionMonitor?.stop()
        sessionMonitor = null
    }

    private fun openSession(
        sessionId: Int,
        packageName: String,
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
        if (effectRegistry.isSessionOpen(sessionId)) {
            FileLogger.w("Service", "Session $sessionId already open")
            return true
        }

        val (effect, alreadyExisted) = effectRegistry.openSession(sessionId, packageName) ?: return false

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
    }

    private fun releaseAllSessions() {
        effectRegistry.releaseAll()
    }

    fun setParam(
        param: Int,
        value: ParamValue,
    ): Boolean = ::transport.isInitialized && transport.set(param, value)

    fun dispatchFullState(state: EffectState) {
        lastUiState = state
        val masterOn = masterEnabled
        stateApplyJob?.cancel()
        stateApplyJob =
            lifecycleScope.launch {
                ensureConfigLoaded()
                applyState(state, masterOn)
            }
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

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent =
            launchIntent?.let {
                PendingIntent.getActivity(
                    this,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        return NotificationCompat
            .Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
