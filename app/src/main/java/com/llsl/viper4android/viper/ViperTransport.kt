package com.llsl.viper4android.viper

import com.llsl.viper4android.effect.EffectState
import com.llsl.viper4android.utils.FileLogger
import java.util.concurrent.ConcurrentHashMap

sealed interface ParamValue {
    val index: Int

    data class Bool(
        val v: Boolean,
        override val index: Int = -1,
    ) : ParamValue

    data class IntV(
        val v: Int,
        override val index: Int = -1,
    ) : ParamValue

    data class FloatV(
        val v: Float,
        override val index: Int = -1,
    ) : ParamValue

    data class Ints(
        val v: IntArray,
        override val index: Int = -1,
    ) : ParamValue {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Ints

            if (index != other.index) return false
            if (!v.contentEquals(other.v)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + v.contentHashCode()
            return result
        }
    }

    data class Floats(
        val v: FloatArray,
        override val index: Int = -1,
    ) : ParamValue {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Floats

            if (index != other.index) return false
            if (!v.contentEquals(other.v)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + v.contentHashCode()
            return result
        }
    }

    data class Bytes(
        val v: ByteArray,
        override val index: Int = -1,
    ) : ParamValue {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Bytes

            if (index != other.index) return false
            if (!v.contentEquals(other.v)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + v.contentHashCode()
            return result
        }
    }
}

enum class BulkKind {
    CONVOLVER,
    DDC,
}

data class DriverStatus(
    val enabled: Boolean,
    val sampleRate: Int,
    val processedFrames: Long,
    val kernelId: Int,
    val versionCode: Int,
    val versionName: String,
    val arch: String,
)

data class BulkApplyResult(
    val ddcApplied: Boolean = true,
)

typealias BulkSideChannelHandler =
    suspend (
        ddcDevice: String,
        convolverKernel: String,
        clearDdc: Boolean,
        clearConvolver: Boolean,
        force: Boolean,
        skipDrainDelay: Boolean,
        send: (Int, ParamValue) -> Boolean,
    ) -> BulkApplyResult

interface ViperTransport {
    var bulkSideChannels: BulkSideChannelHandler?

    fun set(
        param: Int,
        value: ParamValue,
    ): Boolean

    suspend fun pushFullState(
        state: EffectState,
        forceBulk: Boolean = false,
        targetEffect: ViperEffect? = null,
    )

    fun probeStatus(): DriverStatus?

    fun shouldStream(
        kind: BulkKind,
        key: String,
        force: Boolean,
    ): Boolean

    fun markStreamed(
        kind: BulkKind,
        key: String,
    )

    fun clearBulkCache()
}

private suspend fun dispatchBulkAndBuildEffectiveState(
    state: EffectState,
    forceBulk: Boolean,
    skipDrainDelay: Boolean,
    bulkSideChannels: BulkSideChannelHandler?,
    send: (Int, ParamValue) -> Boolean,
): EffectState {
    val wantDdc = state.ddc.enable && state.ddc.device.isNotEmpty()
    val ddcDevice = if (wantDdc) state.ddc.device else ""
    val clearDdc = state.ddc.device.isEmpty()

    val wantConvolver = state.convolver.enable && state.convolver.kernelFile.isNotEmpty()
    val convolverKernel = if (wantConvolver) state.convolver.kernelFile else ""
    val clearConvolver = state.convolver.kernelFile.isEmpty()

    val bulkResult =
        if (ddcDevice.isNotEmpty() || convolverKernel.isNotEmpty() || clearDdc || clearConvolver) {
            bulkSideChannels?.invoke(
                ddcDevice,
                convolverKernel,
                clearDdc,
                clearConvolver,
                forceBulk,
                skipDrainDelay,
                send,
            ) ?: BulkApplyResult()
        } else {
            BulkApplyResult()
        }

    var effectiveState = state
    if (effectiveState.ddc.enable && (!wantDdc || !bulkResult.ddcApplied)) {
        effectiveState = effectiveState.copy(ddc = effectiveState.ddc.copy(enable = false))
    }
    if (effectiveState.convolver.enable && !wantConvolver) {
        effectiveState = effectiveState.copy(convolver = effectiveState.convolver.copy(enable = false))
    }
    return effectiveState
}

class AidlTransport : ViperTransport {
    override var bulkSideChannels: BulkSideChannelHandler? = null

    private val bulkKeys = ConcurrentHashMap<BulkKind, String>()

    init {
        ViperControlClient.onBinderDied = { clearBulkCache() }
    }

