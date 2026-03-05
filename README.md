# Sleep Saver: The Context-Aware Sleep Companion

**Developer:** Jacob Murphy

Sleep Saver infers bedtime automatically using device context signals and logs nightly sleep behavior without requiring a manual "Start Sleep" button.

## Core Concept

The app detects sleep using **sensor fusion** and environmental signals:

1. **Light Sensor** (Hardware #1): Detects dark environment (lights-off behavior)
2. **Proximity Sensor** (Hardware #2): Detects face-down phone placement (intentional sleep position)
3. **Battery Charging State** (Virtual Sensor): Indicates nighttime routine and device availability
4. **Time Window**: Restricts detection to typical sleep hours (10 PM - 2 AM, configurable)

## How It Works

### Sleep Detection (Inference Logic)
Sleep Mode activates **only when all conditions are met:**
- Ambient light < threshold (default: 8 lux)
- Phone is face-down (proximity sensor near)
- Device is actively charging
- Current time is within configured sleep window

This multi-signal fusion prevents false positives from accidental dim lighting or charging.

### Disturbance Tracking
During active sleep mode (between 12 AM and 6 AM), the app detects disturbances:
- **Accelerometer motion spikes** - Sudden movement exceeding threshold (6 m/s² change)
- **Screen unlock events** - User interaction indicating potential wake

Each disturbance is logged to the database for analysis.

## Architecture: MVVM Pattern

### Model Layer (`data/`)
- **Room Database** (`SleepSaverDatabase`, `SleepSessionEntity`, `DisturbanceEventEntity`)
  - Persists sleep sessions with start/end times
  - Stores individual disturbance events linked to sessions
  - Accessed via `SleepDao` and `SleepRepository`
- **DataStore Preferences** (`SettingsRepository`, `ContextSnapshotStore`)
  - User settings: detection enabled, light threshold, sleep window, auto-DND toggle, dark mode
  - Latest sensor snapshot for quick access

### View Layer (`ui/`)
- **Compose UI** in `MainActivity.kt`
  - **Dashboard**: Sleep status, tonight's start time, disturbance count, weekly summary
  - **Context Logic**: Explains sensor fusion and detection logic
  - **Privacy by Design**: Data handling explanation and history deletion
  - **Settings**: User toggles and preferences
- **Theme** (`ui/theme/`)
  - Custom Indigo/Lavender color palette
  - Rounded card shapes (12dp, 18dp, 24dp)
  - Light and dark mode support

### ViewModel Layer (`SleepViewModel.kt`)
- Coordinates all app logic:
  - Observes sensor flows from `EnvironmentSensorMonitor`
  - Applies sleep inference via `SleepInferenceEngine`
  - Manages database writes to `SleepRepository`
  - Combines all streams into reactive `DashboardUiState`
- Handles user interactions (settings changes, history deletion)
- Manages sleep brightness control

## Background Task

**WorkManager Integration** (`SleepCheckWorker`, `WorkScheduler`):
- Periodic task runs every 15 minutes
- Runs even if app is closed or device locked
- Re-evaluates sleep inference conditions
- Automatically starts sleep session if conditions are met
- Gracefully handles app lifecycle

## Features

### Dashboard Screen
- Real-time status: Awake / Sleep Mode Active
- Tonight's sleep start time (formatted HH:MM AM/PM)
- Current disturbance count
- Consistency score (based on sleep start time variance)
- Weekly summary: Start times, durations, disturbance counts

### Context Logic Screen
Explains:
- Why ambient light matters (lights-off behavior indicator)
- Why proximity matters (face-down detection, not facial recognition)
- Why charging matters (bedtime routine signal)
- Time window role (prevents false positives outside typical sleep hours)
- How all signals combine for comprehensive inference

### Privacy Screen
- Certifies light sensor doesn't record images (lux values only)
- Confirms proximity sensor doesn't identify people
- Affirms no audio recording, no location tracking
- Documents local-only storage (Room database + DataStore)
- Provides history deletion switch (clears all sessions and disturbances)

### Settings Screen
User-configurable toggles and sliders:
- **Sleep Detection**: Enable/disable the entire inference engine
- **Light Threshold Slider**: Adjust sensitivity (1-50 lux)
- **Sleep Window Times**: Start hour and end hour (0-23)
- **Auto Do Not Disturb**: Toggle automatic DND when sleep detected
- **Manual Dark Mode**: Toggle dark theme (independent of system preferences)

## Data Persistence

- **Room Database** (`sleep_saver.db`)
  - 2 tables: `sleep_sessions` and `disturbance_events`
  - Auto-increment IDs, foreign key relationships
- **DataStore Preferences**
  - User settings (detection, thresholds, times, toggles)
  - Latest sensor snapshot for offline use

## Test Flow

### Test Sleep Detection

1. **Launch app** on emulator or device
2. **Set system time to 11:00 PM** (within default sleep window 10 PM - 2 AM)
   - Emulator: Settings > System > Date & time > Set time
3. **Simulate charging**: Emulator > Battery icon > Set charging state to "Charging"
4. **Simulate dark environment**: 
   - Emulator: Extended controls > Sensors > Light > Set to 5 lux (below 8 lux threshold)
5. **Simulate face-down placement**: 
   - Emulator: Extended controls > Sensors > Proximity > Set to 0 cm (near = face down)
6. **Wait for detection** (up to 1 minute for main app, up to 15 minutes for WorkManager background check)
7. **Verify Dashboard**:
   - Status should change to "Sleep Mode Active"
   - Tonight's sleep start time should appear
   - Sensor snapshot should show: Charging, Face Down, Low light

### Test Disturbance Logging

1. **Ensure sleep is active** (follow sleep detection steps above)
2. **Verify time is between 12:00 AM and 6:00 AM**
   - Change system time to 1:00 AM if needed
3. **Trigger accelerometer spike**:
   - Emulator: Extended controls > Sensors > Accelerometer > Shake device (increase magnitude changes)
4. **Alternative: Trigger screen unlock**
   - Emulator: Press power button or press unlock
5. **Refresh app** or wait for ticker update (max 60 seconds)
6. **Verify Dashboard**:
   - Disturbance count should increment
   - Weekly summary should reflect disturbance

### Test Dark Mode Toggle

1. Navigate to **Settings** screen
2. Find "Manual dark mode" toggle and toggle on/off
3. Verify UI colors switch between light and dark mode

### Test Settings Persistence

1. Adjust settings (light threshold, sleep window, etc.)
2. Close app completely
3. Relaunch app
4. Verify all settings are restored

## References & Resources

- **Android Sensors API**: https://developer.android.com/guide/topics/sensors
- **Room Database**: https://developer.android.com/training/data-storage/room (I also used this for my MQP!)
- **DataStore Preferences**: https://developer.android.com/topic/libraries/architecture/datastore
- **WorkManager**: https://developer.android.com/topic/libraries/architecture/workmanager
- **Compose Layout**: https://developer.android.com/jetpack/compose/layouts
- **Material 3 Theme**: https://developer.android.com/develop/ui/compose/designsystems/material3
