# Fase 1 — Esqueleto del proyecto ✅

Plan maestro §5 Fase 1.

## Hecho

- Gradle Kotlin DSL + catálogo `gradle/libs.versions.toml`: Kotlin 2.4.21, AGP 9.3.1, Compose BOM 2026.09.00, serialization 1.11.0, datetime 0.8.0, coroutines 1.11.0, Health Connect 1.1.0, Navigation 2.10.2.
- `shared` (KMP, plugin `com.android.kotlin.multiplatform.library`): targets `android`, `jvm`, `iosArm64`, `iosSimulatorArm64`; toolchain 21; `commonMain` con serialization, datetime, coroutines; `commonTest` con `kotlin.test`; `SmokeTest`.
- `androidApp`: `applicationId com.momentum.app`, `minSdk 26`, `compileSdk`/`targetSdk 37`, Compose; `MainActivity` con texto de marcador.
- Copiados: `data/exercises.json`, `tools/` (Python de i18n, ejercicios, animaciones, mockups, `EngineSim/main.swift` de referencia), fixtures de oro.
- `shared/src/commonMain/resources/`: `exercises.json`, `motions.json`.
- CI `.github/workflows/core.yml`: Ubuntu, JDK 21, Android SDK 37, `./gradlew :shared:jvmTest :androidApp:assembleDebug`.
- `.gitignore`, `README.md` mínimo.

## Validación

`./gradlew :shared:jvmTest :androidApp:assembleDebug` → exit 0, `androidApp-debug.apk` generado (2026-11-13, local).

## Diferencias con el plan maestro

- CI usa solo JDK 21 (el plan decía 17/21). Suficiente: el toolchain es 21.
- Aún no existen `jvmMain`, `androidMain` ni el bloqueo de orientación vertical; se añaden en fases 4/9 y 7.
