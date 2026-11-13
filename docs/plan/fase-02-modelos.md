# Fase 2 — Modelos y serialización ✅

Plan maestro §5 Fase 2 y riesgo 5. Origen: `~/Documents/github/fitness-app/Momentum/Models/`.

## Alcance

Paquete `com.momentum.model` en `shared/src/commonMain/kotlin/`. Nada de `java.*` ni `android.*`.

1. **`Models.swift` (661 líneas)** → `Models.kt`
   - Enums `FitnessLevel`, `Goal`, `Equipment`, `Sex`, `BodyArea`, `TrainingHistory`, `RecentFrequency`, … con `@SerialName` = `rawValue` de Swift.
   - Enums con raw `Int` (`TrainingHistory`, `RecentFrequency`) → serializador propio que escribe número.
   - `UserProfile` (con lectura tolerante de guardados viejos: valores por defecto en todo campo nuevo), `WorkoutSession`, `WeightEntry`, `Workout`, etc.
   - Propiedades con `L(...)` → funciones/extensiones (dependen de Fase 3; dejar la firma y devolver la clave en inglés mientras tanto).
   - SF Symbols → `enum IconKey` semántico.
2. **`Motion.swift`, `ExerciseMeta.swift`, `ExerciseLibrary.swift`** → decodificar `exercises.json` y `motions.json` de `commonMain/resources` (lector de recursos multiplataforma: `expect/actual` o generar constantes; no usar `ClassLoader` en common).
3. **`SessionSummary.swift`, `ExerciseProgress.swift`** → agrupación por día con orden determinista (riesgo 3).
4. **`Backup.swift`** → `PersonalData`, `BackupFile` (`format = "momentum-backup"`, `currentVersion = 1`), `BackupError { NotABackup, NewerVersion(n) }`, exportación CSV.
   - `Json { ignoreUnknownKeys = true; explicitNulls = false }`, con tolerancia campo a campo solo en `UserProfile`.
   - Serializador de `Instant` ISO-8601 con `Z` y **sin** fracciones de segundo.
5. Utilidad `roundHalfAwayFromZero` (riesgo 4) en `shared`, con tests.

## Tests

- Portar `BackupTests` y los tests de modelos de `CoreTests` (`fitness-app/MomentumTests/`).
- Golden: `golden/backups/current.json` y `legacy.json` se leen y re-escriben con igualdad JSON estructural; `future.json` lanza `NewerVersion`.
- `ExerciseLibrary` carga los 75 ejercicios y todas sus animaciones.

## Validación

`./gradlew :shared:jvmTest` en verde con los tests anteriores; `:androidApp:assembleDebug` sigue compilando.

## Notas para el handoff

Effort sugerido: medium. Convertir este subplan en plan autocontenido con `feature-plan` (Codex no ve `fitness-app` salvo por ruta; incluir rutas absolutas a los archivos Swift de origen).

## Resultado y desviaciones

- Catálogos de `data/*.json` embebidos por `generateMomentumData`; modelos, backup y CSV portados a `shared`.
- SF Symbols conservados como claves `String` hasta que Android los consuma en fase 6.
- `ExerciseProgress` pasa a fase 4h porque depende de `Progression.estimatedOneRepMax`.
- Los UUID se guardan como `String` para preservar exactamente los valores de iOS.
- Decodificación estricta como en Swift salvo `UserProfile` (tolerante campo a campo).
- `formatFixed` replica el redondeo de C (`%.nf`, empates al par sobre el valor binario exacto).
- `fitness-app` es externo y se usa solo como referencia de lectura.
