package com.llsl.viper4android.audio

import android.media.AudioAttributes
import androidx.compose.runtime.Immutable

/**
 * 会话模式下正在追踪的单个音频流会话信息。
 *
 * @property sessionId Android AudioFlinger 分配的音频会话 ID
 * @property packageName 音频流所属应用包名（部分底层流可能为空或 UID 字符串）
 * @property enabled 当前是否启用 ViPER 音效处理（用户可单独关闭或恢复）
 * @property openedAt 首次捕获该会话的时间戳（毫秒）
 * @property usage 音频用途标识（[AudioAttributes.USAGE_MEDIA] 等）
 */
@Immutable
data class AudioSessionInfo(
    val sessionId: Int,
    val packageName: String,
    val enabled: Boolean,
    val openedAt: Long,
    val usage: Int = AudioAttributes.USAGE_UNKNOWN,
)
