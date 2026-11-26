# ViPER4Android

Miuix UI redesign fork of ViPER4Android FX. Full feature set of the ViPER4Android DSP engine with a HyperOS-inspired Jetpack Compose interface and enhanced audio management features.

## Features

### Audio Effects

- Output Volume / Channel Pan / Limiter
- Playback Gain Control (AGC)
- LUFS Targeting
- Multiband Compressor (5-band)
- FET Compressor

- ViPER Bass (Natural / Pure Bass+ / Subwoofer)
- ViPER Bass Mono (original v0.5.0 algorithm)
- Psychoacoustic Bass Enhancement
- ViPER Clarity (Natural / OZone+ / XHiFi)
- Tube Simulator
- AnalogX

- ViPER-DDC (Digital Device Correction)
- Spectrum Extension (VSE)
- IIR Equalizer (10 / 15 / 25 / 31 bands)
- Dynamic EQ (up to 10 bands, per-band threshold/attack/release)

- Convolver (IRS impulse response loading)
- Field Surround (Colorful Music)
- Differential Surround
- Stereo Imager (3-band width control)
- Headphone Surround+ (VHE)
- Reverberation
- Dynamic System (headphone virtualization)

- Auditory System Protection (Cure crossfeed)
- Speaker Optimization (speaker-only)

---

### App Features

- **Miuix UI Redesign**: HyperOS-inspired Jetpack Compose UI with automatic light/dark theming, squircle styling, frosted glass navigation bars, predictive back transitions, and adaptive wide-screen layouts (collapsible `NavigationRail` and centered content on tablets/foldables)
- **3-Tab Layout (Overview / Effects / Settings)**:
  - **Overview**: Real-time driver status card (working/standby status, version, AIDL/Legacy & Global/Session mode, root permission diagnostics), master toggle, active device shortcut, active sessions entry, and quick-jump list of enabled effects
  - **Effects**: Collapsible effect cards with expand/collapse all toggle, dedicated full-screen Equalizer editor, and long-press documentation for each audio effect
  - **Settings**: Auto-start on boot, persistent notification, Global/Session mode switching, excluded apps, debug mode, imported file managers, and About page
- **Audio Session Management (Per-App Mode)**: Inspect active per-app audio streams with app icons, session IDs, and audio usage tags; toggle processing per session, batch enable/disable via multi-select, or exclude apps directly
- **Persistent Notification**: Optional status bar notification showing real-time driver status, active mode/session count, and a quick Turn On / Turn Off action button
- **Excluded Apps Management**: Searchable app list with system app filtering and excluded apps pinned to the top
- **Per-Device Profiles & Presets**: Dedicated management screens with automatic device profile switching, v2.1 JSON preset import, single/batch preset export to any directory via the system file picker, rename/update, and multi-select batch deletion
- **Kernel & VDC File Management**: Dedicated managers for imported Convolver (`.wav` / `.irs`) and ViPER-DDC (`.vdc`) files with one-tap apply, rename, and multi-select batch deletion
- **AIDL & Legacy HAL Support**: Automatic detection of AIDL and legacy (non-AIDL) audio HALs, supporting both Global mode and Per-App Session mode (with automatic fallback to Global mode when session permissions are unavailable)
- **Debug Log Viewer**: Toggle `Debug mode` in Settings to access live App & Driver logs with source/level filtering, search, and export
- **About Screen & In-App Updater**: Dynamic shader hero header, GitHub Releases update checker with in-app APK download/install, repository links, and open-source licenses

## Installation

