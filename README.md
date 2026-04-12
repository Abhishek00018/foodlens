# Caltrack — Calorie Tracker App

Photo-based calorie tracker Android app. User takes a photo of their meal, AI identifies the food and estimates calories/macros.

## Repo Structure

```
Caltrack/
├── android/              # Android app (Kotlin, Jetpack Compose)
├── backend/              # Spring Boot 3 backend (optional / user builds this)
├── docker-compose.yml    # Local MySQL 8 (for backend)
└── .claude/              # Project notes + agent assets
```

## Android App

### Tech Stack

- Kotlin, Jetpack Compose (Material 3)
- Navigation Compose
- Hilt
- Room
- CameraX
- Retrofit
- Vico (charts)
- DataStore (preferences)

### Prerequisites

- Android Studio (recent stable)
- JDK 21
- Android SDK installed (via Android Studio)
- An emulator (AVD) or a physical device

### First-Time Setup (Android Studio ≥ 2024)

These steps are written for developers opening the project in Android Studio for the first time.

1. Open Android Studio → **Open** → select the `Caltrack/android` folder.
2. Let Gradle sync complete (Android Studio will prompt for it).
3. Set the JDK used by Gradle to Java 21:
	- Android Studio → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Gradle** → **Gradle JDK** → select **JDK 21**.
4. Create an emulator:
	- **Tools → Device Manager** → **Create device** → choose a recent Pixel device → pick a system image **API 34+** (or latest available).
5. Select the correct Run configuration:
	- Top toolbar dropdown should be the **app** configuration (module `:app`).

Optional (recommended if you use the terminal): make `adb` available in your shell.

```bash
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export PATH="$PATH:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator"
```

You can add the lines above to `~/.zshrc` so they persist.

When you hit the green **Run** button, the app should launch into the **Login** screen. After login/skip, the flow continues and interactions like the **profile avatar** and **meal cards** should be clickable.

### One-Time Configuration (Prevents “Cached App” Runs)

Most “stale UI” reports come from Android Studio bringing an already-running app task to the foreground or reinstalling without clearing existing app state.

Do this once on your machine:

- Android Studio → **Settings** → **Build, Execution, Deployment** → **Deployment** → enable:
  - **Uninstall application before launch**

After enabling it, the green **Run** button will reliably reinstall a fresh build each time, so you won’t keep seeing an older installed instance.

### Build (Debug)

From the repo root:

```bash
cd android
JAVA_HOME=$(/usr/libexec/java_home -v 21.0.7) \
ANDROID_HOME=$HOME/Library/Android/sdk \
./gradlew :app:assembleDebug
```

### Run on Emulator / Device

Recommended (installs a fresh debug build onto the connected emulator/device):

```bash
cd android
./gradlew :app:installDebug
```

Then launch from Android Studio (Run ▶) or via ADB:

```bash
$ANDROID_SDK_ROOT/platform-tools/adb shell am start -n com.caltrack.app/.MainActivity
```

## “Non-cached” / Always See Latest UI (Android Studio)

If you keep seeing an older UI (or missing click interactions), it’s usually the emulator keeping the old app process/data, or Android Studio bringing an existing task to the foreground.

### Fast checklist

1. Stop the running app (Android Studio Run window ▶︎ Stop).
2. Force-stop the app:

```bash
$ANDROID_SDK_ROOT/platform-tools/adb shell am force-stop com.caltrack.app
```

3. Reinstall debug (fresh deploy):

```bash
cd android
./gradlew clean :app:installDebug
```

### Strongest “fresh start” (guaranteed)

```bash
$ANDROID_SDK_ROOT/platform-tools/adb uninstall com.caltrack.app
cd android
./gradlew clean :app:installDebug
$ANDROID_SDK_ROOT/platform-tools/adb shell am start -n com.caltrack.app/.MainActivity
```

### Android Studio setting (recommended)

Enable: **Settings → Build, Execution, Deployment → Deployment → Uninstall application before launch**.

### If pressing Run still doesn’t show Login

- Confirm you’re launching the right app/configuration:
	- Run configuration is **app** (module `:app`)
	- Selected device is the emulator you expect (Device Manager)
- Hard reset the app state once:
	- `$ANDROID_SDK_ROOT/platform-tools/adb uninstall com.caltrack.app`
	- then Run again
- If the emulator is acting “sticky”, do a cold boot:
	- Device Manager → emulator menu → **Cold Boot Now**

### If `./gradlew :app:installDebug` says “No connected devices!”

This error means there is no running emulator or connected phone available to install onto.

1. Start an emulator:
	- Android Studio → **Tools → Device Manager** → press the **Play** button on an emulator.
2. Verify it is visible to ADB:

```bash
$HOME/Library/Android/sdk/platform-tools/adb devices -l
```

You should see a device like `emulator-5554` (or a physical device serial).
3. Retry the install:

```bash
cd android
./gradlew :app:installDebug
```

### If it still looks stale

- Clear app data on the emulator: Settings → Apps → Caltrack → Storage → Clear storage
- Cold boot the emulator (Device Manager → your emulator → Cold Boot Now)

## Backend (Optional)

A Spring Boot 3 backend can be run separately. A local MySQL 8 container is provided:

```bash
docker compose up -d
```

## Documentation

- Primary project notes: `.claude/CLAUDE.md`
- A doc-maintenance agent is available at `.github/agents/caltrack-doc-updater.agent.md` to keep docs updated after new features are implemented.
