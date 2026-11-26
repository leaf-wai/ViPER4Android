package com.llsl.viper4android.ui.screens.main

import androidx.compose.runtime.Stable
import com.llsl.viper4android.effect.EffectPref
import com.llsl.viper4android.effect.ListPref
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 音效参数操作接口：将音效列表与主界面从 Hilt [MainViewModel] 解耦，
 * 使 Compose Preview 与纯 UI 测试无需实例化 ViewModel 即可渲染完整界面。
 */
@Stable
interface EffectActions {
    val vdcFileList: StateFlow<List<String>>
    val kernelFileList: StateFlow<List<String>>

    fun <T> applyPref(pref: EffectPref<T>, value: T)

    fun <E> applyBandPref(pref: ListPref<E>, band: Int, value: E, count: Int = 5)

    fun setMasterEnabled(enabled: Boolean)

    fun setConvolverKernel(fileName: String)

    fun setDdcDevice(name: String)

    fun setEqPreset(presetId: Long)

    fun setEqBands(bands: List<Double>)

    fun setEqBandCount(count: Int)

    fun addEqPreset(name: String)

    fun deleteEqPreset(presetId: Long)

    fun resetEqBands()

    fun setDynamicSystemXLow(value: Int)

    fun setDynamicSystemXHigh(value: Int)

    fun setDynamicSystemYLow(value: Int)

    fun setDynamicSystemYHigh(value: Int)

    fun setDynamicSystemSideGainLow(value: Float)

    fun setDynamicSystemSideGainHigh(value: Float)

    fun setDynamicSystemPreset(presetId: Long)

    fun addDynamicSystemPreset(name: String)

    fun deleteDynamicSystemPreset(presetId: Long)

    fun resetDynamicSystemCoefficients()

    fun addDynamicEqBand()

    fun removeDynamicEqBand(index: Int)

    fun setPlaybackGainControlEnabled(enabled: Boolean)

    fun setLufsEnabled(enabled: Boolean)

    fun setFetCompressorEnabled(enabled: Boolean)

    fun setMultibandCompressorEnabled(enabled: Boolean)

    fun setDdcEnabled(enabled: Boolean)

    fun setSpectrumExtensionEnabled(enabled: Boolean)

    fun setEqEnabled(enabled: Boolean)

    fun setDynamicEqEnabled(enabled: Boolean)

    fun setConvolverEnabled(enabled: Boolean)

    fun setFieldSurroundEnabled(enabled: Boolean)

    fun setDiffSurroundEnabled(enabled: Boolean)

    fun setStereoImagerEnabled(enabled: Boolean)

    fun setHeadphoneSurroundEnabled(enabled: Boolean)

    fun setReverbEnabled(enabled: Boolean)

    fun setDynamicSystemEnabled(enabled: Boolean)

    fun setTubeSimulatorEnabled(enabled: Boolean)

    fun setPsychoacousticBassEnabled(enabled: Boolean)

    fun setBassEnabled(enabled: Boolean)

    fun setBassMonoEnabled(enabled: Boolean)

    fun setClarityEnabled(enabled: Boolean)

    fun setCureEnabled(enabled: Boolean)

    fun setAnalogXEnabled(enabled: Boolean)

    fun setSpeakerCorrectionEnabled(enabled: Boolean)

    fun deleteKernelFile(fileName: String): Boolean
}

/** 供 Compose Preview 使用的空操作实现 */
internal object PreviewEffectActions : EffectActions {
    override val vdcFileList: StateFlow<List<String>> =
        MutableStateFlow(listOf("Sony_WH-1000XM5.vdc", "Sennheiser_HD650.vdc"))
    override val kernelFileList: StateFlow<List<String>> =
        MutableStateFlow(listOf("Dolby_Atmos.irs", "Studio_Hall.wav"))

    override fun <T> applyPref(pref: EffectPref<T>, value: T) = Unit

    override fun <E> applyBandPref(pref: ListPref<E>, band: Int, value: E, count: Int) = Unit

    override fun setMasterEnabled(enabled: Boolean) = Unit

    override fun setConvolverKernel(fileName: String) = Unit

    override fun setDdcDevice(name: String) = Unit

    override fun setEqPreset(presetId: Long) = Unit

    override fun setEqBands(bands: List<Double>) = Unit

    override fun setEqBandCount(count: Int) = Unit

    override fun addEqPreset(name: String) = Unit

    override fun deleteEqPreset(presetId: Long) = Unit

    override fun resetEqBands() = Unit

    override fun setDynamicSystemXLow(value: Int) = Unit

    override fun setDynamicSystemXHigh(value: Int) = Unit

    override fun setDynamicSystemYLow(value: Int) = Unit

    override fun setDynamicSystemYHigh(value: Int) = Unit

    override fun setDynamicSystemSideGainLow(value: Float) = Unit

    override fun setDynamicSystemSideGainHigh(value: Float) = Unit

    override fun setDynamicSystemPreset(presetId: Long) = Unit

    override fun addDynamicSystemPreset(name: String) = Unit

    override fun deleteDynamicSystemPreset(presetId: Long) = Unit

    override fun resetDynamicSystemCoefficients() = Unit

    override fun addDynamicEqBand() = Unit

    override fun removeDynamicEqBand(index: Int) = Unit

    override fun setPlaybackGainControlEnabled(enabled: Boolean) = Unit

    override fun setLufsEnabled(enabled: Boolean) = Unit

    override fun setFetCompressorEnabled(enabled: Boolean) = Unit

    override fun setMultibandCompressorEnabled(enabled: Boolean) = Unit

    override fun setDdcEnabled(enabled: Boolean) = Unit

    override fun setSpectrumExtensionEnabled(enabled: Boolean) = Unit

    override fun setEqEnabled(enabled: Boolean) = Unit

    override fun setDynamicEqEnabled(enabled: Boolean) = Unit

    override fun setConvolverEnabled(enabled: Boolean) = Unit

    override fun setFieldSurroundEnabled(enabled: Boolean) = Unit

    override fun setDiffSurroundEnabled(enabled: Boolean) = Unit

    override fun setStereoImagerEnabled(enabled: Boolean) = Unit

    override fun setHeadphoneSurroundEnabled(enabled: Boolean) = Unit

    override fun setReverbEnabled(enabled: Boolean) = Unit

    override fun setDynamicSystemEnabled(enabled: Boolean) = Unit

    override fun setTubeSimulatorEnabled(enabled: Boolean) = Unit

    override fun setPsychoacousticBassEnabled(enabled: Boolean) = Unit

    override fun setBassEnabled(enabled: Boolean) = Unit

    override fun setBassMonoEnabled(enabled: Boolean) = Unit

    override fun setClarityEnabled(enabled: Boolean) = Unit

    override fun setCureEnabled(enabled: Boolean) = Unit

    override fun setAnalogXEnabled(enabled: Boolean) = Unit

    override fun setSpeakerCorrectionEnabled(enabled: Boolean) = Unit

    override fun deleteKernelFile(fileName: String): Boolean = false
}
