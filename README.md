# Sleep Saver - Context-Aware Sleep Companion

Sleep Saver infers bedtime automatically using on-device context signals and logs nightly sleep behavior.

## Implemented in this increment

- MVVM architecture with a single `SleepViewModel` coordinating app state.
- Sensor fusion inputs:
  - Ambient light (`TYPE_LIGHT`)
  - Proximity (`TYPE_PROXIMITY` as a face-down proxy)
  - Charging state (`ACTION_BATTERY_CHANGED`)
  - Sleep window time check
- Sleep inference engine:
  - Enter sleep when low light + face down + charging + in sleep window.
  - Exit sleep when outside window or charging/light conditions indicate wake state.
- Disturbance tracking:
  - Accelerometer spike events (nighttime) and unlock (`ACTION_USER_PRESENT`) are treated as disturbances.
- Persistence:
  - Room database for `SleepSession` and `DisturbanceEvent` records.
  - DataStore Preferences for user settings and latest context snapshot.
- Background work:
  - `WorkManager` periodic task every 15 minutes to re-check sleep inference if app is closed.
- Compose UI screens:
  - Dashboard
  - Context Logic
  - Privacy
  - Settings
- Adaptive layout:
  - Single column phone layout
  - Two-pane tablet-like layout on wider screens
- Custom calm theme:
  - Indigo/lavender palette
  - Rounded card shapes
  - Manual dark mode toggle

## Privacy notes

- No microphone recording.
- No location tracking.
- Light sensor reports lux values only (not images).
- Proximity is near/far only and does not identify users.
- Data stays local in Room/DataStore.
- History can be deleted from the Privacy screen.

## Build and test

```powershell
cd "C:\Users\jjmur\Documents\Programming\CS-4518\Sleep-Saver"
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
```

## Professor quick test flow

1. Launch app on emulator/device.
2. Set system time around 11:00 PM.
3. Ensure charging is active (USB connected).
4. Put phone face down / near proximity condition.
5. Reduce ambient light (or simulate low lux).
6. Wait for immediate in-app checks (or up to 15 minutes for WorkManager).
7. Verify Dashboard changes to `Sleep Mode Active` and shows sleep start.
8. Trigger disturbance between 12:00 AM and 6:00 AM by motion/unlock.
9. Verify disturbance count increases.

## Notes

- DND auto-toggle requires notification policy access on device.
- `android.disallowKotlinSourceSets=false` is set in `gradle.properties` to allow KSP integration with the current AGP built-in Kotlin setup.

