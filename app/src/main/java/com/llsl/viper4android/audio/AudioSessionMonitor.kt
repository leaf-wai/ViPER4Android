package com.llsl.viper4android.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
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
    private val onSessionOpen: (sessionId: Int, packageName: String, usage: Int) -> Boolean,
    private val onSessionClose: (sessionId: Int) -> Unit,
    private val onSessionUpdate: (sessionId: Int, packageName: String, usage: Int) -> Unit = { _, _, _ -> },
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activePiids = mutableMapOf<Int, SessionInfo>()
    private val failedSessionRetries = mutableMapOf<Int, Int>()
    private val resolveLock = Any()
    private val pendingRefreshCallbacks = mutableListOf<() -> Unit>()

    @Volatile
    private var registered = false

    @Volatile
    private var generation = 0
    private var resolving = false
    private var rescanRequested = false
    private var unresolvedRetryCount = 0
    private var resolveExecutor: ExecutorService? = null

    private val retryRunnable =
        Runnable {
            if (registered) {
                scheduleResolve()
            }
        }

    private val unresolvedRetryRunnable =
        Runnable {
            if (registered) {
                scheduleResolve()
            }
        }

    private val isSystemPrivileged: Boolean by lazy {
        isSystemPrivileged(context)
    }

    private data class SessionInfo(
        val sessionId: Int,
        val packageName: String,
        val usage: Int = AudioAttributes.USAGE_UNKNOWN,
        val uid: Int = -1,
        val pid: Int = -1,
    )

    private val playbackCallback =
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                unresolvedRetryCount = 0
                mainHandler.removeCallbacks(retryRunnable)
                mainHandler.removeCallbacks(unresolvedRetryRunnable)
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
        mainHandler.post { unresolvedRetryCount = 0 }
        scheduleResolve()
    }

    fun stop() {
        val executorToShutdown: ExecutorService?
        val orphanedCallbacks: List<() -> Unit>
        synchronized(resolveLock) {
            if (!registered) return
            registered = false
            generation++
            rescanRequested = false
            executorToShutdown = resolveExecutor
            resolveExecutor = null
            orphanedCallbacks = pendingRefreshCallbacks.toList().also { pendingRefreshCallbacks.clear() }
        }
        audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        mainHandler.removeCallbacks(retryRunnable)
        mainHandler.removeCallbacks(unresolvedRetryRunnable)
        executorToShutdown?.shutdownNow()
        val clearState = {
            activePiids.clear()
            failedSessionRetries.clear()
            unresolvedRetryCount = 0
            orphanedCallbacks.forEach { it() }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            clearState()
        } else {
            mainHandler.post(clearState)
        }
        FileLogger.i(TAG, "Playback monitor stopped")
    }

    fun refresh(onComplete: (() -> Unit)? = null) {
        if (!registered) {
            onComplete?.let { mainHandler.post(it) }
            return
        }
        if (onComplete != null) {
            synchronized(resolveLock) {
                pendingRefreshCallbacks.add(onComplete)
            }
        }
        mainHandler.removeCallbacks(retryRunnable)
        mainHandler.removeCallbacks(unresolvedRetryRunnable)
        val resetRetries = {
            failedSessionRetries.clear()
            unresolvedRetryCount = 0
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            resetRetries()
        } else {
            mainHandler.post(resetRetries)
        }
        scheduleResolve()
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
                        val callbacksForRun =
                            synchronized(resolveLock) {
                                pendingRefreshCallbacks.toList().also { pendingRefreshCallbacks.clear() }
                            }
                        resolveFromDumpsys(runGeneration)
                        if (callbacksForRun.isNotEmpty()) {
                            // resolveFromDumpsys() 内已先 post 状态更新到 mainHandler，FIFO 保证回调在更新之后执行
                            mainHandler.post { callbacksForRun.forEach { it() } }
                        }
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
                    val remainingCallbacks =
                        synchronized(resolveLock) {
                            resolving = false
                            rescanRequested = false
                            pendingRefreshCallbacks.toList().also { pendingRefreshCallbacks.clear() }
                        }
                    if (remainingCallbacks.isNotEmpty()) {
                        mainHandler.post { remainingCallbacks.forEach { it() } }
                    }
                }
            }
        } catch (_: RejectedExecutionException) {
            val remainingCallbacks =
                synchronized(resolveLock) {
                    resolving = false
                    rescanRequested = false
                    pendingRefreshCallbacks.toList().also { pendingRefreshCallbacks.clear() }
                }
            if (remainingCallbacks.isNotEmpty()) {
                mainHandler.post { remainingCallbacks.forEach { it() } }
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
            val existingPiids = discoveredPiids.intersect(currentPiids)
            val gonePiids = currentPiids - discoveredPiids

            val attemptedFailedSessions = HashSet<Int>()
            var needsRetry = false

            for (piid in newPiids) {
                val info = discovered[piid] ?: continue
                if (info.sessionId <= 0) continue
                val existingForSession = activePiids.values.filter { it.sessionId == info.sessionId }
                if (existingForSession.isEmpty()) {
                    if (info.sessionId in attemptedFailedSessions) continue
                    val prevAttempts = failedSessionRetries[info.sessionId] ?: 0
                    if (prevAttempts > MAX_OPEN_RETRIES) continue

                    FileLogger.i(
                        TAG,
                        "New session piid=$piid session=${info.sessionId} (${info.packageName}) usage=${info.usage}",
                    )
                    if (onSessionOpen(info.sessionId, info.packageName, info.usage)) {
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
                    // 同会话的新 piid 如果带有更完整的包名或用途，顺便补全会话元数据
                    val hadRealPkg = existingForSession.any { isRealPackageName(it.packageName) }
                    val hadUsage = existingForSession.any { it.usage != AudioAttributes.USAGE_UNKNOWN }
                    if ((!hadRealPkg && isRealPackageName(info.packageName)) ||
                        (!hadUsage && info.usage != AudioAttributes.USAGE_UNKNOWN)
                    ) {
                        onSessionUpdate(info.sessionId, info.packageName, info.usage)
                    }
                }
            }

            // 对已追踪的 piid 检查是否补全了此前缺失的包名或音频用途
            for (piid in existingPiids) {
                val old = activePiids[piid] ?: continue
                val fresh = discovered[piid] ?: continue
                if (fresh.sessionId <= 0) continue
                val mergedPkg =
                    when {
                        isRealPackageName(fresh.packageName) -> fresh.packageName
                        isRealPackageName(old.packageName) -> old.packageName
                        fresh.packageName.isNotEmpty() -> fresh.packageName
                        else -> old.packageName
                    }
                val mergedUsage =
                    if (fresh.usage != AudioAttributes.USAGE_UNKNOWN) fresh.usage else old.usage
                val mergedUid = if (fresh.uid >= 0) fresh.uid else old.uid
                val mergedPid = if (fresh.pid > 0) fresh.pid else old.pid

                if (mergedPkg != old.packageName ||
                    mergedUsage != old.usage ||
                    mergedUid != old.uid ||
                    mergedPid != old.pid
                ) {
                    activePiids[piid] =
                        old.copy(
                            packageName = mergedPkg,
                            usage = mergedUsage,
                            uid = mergedUid,
                            pid = mergedPid,
                        )
                    if (mergedPkg != old.packageName || mergedUsage != old.usage) {
                        FileLogger.i(
                            TAG,
                            "Enriched session=${fresh.sessionId} piid=$piid pkg=$mergedPkg usage=$mergedUsage",
                        )
                        onSessionUpdate(fresh.sessionId, mergedPkg, mergedUsage)
                    }
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

            val hasUnresolvedSession =
                activePiids.values
                    .groupBy { it.sessionId }
                    .any { (sid, infos) -> sid > 0 && infos.none { isRealPackageName(it.packageName) } }

            if (needsRetry) {
                mainHandler.removeCallbacks(unresolvedRetryRunnable)
                mainHandler.removeCallbacks(retryRunnable)
                mainHandler.postDelayed(retryRunnable, RETRY_DELAY_MS)
            } else if (hasUnresolvedSession) {
                if (unresolvedRetryCount < UNRESOLVED_RETRY_DELAYS_MS.size) {
                    val delayMs = UNRESOLVED_RETRY_DELAYS_MS[unresolvedRetryCount++]
                    mainHandler.removeCallbacks(unresolvedRetryRunnable)
                    mainHandler.postDelayed(unresolvedRetryRunnable, delayMs)
                }
            } else {
                unresolvedRetryCount = 0
                mainHandler.removeCallbacks(unresolvedRetryRunnable)
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
                val pkg = resolvePackageFromUid(uid) ?: uid.toString()
                val usage = config.audioAttributes.usage

                playerSessions[piid] = SessionInfo(sid, pkg, usage, uid = uid)
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
                val rawPkg = PACKAGE_PATTERN.find(trimmed)?.groupValues?.get(1) ?: ""
                val pkg = if (isRealPackageName(rawPkg)) rawPkg else ""
                val parsedUsage =
                    USAGE_PATTERN
                        .find(trimmed)
                        ?.groupValues
                        ?.get(1)
                        ?.let(::parseUsageToken)
                        ?: AudioAttributes.USAGE_UNKNOWN
                val uidPidMatch = UID_PID_PATTERN.find(trimmed)
                val uid = uidPidMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
                val pid = uidPidMatch?.groupValues?.get(2)?.toIntOrNull() ?: -1

                val prev = playerSessions[piid]
                val mergedSid = if (sid > 0) sid else (prev?.sessionId ?: 0)
                if (mergedSid > 0 || pkg.isNotEmpty() || uid >= 0 || pid > 0) {
                    playerSessions[piid] =
                        SessionInfo(
                            sessionId = mergedSid,
                            packageName = if (pkg.isNotEmpty()) pkg else (prev?.packageName ?: ""),
                            usage =
                                if (parsedUsage != AudioAttributes.USAGE_UNKNOWN) {
                                    parsedUsage
                                } else {
                                    prev?.usage ?: AudioAttributes.USAGE_UNKNOWN
                                },
                            uid = if (uid >= 0) uid else (prev?.uid ?: -1),
                            pid = if (pid > 0) pid else (prev?.pid ?: -1),
                        )
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
                if (info.sessionId <= 0) continue
                // 第 2 级兜底：当 dumpsys audio 未携带 package: 时，利用同一行的 u/pid 反查包名
                val resolvedPkg =
                    if (isRealPackageName(info.packageName)) {
                        info.packageName
                    } else {
                        resolvePackageFromUidAndPid(info.uid, info.pid)
                    }
                result[piid] = info.copy(packageName = resolvedPkg)
            }

            // 第 3 级兜底：仍有未解析出包名的会话时，查一次 AudioFlinger 活跃 Track 的 pid/uid
            val unresolvedSids =
                result.values
                    .filter { !isRealPackageName(it.packageName) }
                    .mapTo(HashSet()) { it.sessionId }
            if (unresolvedSids.isNotEmpty()) {
                val flingerOwners = queryAudioFlingerOwners(unresolvedSids)
                if (flingerOwners.isNotEmpty()) {
                    for ((piid, info) in result.entries.toList()) {
                        if (isRealPackageName(info.packageName)) continue
                        val owner = flingerOwners[info.sessionId] ?: continue
                        val ownerUid = if (owner.first >= 0) owner.first else info.uid
                        val ownerPid = if (owner.second > 0) owner.second else info.pid
                        val pkg = resolvePackageFromUidAndPid(ownerUid, ownerPid)
                        if (pkg.isNotEmpty()) {
                            result[piid] = info.copy(packageName = pkg, uid = ownerUid, pid = ownerPid)
                        }
                    }
                }
            }

            result
        } catch (e: Exception) {
            FileLogger.e(TAG, "dumpsys query failed", e)
            null
        }

    private fun resolvePackageFromUid(uid: Int): String? {
        if (uid < 0) return null
        val pm = context.packageManager
        pm.getPackagesForUid(uid)?.firstOrNull()?.let { return it }
        // 兼容应用双开 / 工作资料（userId * 100000 + appId）
        if (uid >= PER_USER_RANGE) {
            pm.getPackagesForUid(uid % PER_USER_RANGE)?.firstOrNull()?.let { return it }
        }
        return null
    }

    private fun resolvePackageFromUidAndPid(
        uid: Int,
        pid: Int,
    ): String {
        val pm = context.packageManager
        val directPackages = if (uid >= 0) pm.getPackagesForUid(uid) else null
        if (directPackages != null && directPackages.size == 1) {
            return directPackages[0]
        }

        // 多包共享 UID（如系统 UID 1000）、双开/多用户 UID 或 uid 缺失时，优先从 /proc/<pid>/cmdline 精确定位
        if (pid > 0) {
            readPackageFromPid(pid)?.let { return it }
        }

        if (directPackages != null && directPackages.isNotEmpty()) {
            return directPackages[0]
        }

        if (uid >= PER_USER_RANGE) {
            pm.getPackagesForUid(uid % PER_USER_RANGE)?.firstOrNull()?.let { return it }
        }

        if (uid < 0 && pid > 0) {
            readUidFromPid(pid)?.let { procUid ->
                resolvePackageFromUid(procUid)?.let { return it }
                return procUid.toString()
            }
        }

        return if (uid >= 0) uid.toString() else ""
    }

    private fun readPackageFromPid(pid: Int): String? =
        try {
            val process = RootShell.exec("cat /proc/$pid/cmdline", timeoutSec = PROC_READ_TIMEOUT_SEC)
            val raw = process.inputStream.bufferedReader().readText()
            val firstToken = raw.substringBefore('\u0000').trim()
            val candidate = firstToken.substringBefore(':')
            if (PACKAGE_NAME_PATTERN.matches(candidate)) candidate else null
        } catch (_: Exception) {
            null
        }

    private fun readUidFromPid(pid: Int): Int? =
        try {
            val process = RootShell.exec("grep -m1 '^Uid:' /proc/$pid/status", timeoutSec = PROC_READ_TIMEOUT_SEC)
            val line = process.inputStream.bufferedReader().readText().trim()
            PROC_UID_PATTERN
                .find(line)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        } catch (_: Exception) {
            null
        }

    /**
     * 从 dumpsys media.audio_flinger 提取目标 sessionId 对应的 (uid, pid)。
     */
    private fun queryAudioFlingerOwners(targetSessionIds: Set<Int>): Map<Int, Pair<Int, Int>> =
        try {
            val process =
                RootShell.exec(
                    "dumpsys media.audio_flinger | grep -E 'yes|sessionId|session:'",
                    timeoutSec = FLINGER_TIMEOUT_SEC,
                )
            val text = process.inputStream.bufferedReader().readText()
            val owners = mutableMapOf<Int, Pair<Int, Int>>()

            for (line in text.lineSequence()) {
                // 匹配标准 PlaybackThread 活跃 Track 表行："[F] <id> yes <clientPid> <sessionId> <portId> ..."
                val trackMatch = FLINGER_TRACK_PATTERN.find(line)
                if (trackMatch != null) {
                    val pid = trackMatch.groupValues[1].toIntOrNull() ?: -1
                    val sid = trackMatch.groupValues[2].toIntOrNull() ?: 0
                    if (sid in targetSessionIds && pid > 0) {
                        owners.putIfAbsent(sid, -1 to pid)
                    }
                    continue
                }

                // 匹配带 key=value / key:value 格式的行（如 MmapThread / AAudio）
                val sid =
                    FLINGER_SESSION_PATTERN
                        .find(line)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull() ?: continue
                if (sid !in targetSessionIds) continue
                val uid =
                    FLINGER_UID_PATTERN
                        .find(line)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull() ?: -1
                val pid =
                    FLINGER_PID_PATTERN
                        .find(line)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull() ?: -1
                if (uid >= 0 || pid > 0) {
                    val prev = owners[sid]
                    owners[sid] =
                        (if (uid >= 0) uid else (prev?.first ?: -1)) to
                            (if (pid > 0) pid else (prev?.second ?: -1))
                }
            }
            owners
        } catch (e: Exception) {
            FileLogger.d(TAG, "AudioFlinger fallback query failed: ${e.message}")
            emptyMap()
        }

    private fun isRealPackageName(value: String): Boolean =
        value.isNotEmpty() &&
            !value.all(Char::isDigit) &&
            !value.equals("null", ignoreCase = true) &&
            !value.equals("unknown", ignoreCase = true)

    private fun parseUsageToken(token: String): Int {
        token.toIntOrNull()?.let { return it }
        return when (token) {
            "USAGE_MEDIA" -> AudioAttributes.USAGE_MEDIA
            "USAGE_GAME" -> AudioAttributes.USAGE_GAME
            "USAGE_ASSISTANCE_NAVIGATION_GUIDANCE" -> AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
            "USAGE_VOICE_COMMUNICATION",
            "USAGE_VOICE_COMMUNICATION_SIGNALLING",
            -> AudioAttributes.USAGE_VOICE_COMMUNICATION
            "USAGE_ALARM" -> AudioAttributes.USAGE_ALARM
            "USAGE_NOTIFICATION",
            "USAGE_NOTIFICATION_RINGTONE",
            "USAGE_NOTIFICATION_EVENT",
            -> AudioAttributes.USAGE_NOTIFICATION
            "USAGE_ASSISTANT" -> AudioAttributes.USAGE_ASSISTANT
            else -> AudioAttributes.USAGE_UNKNOWN
        }
    }

    companion object {
        private const val TAG = "SessionMonitor"
        private const val PERMISSION_MODIFY_AUDIO_ROUTING = "android.permission.MODIFY_AUDIO_ROUTING"
        private const val DUMPSYS_TIMEOUT_SEC = 5L
        private const val FLINGER_TIMEOUT_SEC = 3L
        private const val PROC_READ_TIMEOUT_SEC = 2L
        private const val MAX_OPEN_RETRIES = 3
        private const val RETRY_DELAY_MS = 600L
        private const val PER_USER_RANGE = 100_000
        private val UNRESOLVED_RETRY_DELAYS_MS = longArrayOf(1500L, 3000L, 5000L, 8000L)

        private val PIID_PATTERN = Regex("""piid:(\d+)""")
        private val SESSION_PATTERN = Regex("""session(?:Id)?:(\d+)""")
        private val PACKAGE_PATTERN = Regex("""package:(\S+)""")
        private val USAGE_PATTERN = Regex("""usage=(\w+)""")
        private val UID_PID_PATTERN = Regex("""u(?:id)?/pid:(-?\d+)/(-?\d+)""")
        private val PACKAGE_NAME_PATTERN = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$""")
        private val PROC_UID_PATTERN = Regex("""^Uid:\s*(\d+)""")
        private val FLINGER_TRACK_PATTERN = Regex("""^\s*(?:F\s+)?\d+\s+yes\s+(\d+)\s+(\d+)\s+\d+""")
        private val FLINGER_SESSION_PATTERN = Regex("""session(?:Id)?[:=]\s*(\d+)""")
        private val FLINGER_UID_PATTERN = Regex("""uid[:=]\s*(\d+)""")
        private val FLINGER_PID_PATTERN = Regex("""pid[:=]\s*(\d+)""")

        /** 检测是否作为特权系统应用获授 MODIFY_AUDIO_ROUTING（无需 Root 即可使用会话模式） */
        fun isSystemPrivileged(context: Context): Boolean =
            context.checkSelfPermission(PERMISSION_MODIFY_AUDIO_ROUTING) ==
                PackageManager.PERMISSION_GRANTED

        /** 检测当前环境是否具备会话模式权限（特权系统应用或 Root）；涉及 su 探测，需在 IO 线程调用 */
        fun canUseSessionMode(context: Context): Boolean =
            isSystemPrivileged(context) || RootShell.isRootAvailable()
    }
}