1. Download the latest APK from the [Releases](https://github.com/leaf-wai/ViPER4Android/releases)
2. Install the APK
3. Flash the Magisk module from [ViPERFX_RE](https://github.com/likelikeslike/ViPERFX_RE) (or the AIDL variant)
4. Reboot
5. Open the app and tune the settings.
6. Enjoy

## Q&A

- **What is Global Mode?**

    > Creates a single AudioEffect on session ID 0 (the global mix). The Android audio framework
    > routes all audio through session 0, so one effect instance processes everything. This is the
    > simplest and most compatible mode.

- **What is Per-App Mode?**

    > Creates a separate AudioEffect for each audio player's session ID. The DSP processes each
    > app's audio independently. Requires **either root or installation as a privileged system app**
    > (see below).

- **Why does Per-App Mode require root (or a system-app install)?**

    > On modern Android (API 34+ / Android 14+), the framework no longer broadcasts
    > `OPEN_AUDIO_EFFECT_CONTROL_SESSION`, and `AudioManager.getActivePlaybackConfigurations()`
    > returns anonymized session IDs (`sessionId:0, u/pid:-1/-1`) to ordinary apps. The app uses
    > `AudioPlaybackCallback` to detect playback state changes, then needs a privileged path to
    > recover the real session ID. Two paths are supported:
    >
    > - **Root (default):** the real session ID is read via `su -c "dumpsys audio"`.
    > - **Privileged system app (no root):** if the app is installed under `/system/priv-app`
    >   and granted the `MODIFY_AUDIO_ROUTING` permission, the framework returns the real
    >   (non-anonymized) `sessionId`/`clientUid` directly from `getActivePlaybackConfigurations()`,
    >   so no root and no `dumpsys` shell-out is needed. The app auto-detects this at runtime
    >   (the permission grant is the trigger) and prefers it over the root path.

- **How do I install as a privileged system app (per-app mode without root)?**

    > Place the APK in `/system/priv-app/ViPER4Android/` and add a privileged-permission
    > allowlist so the OS grants `MODIFY_AUDIO_ROUTING`:
    >
    > ```xml
    > <!-- /system/etc/permissions/privapp-permissions-viper4android.xml -->
    > <?xml version="1.0" encoding="utf-8"?>
    > <permissions>
    >     <privapp-permissions package="com.leaf.viper4android">
    >         <permission name="android.permission.MODIFY_AUDIO_ROUTING"/>
    >     </privapp-permissions>
    > </permissions>
    > ```
    >
    > This is done for you when the app ships inside a ROM or a Magisk module that mounts it
    > systemlessly. Without the allowlist entry the permission is silently **denied**
    > (with `ro.control_privapp_permissions=enforce`, the default), and the app falls back to the
    > root path. No platform signature is required, priv-app placement satisfies the `privileged`
    > half of the `signature|privileged` protection level.
    >
    > Note that you may need allow the following SElinux rules for the app (as system app) to work:
    >
    > ```sh
    > allow priv_app servicemanager service_manager list
    > allow priv_app viper_control_service service_manager find
    > allow priv_app default_android_service service_manager find
    > allow priv_app hal_audio_default binder { call transfer }
    > ```

- **What is Per-Device Profile?**

    > Each audio device (e.g., "Galaxy Buds Pro", "Airpods", "Speaker") keeps its own saved effect
    > settings, loaded automatically on connect and saved when the app goes to the background. See [Per-Device Profiles](#per-device-profiles) for details.

- **What is AIDL Mode?**
    > Android 14+ introduced a new Audio HAL interface based on AIDL (Android Interface Definition Language),
    > replacing the legacy HIDL interface. The App automatically check which HAL your device using.

## Presets

Presets are stored in the **v2 grouped-JSON format** (`schemaVersion: 2.1`):

> v2.1 stores the raw semantic values that send to the DSP

```json
{
  "schemaVersion": 2.1,
  "name": "My Preset",
  "equalizer": { "enable": true, "bandCount": 10, "bands": [3.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 3.0], "presetId": null },
  "dynamicEq": { "enable": false, "bandCount": 4, "freqs": [...], "gains": [...] },
  "ddc": { "enable": false, "device": "" }
}
```

### Migrating v1 / legacy presets to v2 and v2.1

v2 is **not** compatible with the old flat v1 JSON or the legacy ViPER4Android XML presets. Two migration paths are available:

1. **In-app export (recommended).** In the last v1 app release (1.5.5), use **Export preset as v2** to produce a `.v2.json` file, then import it in v2.
2. **Command-line tool.** For v1 JSON or legacy XML presets, use [`tools/convert_preset.py`](tools/convert_preset.py) (Python 3.11+):

    ```bash
    # v1 flat JSON  ->  v2.1
    python3 tools/convert_preset.py preset.json --to 2.1 -o preset.v2.json
    # legacy ViPER4Android XML  ->  v2.1
    python3 tools/convert_preset.py preset.xml --to 2.1 -o preset.v2.json
    # v2 -> v2.1
    python3 tools/convert_preset.py preset.v2.json -o preset.v2_1.json
    ```

The input format (v1 JSON vs. XML) and, for v1 JSON, the headphone/speaker namespace are auto-detected.
Missing fields are filled with the app defaults. Import the resulting `.v2.1.json` in the app.

> [!NOTE]
> v2 no longer stores separate headphone and speaker copies inside a preset. A preset now holds a
> single unified effect state and is applied to a device through the per-device profile system.

## Per-Device Profiles

Each audio output device keeps its **own** full effect profile. When you switch outputs, the app
automatically loads the incoming device's profile, so your speaker, wired earphones, and each pair
of Bluetooth buds all remember their own tuning.

### How a device is identified

The active output is detected from the Android audio routing API, and each device gets a stable `deviceId`:

- **Speaker** → `speaker`
- **Wired headphone / headset** → `wired_headphone`
- **Bluetooth** (A2DP / BLE / hearing aid) → the device's **MAC address** (falls back to `bt_<id>` if unavailable)
- **USB DAC / headset** → the USB device **address** (falls back to `usb_<id>`)

Because Bluetooth and USB devices are keyed by their hardware address, two different pairs of buds
are stored separately, and reconnecting the same device restores exactly its profile. The display
name comes from the device's reported product name (e.g. "Galaxy Buds Pro"), with a type-based fallback ("Bluetooth A2DP", "USB Audio", ...).

### When profiles are saved and loaded

- **On output switch**: the app detects the new active device, **loads** its profile.
- **On app background** (home button, app switch / `ON_STOP`) and **on shutdown**: the current settings are saved to the active device's profile.
- **First time a device is seen**: a profile is created automatically from the current settings.

### Managing profiles

Open **Active Device** on the Overview tab to access the **Audio Devices** screen. Tap a device to **load** its profile into the current settings, or long-press / enter multi-select mode to **update** a device with current settings, **rename** a device, or **delete** profiles in batch. Deleting a profile removes its saved settings (built-in and active devices are protected); the device gets a fresh profile the next time it connects.

## Root Access

This app may require root access for:

- **Per-App Mode**: Retrieving real audio session IDs via `dumpsys` (not needed when installed as a privileged system app, see Per-App Mode in the Q&A)
- **Debug log viewer**: Reading `logcat` for driver diagnostics

Please make sure the source of any modified APK is trustworthy to avoid any security risks.

## Contributing

Contributions are welcome. Open an issue or submit a pull request.

### Localization

To help with translations, follow the [template guide](app/res-template/values-template/strings.xml).

- RU: [@maurerdv](https://github.com/maurerdv)
- ZH-CN: [@Arissekai](https://github.com/Arissekai)
