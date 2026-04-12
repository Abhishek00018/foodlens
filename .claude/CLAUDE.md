# Caltrack — Calorie Tracker App

## Overview
Photo-based calorie tracker Android app. User takes a photo of their meal, AI (AWS Bedrock Claude Vision) identifies the food and estimates calories/macros.

## Project Structure
```
Caltrack/
├── android/              # Android app (Kotlin, Jetpack Compose)
│   └── app/src/main/java/com/caltrack/app/
│       ├── CaltrackApp.kt           # Hilt Application
│       ├── MainActivity.kt          # Entry + Compose
│       ├── data/                    # Room DB, Retrofit, Repositories
│       ├── di/                      # Hilt modules
│       └── ui/
│           ├── NavHost.kt           # Navigation + transitions
│           ├── theme/Theme.kt       # Premium Dark theme
│           ├── camera/              # Camera + scan result sheet
│           ├── dashboard/           # Main screen + progress ring
│           ├── history/             # History + weekly chart
│           └── components/          # Reusable composables
├── backend/              # Spring Boot 4 backend at /Users/abhisheksharma/IdeaProjects/Caltrack/
├── docker-compose.yml    # Local MySQL 8
└── .claude/
    ├── agents/           # Specialized Claude agents
    └── context/          # Cross-session status docs
        ├── backend-status.md   # Backend: what's done, pending, API contract
        └── android-status.md   # Android: what's done, pending, expectations
```

## Key Technical Details
- **Android:** Kotlin, Compose, CameraX, Room, Hilt, Retrofit, Vico
- **Backend:** Spring Boot 4.0.5, Java 21, Maven, MySQL 8 (at `/Users/abhisheksharma/IdeaProjects/Caltrack/`)
- **AWS:** Bedrock (`us.anthropic.claude-sonnet-4-6`), S3, Cognito, EC2, RDS
- **Theme:** Premium Dark (#0D0D14 bg, #80FF00 neon lime accent)
- **Build:** `cd android && JAVA_HOME=$(/usr/libexec/java_home -v 21.0.7) ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew assembleDebug`

## Coding Conventions
- Compose screens: one file per screen in dedicated package
- ViewModels: @HiltViewModel, expose StateFlow
- Room: entities in data/local/entity/, DAOs in data/local/dao/
- API: Retrofit interfaces in data/remote/
- Use Material 3 components only
- All colors from Theme.kt, never hardcode outside theme

## Cross-Session Context
Before starting work, read the relevant context file:
- **Backend session:** Read `.claude/context/backend-status.md` for done/pending status + full API contract
- **Android session:** Read `.claude/context/android-status.md` for done/pending status + what Android expects from backend
- **Both files define the shared API contract** — any endpoint change must be reflected in both
