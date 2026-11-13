# Plan: migrar Momentum (iOS / Swift) a Kotlin

Repositorio origen: `~/Documents/github/fitness-app` (app **Momentum**, SwiftUI, iOS 17+, ~10.5k líneas Swift, 115 archivos).
Destino propuesto: nuevo repositorio `~/Documents/github/momentum-kotlin` (o carpeta hermana; el origen no se toca hasta tener paridad).

Codex effort: high

> Este documento es autocontenido: quien lo implemente no ve la conversación donde se escribió.

---

## 1. Objetivo y decisión de alcance

Tener Momentum en Kotlin con **la misma funcionalidad y las mismas decisiones de entrenamiento** que la versión Swift: offline, sin cuenta, sin servidor, tres idiomas (en / es / pt-BR), motor explicable.

**Decisión recomendada: Kotlin Multiplatform (KMP) con la UI de Android en Jetpack Compose.**

| Opción | Resultado | Coste | Veredicto |
|---|---|---|---|
| A. Android nativo, todo en un módulo | App Android; iOS queda en Swift y diverge | Bajo | Duplica el motor: dos fuentes de verdad para las reglas de entrenamiento |
| **B. KMP: `shared` (motor + modelos) + `androidApp` (Compose)** | App Android; el motor Kotlin puede reutilizarse luego en iOS | Medio (+ ~10 % de estructura) | **Recomendada** |
| C. KMP + Compose Multiplatform (UI compartida, iOS también) | Una sola base para ambas plataformas | Alto; descarta la UI SwiftUI que ya funciona | Solo si se quiere abandonar Swift por completo |

Este plan ejecuta la **opción B**. El módulo `shared` no puede importar nada de Android (`android.*`, `Context`, etc.) ni de la JVM-only (`java.*`), para que quede listo para iOS. Si se decide A, basta fusionar `shared` dentro de `androidApp` (las fases no cambian).

Fuera de alcance: rediseño visual, nuevas funciones del ROADMAP, publicar en Play Store, retirar la app iOS.

---

## 2. Qué hay hoy (mapa de origen → destino)

Arquitectura actual (ver `docs/ARCHITECTURE.md`): dos mitades que solo hablan mediante tipos de datos planos.

| Origen (Swift) | Líneas aprox. | Destino (Kotlin) | Dificultad |
|---|---|---|---|
| `Momentum/Models/` (Models, Backup, Motion, ExerciseMeta, ExerciseLibrary, SessionSummary, ExerciseProgress, Localization) | ~1.5k | `shared/commonMain/.../model` | Baja |
| `Momentum/Models/ExerciseCatalog.swift` (generado de `data/exercises.json`) | gen. | `shared` — leer el JSON directamente | Baja |
| `Momentum/Models/LocalizationData.swift`, `ExerciseTranslations.swift` (generados desde `tools/i18n/*.py`) | gen. | Recursos Android + tablas generadas | Media |
| `Momentum/Design/MotionData.swift` (JSON de animaciones generado de `tools/motions_*.py`) | gen. | Recurso JSON en `shared` | Baja |
| `Momentum/Engine/` (PlanGenerator 721, Progression 419, Alternatives, Periodization, AdaptiveEngine, Assessment, WeeklyReview, Reminders, TrainingCalendar, DemoData) | ~2k | `shared/commonMain/.../engine` | **Alta** (precisión numérica y fechas) |
| `Momentum/Store/AppStore.swift` (+ LaunchMode, ReminderScheduler) | ~450 | `shared` (`AppStore` como `StateFlow`) + `androidApp` (scheduler) | Media |
| `Momentum/Health/HealthService.swift` (HealthKit) | 205 | `androidApp` con **Health Connect** | Media-alta (no hay equivalente 1:1) |
| `Momentum/Design/FigureView.swift` + `Components.swift` + `Theme.swift` | ~550 | `androidApp` Compose `Canvas` + tema Material 3 | Media |
| `Momentum/Views/**` (SwiftUI) | ~3.3k | `androidApp` Compose | Media (volumen) |
| `MomentumTests/` (14 archivos, 170+ tests) | ~2.4k | `shared/commonTest` (kotlin.test) | Media |
| `MomentumUITests/ScreenshotTests.swift` | 110 | Compose UI tests / Paparazzi o Maestro | Baja-media |
| `Tools/EngineSim/main.swift` | 332 | `shared` ejecutable JVM (`jvmMain`) o tarea Gradle | Baja |
| `tools/**/*.py` (generadores i18n, ejercicios, animaciones) | — | **Se reutilizan sin portarlos**; se añaden exportadores Kotlin/Android | Baja |
| `.github/workflows/*` | — | Nuevos workflows Gradle | Baja |

