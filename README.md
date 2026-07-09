# Garage — 2.0

A ground-up rebuild of the original vehicle maintenance tracker, using
Kotlin, Jetpack Compose, Hilt, Room, and Firestore. See the architecture
notes below for why each piece is built the way it is.

## Status: Phase 1

This is intentionally a **partial** app so far — a working prototype to
build confidence in the architecture before investing in every screen.

**Built and working:**
- Sign in / register / sign out (Firebase Auth)
- Home dashboard: vehicle selector, due-soon tasks, recent activity
- Offline-first data layer (Room + Firestore sync) for vehicles, service
  records, and maintenance tasks
- Bottom navigation shell (Home / Vehicles / Checkups / Profile)
- Daily WorkManager check for due tasks (notification scaffolding)

**Stubbed for now** ("Coming soon" placeholder screens):
- Vehicles tab (list view is actually already built in
  `ui/vehicles/VehiclesScreen.kt` — just not wired into navigation yet)
- Checkups/Maintenance tab

**Not started yet:**
- Add Vehicle screen
- Log a Service Record screen (including the natural-language "describe
  it" field — the parsing logic already exists and is unit-testable at
  `domain/usecase/ParseServiceEntryUseCase.kt`)
- Add/edit Maintenance Task screen
- Full Profile screen (theme toggle, app info, etc.)

Because there's no Add Vehicle screen yet, Home will show its empty state
until you either build that screen or insert a test document directly in
the Firestore console under `users/{your uid}/vehicles/{any-id}`.

### Suggested phase plan
1. ~~Login/Register + Home~~ ✅ (this phase)
2. Vehicles tab + Add Vehicle screen
3. Log Record screen (with the NL parser wired up) + record detail
4. Maintenance tab + Add Task screen
5. Real Profile screen, polish, tests

## Setup

1. Open the project root in Android Studio (Koala or newer recommended).
2. Create a Firebase project, add an Android app with package name
   `com.example.garage`, and download `google-services.json`.
3. Replace `app/google-services.json.example` — save your real file as
   `app/google-services.json` (this filename is gitignored/should not be
   committed since it's tied to your Firebase project).
4. In the Firebase console, enable **Authentication → Email/Password**
   and create a **Firestore database** (start in test mode while
   developing; lock down rules before sharing with family/friends — see
   below).
5. Sync Gradle and run. If Android Studio doesn't offer to generate the
   Gradle wrapper jar automatically, run `gradle wrapper` once from the
   project root, or just let Android Studio's "Sync Project" handle it.

### Suggested Firestore security rules (tighten before real use)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Architecture

- **UI**: Jetpack Compose, Material3, single-Activity + Navigation-Compose.
- **State**: `StateFlow` + Hilt `ViewModel`s, no `LiveData`.
- **Data**: Room is the single source of truth the UI reads from.
  Firestore only talks to the repository layer — see
  `data/repository/*Repository.kt`. Each entity has an `isSynced` /
  `isDeleted` flag so writes work instantly offline and get pushed to
  Firestore in the background (`SyncCoordinator` wires this up per user,
  started from `GarageApplication`).
- **DI**: Hilt (`di/` modules for Room, Firebase, and a process-lifetime
  `CoroutineScope`).
- **Domain layer**: plain Kotlin models decoupled from both Room entities
  and Firestore documents, plus small use-cases
  (`TaskUrgencyCalculator`, `ParseServiceEntryUseCase`) that are easy to
  unit test in isolation from Android.
- **Notifications**: WorkManager + Hilt-Work (`notifications/`).

## Package (application ID)
`com.example.garage` — change this before publishing anywhere; it's a
placeholder matching the original app's `com.example.myapp` convention.