    override fun set(
        param: Int,
        value: ParamValue,
    ): Boolean =
        when (value) {
            is ParamValue.Bool -> {
                if (value.index != -1) {
                    ViperControlClient.dispatchParam(param, value.index, value.v)
                } else {
                    ViperControlClient.dispatchParam(param, value.v)
                }
            }

            is ParamValue.IntV -> {
                if (value.index != -1) {
                    ViperControlClient.dispatchParam(param, value.index, value.v)
                } else {
                    ViperControlClient.dispatchParam(param, value.v)
                }
            }

            is ParamValue.FloatV -> {
                if (value.index != -1) {
                    ViperControlClient.dispatchParam(param, value.index, value.v)
                } else {
                    ViperControlClient.dispatchParam(param, value.v)
                }
            }

            is ParamValue.Ints -> {
                if (value.v.size == 3) {
                    ViperControlClient.dispatchParam(param, value.v[0], value.v[1], value.v[2])
                } else {
                    FileLogger.w("AidlTransport", "Dropping Ints param $param: size ${value.v.size} != 3")
                    false
                }
            }

            is ParamValue.Floats -> {
                ViperControlClient.dispatchParam(param, value.v)
            }

            is ParamValue.Bytes -> {
                ViperControlClient.dispatchParam(param, value.v)
            }
        }

    override suspend fun pushFullState(
        state: EffectState,
        forceBulk: Boolean,
        targetEffect: ViperEffect?,
    ) {
        val effectiveState =
            dispatchBulkAndBuildEffectiveState(
                state = state,
                forceBulk = forceBulk,
                skipDrainDelay = false,
                bulkSideChannels = bulkSideChannels,
                send = ::set,
            )
        val snapshotApplied =
            ViperControlClient.dispatchParam(
                ViperParams.PARAM_SET_FULL_PARAMS,
                ViperParamsSerializer.toByteArray(effectiveState),
            )
        if (!snapshotApplied) {
            FileLogger.w("AidlTransport", "Full snapshot dispatch failed")
        }
    }

    override fun probeStatus(): DriverStatus? = ViperControlClient.getStatus()

    override fun shouldStream(
        kind: BulkKind,
        key: String,
        force: Boolean,
    ): Boolean = force || bulkKeys[kind] != key

    override fun markStreamed(
        kind: BulkKind,
        key: String,
    ) {
        bulkKeys[kind] = key
    }

    override fun clearBulkCache() {
        bulkKeys.clear()
    }
}

class HidlTransport(
    private val effects: () -> List<ViperEffect>,
    private val activeEffect: () -> ViperEffect? = { null },
) : ViperTransport {
    override var bulkSideChannels: BulkSideChannelHandler? = null

    private val bulkKeys = ConcurrentHashMap<BulkKind, String>()

    override fun set(
        param: Int,
        value: ParamValue,
    ): Boolean {
        val list = effects()
        if (list.isEmpty()) return false
        var allOk = true
        for (fx in list) {
            if (!fx.setParameter(param, value)) {
                allOk = false
            }
        }
        return allOk
    }

    override suspend fun pushFullState(
        state: EffectState,
        forceBulk: Boolean,
        targetEffect: ViperEffect?,
    ) {
        val targets = if (targetEffect != null) listOf(targetEffect) else effects()
        if (targets.isEmpty()) return

        val send: (Int, ParamValue) -> Boolean =
            if (targetEffect != null) {
                { param, value -> targetEffect.setParameter(param, value) }
            } else {
                ::set
            }
        val effectiveState =
            dispatchBulkAndBuildEffectiveState(
                state = state,
                forceBulk = forceBulk || targetEffect != null,
                skipDrainDelay = targetEffect != null,
                bulkSideChannels = bulkSideChannels,
                send = send,
            )
        val snapshot = ParamValue.Bytes(ViperParamsSerializer.toByteArray(effectiveState))
        targets.forEach {
            val snapshotApplied = it.setParameter(ViperParams.PARAM_SET_FULL_PARAMS, snapshot)
            if (!snapshotApplied) {
                FileLogger.w("HidlTransport", "Full snapshot dispatch failed")
            }
        }
    }

    override fun probeStatus(): DriverStatus? {
        val active = activeEffect()
        if (active != null && active.isCreated) return active.getStatus()
        val probe = ViperEffect(0, ViperEffect.EFFECT_TYPE_UUID)
        if (!probe.create()) {
            probe.release()
            return null
        }
        val status = probe.getStatus()
        probe.release()
        return status
    }

    override fun shouldStream(
        kind: BulkKind,
        key: String,
        force: Boolean,
    ): Boolean = force || bulkKeys[kind] != key

    override fun markStreamed(
        kind: BulkKind,
        key: String,
    ) {
        bulkKeys[kind] = key
    }

    override fun clearBulkCache() {
        bulkKeys.clear()
    }
}