Dato clave: **`Package.swift` ya aísla `Models` + `Engine`** como paquete sin UI y testeable en Linux. Esa frontera es exactamente el módulo `shared`; es el primer trozo a migrar y el que más valor tiene.

---

## 3. Riesgos reales y cómo se controlan

1. **Paridad de decisiones del motor** (el riesgo nº 1). `PlanGenerator` es una función pura de `(profile, history, date)`; cualquier diferencia de redondeo, orden o fecha cambia el plan.
   - Control: **tests de oro (golden)**. Antes de portar, generar con el código Swift actual un conjunto de fixtures JSON (entrada + salida esperada) y hacer que los tests Kotlin los lean. Ver Fase 0.
2. **Fechas y calendario.** Swift usa `Calendar.current` con `weekStart`, `weeksBetween`, `daysBetween`, `sameDay` (`Engine/TrainingCalendar.swift`) y `Dictionary(grouping:)` por día (`Models/SessionSummary.swift`).
   - Control: usar `kotlinx-datetime` (`LocalDate`, `TimeZone`) con una clase `TrainingCalendar` propia que reproduzca exactamente primer día de la semana y husos horarios; test explícito de cambio de horario de verano y de semana que cruza año.
3. **Orden de colecciones.** Los `Dictionary` de Swift no tienen orden estable; Kotlin `LinkedHashMap` sí. Si algún resultado depende hoy de iterar un diccionario, hay que encontrar el desempate determinista. Revisar cada `Dictionary(grouping:)`, `.keys`, `.values` y `Set` en `Engine/` y `Models/` y dejar el desempate escrito.
4. **Aritmética de coma flotante.** `Double` es igual en ambos, pero `rounded()` de Swift es "al más cercano, empates lejos de cero" y el redondeo de Kotlin (`roundToInt`) es "empates hacia +∞". Crear funciones `roundHalfAwayFromZero` en `shared` y usarlas en cada punto donde el original usa `.rounded()`. Los pasos de carga (mancuernas, discos) deben comparar con tolerancia donde el original ya lo hace.
5. **Compatibilidad del archivo de copia de seguridad.** `Backup.swift` usa `JSONEncoder` con `dateEncodingStrategy = .iso8601` (sin fracciones de segundo), `format = "momentum-backup"`, `currentVersion = 1`, y `AppStore` guarda un único JSON con `PersonalData { profile, sessions, weights }`. Con "tolerant `init(from:)`" en `UserProfile` para leer guardados viejos.
   - Control: el Kotlin debe **leer y escribir exactamente ese JSON** (nombres de campos, enums como string, fechas ISO-8601 con `Z` y sin milisegundos, campos opcionales ausentes). Un backup creado en iOS debe restaurarse en Android y viceversa. Usar `kotlinx.serialization` con `ignoreUnknownKeys = true`, `coerceInputValues = true`, `explicitNulls = false` y valores por defecto en todos los campos nuevos.
6. **Localización.** El inglés es la clave (`L("Up {0} kg", 2)`), placeholders `{0}`, `{1}`; fallback a inglés; idioma activo propio (`Loc.language`) distinto del del sistema; español latinoamericano neutro (`es_419`), portugués de Brasil (`pt_BR`).
   - Control: **no usar `strings.xml` como fuente de verdad** (las claves son frases en inglés con espacios y signos). Generar un `Translations` Kotlin/JSON desde las mismas tablas Python y mantener la API `L("…", args)`. Se puede generar además `strings.xml` solo para lo que Android exige localizar en el manifest (nombre de la app, permisos de salud: hoy `Resources/*.lproj/InfoPlist.strings`).
