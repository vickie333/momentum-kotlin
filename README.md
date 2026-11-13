# Momentum for Android

This is the migration scaffold for Momentum. The Kotlin Multiplatform `shared` module will contain the models and training engine; `androidApp` will contain the Jetpack Compose UI and Android integrations. The app currently opens to a placeholder screen.

Build with `./gradlew :shared:jvmTest :androidApp:assembleDebug`. Swift golden fixtures are in `shared/src/commonTest/resources/golden/`; the iOS source remains in the sibling `fitness-app` repository while parity is being established.
