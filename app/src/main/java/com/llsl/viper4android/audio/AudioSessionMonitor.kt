package com.llsl.viper4android.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import com.llsl.viper4android.utils.FileLogger
import com.llsl.viper4android.utils.RootShell
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

class AudioSessionMonitor(
    private val context: Context,
    private val onSessionOpen: (sessionId: Int, packageName: String) -> Boolean,
    private val onSessionClose: (sessionId: Int) -> Unit,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activePiids = mutableMapOf<Int, SessionInfo>()
    private val failedSessionRetries = mutableMapOf<Int, Int>()
    private val resolveLock = Any()

    @Volatile
    private var registered = false

    @Volatile
    private var generation = 0
    private var resolving = false
    private var rescanRequested = false
    private var resolveExecutor: ExecutorService? = null

    private val retryRunnable =
        Runnable {
            if (registered) {
                scheduleResolve()
            }
        }

    private val isSystemPrivileged: Boolean by lazy {
        context.checkSelfPermission("android.permission.MODIFY_AUDIO_ROUTING") ==
            PackageManager.PERMISSION_GRANTED
    }

    private data class SessionInfo(
        val sessionId: Int,
        val packageName: String,
    )

    private val playbackCallback =
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                mainHandler.removeCallbacks(retryRunnable)
                scheduleResolve()
            }
        }

    fun start() {
        val started =
            synchronized(resolveLock) {
                if (registered) return
                generation++
                registered = true
                resolving = false
                rescanRequested = false
                resolveExecutor =
                    Executors.newSingleThreadExecutor { runnable ->
                        Thread(runnable, "V4A-SessionMonitor").apply { isDaemon = true }
                    }
                true
            }
        if (!started) return
        FileLogger.i(TAG, "Starting playback monitor")
        audioManager.registerAudioPlaybackCallback(playbackCallback, mainHandler)
        scheduleResolve()
    }

    fun stop() {
        val executorToShutdown: ExecutorService?
        synchronized(resolveLock) {
            if (!registered) return
            registered = false
            generation++
            rescanRequested = false
            executorToShutdown = resolveExecutor
            resolveExecutor = null
        }
        audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        mainHandler.removeCallbacks(retryRunnable)
        executorToShutdown?.shutdownNow()
        val clearState = {
            activePiids.clear()
            failedSessionRetries.clear()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            clearState()
        } else {
            mainHandler.post(clearState)
        }
        FileLogger.i(TAG, "Playback monitor stopped")
    }

    private fun scheduleResolve() {
        val executor: ExecutorService
        val runGeneration: Int
        synchronized(resolveLock) {
            if (!registered) return
            if (resolving) {
                rescanRequested = true
                return
            }
            executor = resolveExecutor ?: return
            resolving = true
            runGeneration = generation
        }
        try {
            executor.execute {
                try {
                    while (registered && generation == runGeneration) {
                        resolveFromDumpsys(runGeneration)
                        synchronized(resolveLock) {
                            if (!rescanRequested || !registered || generation != runGeneration) {
                                resolving = false
                                rescanRequested = false
                                return@execute
                            }
                            rescanRequested = false
                        }
                    }
                } finally {
                    synchronized(resolveLock) {
                        resolving = false
                        rescanRequested = false
                    }
                }
            }
        } catch (_: RejectedExecutionException) {
            synchronized(resolveLock) {
                resolving = false
                rescanRequested = false
            }
        }
    }

    private fun resolveFromDumpsys(runGeneration: Int) {
        if (!isSystemPrivileged && !RootShell.isRootAvailable()) {
            FileLogger.w(TAG, "Root not available")
            return
        }

        val discovered =
            if (isSystemPrivileged) {
                queryActivePlayersSys()
            } else {
                queryActivePlayers()
            } ?: return

        mainHandler.post {
            if (!registered || generation != runGeneration) return@post

            val discoveredPiids = discovered.keys
            val discoveredSessionIds = discovered.values.mapTo(HashSet()) { it.sessionId }
            // 已消失的会话清理其失败重试记录，后续重新播放时可重新尝试
            failedSessionRetries.keys.retainAll(discoveredSessionIds)

            val currentPiids = activePiids.keys.toSet()
            val newPiids = discoveredPiids - currentPiids
            val gonePiids = currentPiids - discoveredPiids

            val attemptedFailedSessions = HashSet<Int>()
            var needsRetry = false

            for (piid in newPiids) {
                val info = discovered[piid] ?: continue
                if (info.sessionId <= 0) continue
                val alreadyTracked = activePiids.values.any { it.sessionId == info.sessionId }
                if (!alreadyTracked) {
                    if (info.sessionId in attemptedFailedSessions) continue
                    val prevAttempts = failedSessionRetries[info.sessionId] ?: 0
                    if (prevAttempts > MAX_OPEN_RETRIES) continue

                    FileLogger.i(
                        TAG,
                        "New session piid=$piid session=${info.sessionId} (${info.packageName})",
                    )
                    if (onSessionOpen(info.sessionId, info.packageName)) {
                        activePiids[piid] = info
                        failedSessionRetries.remove(info.sessionId)
                    } else {
                        attemptedFailedSessions.add(info.sessionId)
                        val nextAttempts = prevAttempts + 1
                        failedSessionRetries[info.sessionId] = nextAttempts
                        if (nextAttempts <= MAX_OPEN_RETRIES) {
                            needsRetry = true
                        }
                    }
                } else {
                    activePiids[piid] = info
                    FileLogger.d(
                        TAG,
                        "Additional piid=$piid for existing session=${info.sessionId} (${info.packageName})",
                    )
                }
            }

            for (piid in gonePiids) {
                val info = activePiids.remove(piid) ?: continue
                if (info.sessionId > 0) {
                    val stillActive = activePiids.values.any { it.sessionId == info.sessionId }
                    FileLogger.i(
                        TAG,
                        "Piid ended piid=$piid session=${info.sessionId} (${info.packageName}) stillActive=$stillActive",
                    )
                    if (!stillActive) {
                        onSessionClose(info.sessionId)
                    }
                }
            }

            if (needsRetry) {
                mainHandler.removeCallbacks(retryRunnable)
                mainHandler.postDelayed(retryRunnable, RETRY_DELAY_MS)
            }
        }
    }

    private fun queryActivePlayersSys(): Map<Int, SessionInfo>? =
        try {
            val playerSessions = mutableMapOf<Int, SessionInfo>()
            val configs = audioManager.activePlaybackConfigurations
            val configClass = AudioPlaybackConfiguration::class.java
            // Required when not using system internal sdk
            val isActiveMethod = configClass.getMethod("isActive")
            val getAudioSessionIdMethod = configClass.getMethod("getSessionId")
            val getClientUidMethod = configClass.getMethod("getClientUid")
            val getPlayerInterfaceIdMethod = configClass.getMethod("getPlayerInterfaceId")

            for (config in configs) {
                if (isActiveMethod.invoke(config) as? Boolean != true) continue

                val sid = getAudioSessionIdMethod.invoke(config) as? Int
                if (sid == null || sid <= 0) continue

                // Some player use LL API do not have piid, make sure to fake one from session id
                val piid = (getPlayerInterfaceIdMethod.invoke(config) as Int).let { if (it != 0) it else -sid }

                val uid = getClientUidMethod.invoke(config) as? Int
                if (uid == null || uid < 0) continue
                val pkg = context.packageManager.getPackagesForUid(uid)?.firstOrNull() ?: uid.toString()

                playerSessions[piid] = SessionInfo(sid, pkg)
            }
            playerSessions
        } catch (e: Exception) {
            FileLogger.e(TAG, "Framework active playback query failed", e)
            null
        }

    private fun queryActivePlayers(): Map<Int, SessionInfo>? =
        try {
            val process =
                RootShell.exec(
                    "dumpsys audio | grep -E 'state:started|new player'",
                    timeoutSec = DUMPSYS_TIMEOUT_SEC,
                )
            val text = process.inputStream.bufferedReader().readText()

            val playerSessions = mutableMapOf<Int, SessionInfo>()
            for (line in text.lineSequence()) {
                val trimmed = line.trim()
                val piid =
                    PIID_PATTERN
                        .find(trimmed)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull() ?: continue
                val sid =
                    SESSION_PATTERN
                        .find(trimmed)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull() ?: 0
                val pkg = PACKAGE_PATTERN.find(trimmed)?.groupValues?.get(1) ?: ""
                if (sid > 0 && (piid !in playerSessions || pkg.isNotEmpty())) {
                    playerSessions[piid] = SessionInfo(sid, pkg)
                }
            }

            val activePiidSet = mutableSetOf<Int>()
            for (line in text.lineSequence()) {
                if (!line.contains("state:started")) continue
                val piid =
                    PIID_PATTERN
                        .find(line)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull() ?: continue
                activePiidSet.add(piid)
            }

            val result = mutableMapOf<Int, SessionInfo>()
            for (piid in activePiidSet) {
                val info = playerSessions[piid] ?: continue
                result[piid] = info
            }
            result
        } catch (e: Exception) {
            FileLogger.e(TAG, "dumpsys query failed", e)
            null
        }

    companion object {
        private const val TAG = "SessionMonitor"
        private const val DUMPSYS_TIMEOUT_SEC = 5L
        private const val MAX_OPEN_RETRIES = 3
        private const val RETRY_DELAY_MS = 600L

        private val PIID_PATTERN = Regex("""piid:(\d+)""")
        private val SESSION_PATTERN = Regex("""session(?:Id)?:(\d+)""")
        private val PACKAGE_PATTERN = Regex("""package:(\S+)""")
    }
}