7. **Salud.** HealthKit → Health Connect no es equivalente: permisos distintos, política de Google Play para apps de salud, pasos y energía activa se leen distinto, no hay Apple Watch sino Wear OS / apps de terceros. Tratarlo como funcionalidad **opcional y detrás de una interfaz** (`HealthGateway`) para poder entregar la app sin ella.
8. **Notificaciones y recordatorios.** `Reminders.plan` (decisión) es pura y se porta al `shared`; `ReminderScheduler` (programar en el sistema) se reescribe con `AlarmManager`/`WorkManager` y permiso `POST_NOTIFICATIONS` (Android 13+) más `SCHEDULE_EXACT_ALARM` o alarma inexacta aceptable ("a la mañana").
9. **Animaciones de figuras.** `FigureRenderer` dibuja un esqueleto de 14 valores por fotograma interpolados entre keyframes con `GraphicsContext`. Es geométrico puro y se porta a `DrawScope` de Compose casi línea a línea. Respetar *Reduce Motion* (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`).
10. **Volumen de UI.** ~3.3k líneas de SwiftUI no se traducen automáticamente. Se migran pantalla a pantalla con el mismo árbol de navegación.

---

## 4. Estructura de destino

```
momentum-kotlin/
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml          # catálogo de versiones
  shared/                            # KMP: commonMain, commonTest, jvmMain (EngineSim), androidMain (vacío al principio)
    src/commonMain/kotlin/com/momentum/
      model/        # Models, Backup, Motion, ExerciseMeta, ExerciseLibrary, SessionSummary, ExerciseProgress
      engine/       # PlanGenerator, Progression, AdaptiveEngine, Alternatives, Periodization,
                    # Assessment, WeeklyReview, Reminders, TrainingCalendar, DemoData
      i18n/         # Loc, AppLanguage, L(), tablas generadas
      store/        # AppStore (StateFlow), PersonalData, LaunchMode
      health/       # interfaz HealthGateway (sin implementación)
    src/commonMain/resources/       # exercises.json, motions.json
    src/commonTest/kotlin/…         # port de MomentumTests + golden tests
    src/commonTest/resources/golden/
    src/jvmMain/kotlin/…/EngineSim.kt
  androidApp/
    src/main/AndroidManifest.xml
    src/main/kotlin/com/momentum/
      MainActivity.kt, MomentumApp.kt
      ui/theme/        # Theme.kt (Material 3, claro/oscuro)
      ui/components/   # Components.kt, FigureView.kt
      ui/onboarding|workouts|explore|progress|settings/
      data/            # FileStorage (archivo JSON en filesDir), BackupDocument (SAF CreateDocument)
      health/          # HealthConnectGateway
      reminders/       # ReminderScheduler
    src/androidTest/   # UI tests / capturas
  tools/                # copiados desde fitness-app: python de i18n, ejercicios, animaciones + exportadores nuevos
  data/exercises.json   # fuente de verdad del catálogo (misma que iOS)
```

Pila: Kotlin 2.x, Gradle (Kotlin DSL + version catalog), `kotlinx.serialization-json`, `kotlinx-datetime`, `kotlinx.coroutines`, Jetpack Compose + Material 3, Navigation Compose, `androidx.health.connect:connect-client`. `minSdk` 26, `targetSdk` el vigente. **Sin DI framework** (el original no lo tiene; pasar dependencias por constructor). Gráficos del tab Progreso: el original usa Swift Charts; elegir Vico o dibujar con `Canvas` (preferible `Canvas` para no sumar dependencia: son líneas y barras simples).

> Antes de fijar versiones exactas, comprobar las vigentes de cada librería; no copiar versiones de este documento.

---

## 5. Fases y tareas

Cada fase termina con un criterio de validación ejecutable. No pasar a la siguiente con la anterior en rojo.

### Fase 0 — Línea base y fixtures de oro (en el repo Swift, sin cambiar comportamiento)

1. Ejecutar `swift test` en `fitness-app` y anotar que pasa (170+ tests). Si hay fallos previos, listarlos y no considerarlos regresiones.
2. Añadir a `Tools/EngineSim` (o a un target nuevo `GoldenExport`) un comando `golden` que escriba JSON en `fitness-app/docs/golden/` con **entradas y salidas** reales del motor:
   - `plans.json`: ≥ 30 perfiles distintos (objetivo × nivel × equipo × áreas protegidas × días × duración × sexo/edad/peso) → `Workout` de la semana y fecha dada.
   - `sim16w.json`: la simulación de 16 semanas del atleta sintético (`EngineSim sim`), sesión a sesión: perfil resultante, peldaños, deloads, mensajes.
   - `alternatives.json`: `Alternatives.suggest` por cada motivo de cambio (muy difícil, muy fácil, duele, sin equipo, no me gusta).
   - `progression.json`, `weekly_review.json`, `reminders.json`, `assessment.json`: casos de los tests existentes.
   - `backups/`: un backup real generado con `Backup` + uno antiguo sin campos nuevos + uno con `version` mayor (debe rechazarse).
3. Fechas **fijas** en todas las entradas (no `Date()`); idioma fijo `en` salvo en los casos de localización.
4. Commit en el repo Swift: "Golden fixtures para la migración a Kotlin". Estos archivos se copian a `shared/src/commonTest/resources/golden/`.

**Validación:** los fixtures se regeneran idénticos dos veces seguidas (determinismo). Si no, hay no determinismo en el motor: documentarlo (es un hallazgo que además debe resolverse en Kotlin).

### Fase 1 — Esqueleto del proyecto Kotlin

1. Crear `momentum-kotlin` con Gradle, módulos `shared` y `androidApp`, catálogo de versiones, `.gitignore`, `README` mínimo.
2. Copiar `data/exercises.json`, `tools/` y los fixtures.
3. Workflow de CI `core.yml` (Ubuntu, JDK 17/21): `./gradlew :shared:jvmTest` (equivalente de `core.yml` Swift) y `:androidApp:assembleDebug`.

**Validación:** `./gradlew :shared:jvmTest :androidApp:assembleDebug` termina en verde con un test de ejemplo y una `MainActivity` vacía.

### Fase 2 — Modelos y serialización (`shared/model`)

1. Portar `Models.swift` (661 líneas): enums `FitnessLevel`, `Goal`, `Equipment`, `Sex`, `BodyArea`, `TrainingHistory`, `RecentFrequency`, …; `UserProfile`, `WorkoutSession`, `WeightEntry`, `Workout`, etc.
   - Enums con `@SerialName` igual al `rawValue` de Swift. Los enums con `Int` raw (`TrainingHistory`, `RecentFrequency`) se serializan como número (serializador propio).
   - Propiedades calculadas que usan `L(...)` (títulos, descripciones) pasan a extensiones o a funciones; los **iconos SF Symbols** (`dumbbell.fill`, `bolt.fill`, …) se reemplazan por una clave semántica (`enum IconKey`) que cada plataforma mapea (Material Icons en Android).
2. Portar `Motion.swift`, `ExerciseMeta.swift`, `ExerciseLibrary.swift` (decodificar `exercises.json` y `motions.json` desde recursos), `SessionSummary.swift`, `ExerciseProgress.swift`.
3. Portar `Backup.swift`: `PersonalData`, `BackupFile`, `BackupError { NotABackup, NewerVersion(n) }`, exportación CSV. Serializador de `Instant` ISO-8601 sin fracciones (ver riesgo 5).
4. Portar `BackupTests` y los tests de modelos de `CoreTests`.

**Validación:** `BackupTests`/`CoreTests` portados en verde; los backups de `golden/backups/` generados por iOS se leen y re-escriben **byte a byte equivalentes** (comparación JSON estructural); el backup de versión futura lanza `NewerVersion`.

### Fase 3 — Localización (`shared/i18n`)

1. Portar `Localization.swift`: `AppLanguage { en, es, pt }`, detección por lista de idiomas preferidos (`detect`), `Loc.language`, `L(key, vararg args)` con `{0}`, `{1}`, fallback a inglés.
2. Añadir exportadores en `tools/i18n/` (nuevos `export_kotlin.py`, `export_exercises_kotlin.py`) que generen desde las mismas tablas Python un `LocalizationData.kt` / `ExerciseTranslations.kt` (o JSON en recursos, preferible por tamaño de método de JVM) y mantengan `--strict` (falla si falta una traducción).
3. Formateo de fechas con plantillas (`Loc.date(date, "EEEE")`, `"MMMMd"`, `"jm"`): definir una interfaz `DateFormatter` en `shared` e implementar en Android con `java.time.format.DateTimeFormatter`/`DateFormat.getBestDateTimePattern` y la `Locale` del idioma de la app (`es-419`, `pt-BR`).
4. Portar `LocalizationTests` (completitud de traducciones, placeholders coherentes entre idiomas).

**Validación:** `LocalizationTests` en verde; el job de CI "translations" regenera las tablas Kotlin y falla con `git diff --exit-code` si están desactualizadas.

### Fase 4 — Motor (`shared/engine`) — la fase crítica

Orden recomendado (de hojas a raíz): `TrainingCalendar` → `Periodization` → `Progression` → `Assessment` → `Alternatives` → `AdaptiveEngine` → `PlanGenerator` → `WeeklyReview` → `Reminders` → `DemoData`.

Reglas de portado:
- Mantener nombres, firmas y estructura de funciones para facilitar la comparación línea a línea.
- Cada `rounded()` → `roundHalfAwayFromZero`; cada `Dictionary` iterado → mapa ordenado con desempate explícito (riesgo 3 y 4).
- Sin `Date()`/`Instant.now()` dentro del motor: la fecha siempre es parámetro (ya es así).
- Documentar en `docs/ENGINE.md` (copiado) cualquier diferencia inevitable.

Tests: portar `PlanTests` (606), `AdaptationTests`, `ProgressionTests`, `AssessmentTests`, `InvariantTests`, `WeeklyReviewTests`, `ReminderTests`, `DemoDataTests`, `ExerciseProgressTests`, `HistoryTests`, y `TestSupport`; **más** los tests de oro: para cada fixture, ejecutar el motor Kotlin y comparar estructuralmente con la salida de Swift (ejercicios, series, repeticiones, cargas, mensajes "por qué"). Textos comparados en `en`.

**Validación (criterio de aceptación de la migración del motor):**
- 100 % de los tests portados en verde.
- 100 % de los fixtures de oro idénticos (se admite tolerancia de 1e-9 en `Double`, nada más).
- `EngineSim sim` en Kotlin imprime la misma tabla de 16 semanas que en Swift (diferencia vacía con `diff`).

### Fase 5 — Almacén de estado (`shared/store` + Android `data`)

1. `AppStore` como clase con `StateFlow<PersonalData>`: `profile`, `sessions`, `weights`; guarda tras cada cambio en un único archivo JSON (`filesDir/momentum.json`) con escritura atómica (archivo temporal + rename). Expone los mismos ayudantes de plan y estadísticas que usan las vistas.
2. Interfaz `Storage { read(): String?; write(String) }` en `shared`; implementación Android en `androidApp/data`. Para tests, implementación en memoria.
3. `LaunchMode`: equivalente a `-uiTesting`/`-demoData`/`-demoLanguage` mediante extras del `Intent` o `BuildConfig` en variantes de depuración (nunca toca los datos reales).
4. Recuperación segura: si el JSON está corrupto, conservar una copia `.corrupt` y arrancar vacío (pendiente del ROADMAP "copia automática local antes de cambios de esquema" — implementar solo la copia mínima aquí).

**Validación:** tests de `AppStore` (guardar → reabrir → mismo estado; JSON corrupto no borra datos; migración desde un guardado iOS de `golden/backups/`).

### Fase 6 — Tema, componentes y figuras animadas (`androidApp/ui`)

1. `Theme.kt`: Material 3 claro/oscuro equivalente a `Theme.swift` (leer colores y tipografía del original; `AccentColor` en `Assets.xcassets`).
2. `Components.kt`: portar `Components.swift` (194 líneas) a composables.
3. `FigureView.kt`: portar `FigureRenderer` a `DrawScope`: misma transformación de `bounds`, mismos props (`bench`, `chair`, `bar`, `dumbbell`, `barbell`, cables, `plate`), interpolación entre fotogramas con `dur`, soporte de `held`; pausa/estático con animaciones desactivadas.
4. Verificación visual: captura de las 75 animaciones y comparación con `docs/exercise-animations.png`.

**Validación:** pantalla de galería de depuración que muestra los 75 ejercicios; revisión visual contra `docs/exercise-animations.png` (sin diferencias de forma).

### Fase 7 — Pantallas (`androidApp/ui`), en este orden

1. Navegación raíz: `RootView` → `NavHost` con barra inferior (Entrenamientos, Explorar, Progreso, Ajustes) y decisión onboarding/app.
2. **Onboarding** (620 líneas): objetivo, historial, chequeo de fuerza opcional, días disponibles, duración, equipo (incluida la mancuerna más pesada), áreas a proteger, bajo impacto, sexo/edad/peso/altura, vista previa del plan, selector de idioma en la primera pantalla.
3. **Entrenamientos**: `WorkoutsView`, `WorkoutDetailView`, `SwapSheet` (bottom sheet con motivos y alternativas), **`ActiveWorkoutView`** (645 líneas: series, descanso, temporizador, "cómo te sientes").
4. **Explorar** (`ExploreView`): entrenamientos rápidos (10 min, sin equipo, movilidad) y enlaces de vídeo (`curatedVideos` + búsqueda de YouTube con `Intent.ACTION_VIEW`).
5. **Progreso** (`ProgressTabView` 487 + `HistoryView`): calorías/minutos semanales, volumen por músculo vs objetivo, 1RM estimado y récords, tendencia de peso, racha, gráfico por ejercicio, revisión semanal.
6. **Ajustes** (`SettingsView` 407 + `DataFileDocument`): idioma, recordatorios, Salud, exportar/importar copia (`ActivityResultContracts.CreateDocument` / `OpenDocument`), exportar CSV, borrar datos.

Reglas: todo texto visible pasa por `L(...)`; accesibilidad (`contentDescription`, tamaños de toque ≥ 48 dp, escala de fuente); rotación bloqueada a vertical como el original (`UIInterfaceOrientationPortrait`); modo claro/oscuro.

**Validación por pantalla:** UI test de Compose que arranca con `-demoData` equivalente, y captura en en/es/pt (sustituye a `ScreenshotTests`); recorrido manual con emulador del flujo completo: onboarding → primer entrenamiento → terminar → ver progreso → exportar y restaurar copia.

### Fase 8 — Plataforma: recordatorios y Salud

1. **Recordatorios:** `Reminders.plan` ya está en `shared`; `ReminderScheduler` Android programa notificaciones locales en días de entrenamiento con el título/cuerpo localizados. Canal de notificación silencioso, permiso `POST_NOTIFICATIONS` pedido solo al activar. Reprogramar tras reinicio (`BOOT_COMPLETED`) y al cambiar perfil.
2. **Salud:** `HealthGateway` en `shared`: `requestAuthorization`, `saveWorkout`, `saveWeight`, `readSteps`, `readActiveEnergy`. `HealthConnectGateway` en Android con `HealthConnectClient`; comprobar disponibilidad (`getSdkStatus`), declaración de permisos y la pantalla de justificación de privacidad que Health Connect exige. Si no está disponible, ocultar la sección en Ajustes sin error.
3. Actualizar `PRIVACY.md` para Android (qué se lee/escribe, que nada sale del dispositivo).

**Validación:** prueba manual en emulador con Health Connect instalado: guardar un entrenamiento y verlo en Health Connect; recordatorio de prueba disparado con `adb`.

### Fase 9 — Herramientas, CI y documentación

1. `EngineSim` (jvmMain) con los mismos subcomandos `plans`, `sim [-v]`, `alts`.
2. CI: `core.yml` (tests de `shared`), `android.yml` (build + tests instrumentados con emulador o Gradle Managed Devices), `screenshots.yml` (capturas en en/es/pt como artefacto de la ejecución; el repositorio solo conserva `main`), job `translations`.
3. Documentación: README, `docs/ARCHITECTURE.md` (la misma "una idea": dos mitades que solo hablan mediante datos), `docs/ENGINE.md` copiado, CHANGELOG, guía para añadir pantallas/traducciones/ejercicios, instrucciones de compilación y firma.
4. Script equivalente a `scripts/install-on-phone.sh`: `./gradlew :androidApp:installDebug`.

**Validación:** un clon limpio compila y pasa todos los jobs en CI; un colaborador puede añadir un ejercicio en `data/exercises.json` y ver el cambio en ambas apps tras regenerar.

### Fase 10 — Paridad y corte

1. Lista de comprobación funcional completa contra `README.md` ("What it does"): cada viñeta marcada en Android.
2. Prueba cruzada de datos: backup iOS → restaurar en Android → el siguiente entrenamiento propuesto coincide con el que iOS proponía en esa fecha (usa el mismo fixture).
3. Decidir el futuro del repo Swift (mantener, archivar, o que consuma `shared` vía KMP/XCFramework — fuera del alcance de este plan).

---

## 6. Estrategia de pruebas (resumen)

| Capa | Cómo | Equivalente actual |
|---|---|---|
| Motor, modelos, backup, recordatorios, localización | `./gradlew :shared:jvmTest` (kotlin.test) + golden | `swift test` |
| Paridad con iOS | Fixtures de `golden/` comparados estructuralmente | — (nuevo) |
| Simulación de 16 semanas | `EngineSim sim` con `diff` contra Swift | `swift run EngineSim sim` |
| Compila la UI, tests de unidad Android | `./gradlew :androidApp:testDebugUnitTest` | `ios.yml` |
| Pantallas en 3 idiomas | UI tests + capturas | `screenshots.yml` |
| Traducciones completas | Job `translations` | `core.yml` `translations` |

---

## 7. Orden de entrega sugerido (PRs)

1. Fase 0 (repo Swift) — PR aparte, solo fixtures y exportador.
2. Fases 1–2 — esqueleto + modelos + backup.
3. Fase 3 — localización.
4. Fase 4 — motor (el PR más revisable: pedir revisión línea a línea contra Swift; puede dividirse por archivo).
5. Fase 5 — almacén.
6. Fases 6–7 — UI (un PR por pantalla grande).
7. Fases 8–10.

---

## 8. Decisiones abiertas (resolver antes o durante la Fase 1)

- ¿Opción B (KMP, recomendada), A (Android solo) o C (Compose Multiplatform)? El plan asume **B**.
- ¿Nombre de paquete? Propuesta: `com.momentum` (igual que el bundle iOS `com.momentum.app`); confirmar para Play Store.
- ¿Licencia? El ROADMAP del original la deja pendiente (💬 *Choose a license*); aplicarla igual en el repo nuevo.
- ¿Salud en la primera versión o después? Se recomienda **después** (Fase 8 puede aplazarse sin bloquear el corte).
- ¿Repositorio nuevo o rama/carpeta en `fitness-app`? Se asume repositorio nuevo para no mezclar Gradle con `project.yml`/XcodeGen.

## 9. Estimación orientativa

| Fase | Esfuerzo relativo |
|---|---|
| 0 Fixtures | S |
| 1 Esqueleto | S |
| 2 Modelos/backup | M |
| 3 Localización | M |
| 4 Motor + tests | **L** |
| 5 Almacén | S |
| 6 Tema/figuras | M |
| 7 Pantallas | **L** |
| 8 Recordatorios/Salud | M |
| 9 CI/docs | S |
| 10 Paridad | S |

S ≈ días, M ≈ 1–2 semanas, L ≈ 2–4 semanas de una persona; reducible si Codex avanza por fases en paralelo (el motor y la UI solo dependen de los modelos).
