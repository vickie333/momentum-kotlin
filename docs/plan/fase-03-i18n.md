# Fase 3 — Localización ✅

Plan maestro §5 Fase 3 y riesgo 6. Depende de: Fase 2.

## Estado

Las tablas Python compartidas se exportan completas a `data/i18n/strings.json` y `data/i18n/exercises.json` con `export_kotlin.py --strict` y `export_exercises_kotlin.py`. `generateMomentumData` incrusta ambos JSON en `shared`. `Loc` y `ExerciseText` ofrecen español y portugués brasileño con fallback a inglés; las claves de los modelos son literales extraíbles. `LocalizationTest` cubre idioma, marcadores, tablas, ejercicios y modelos. El job `translations` verifica la sincronización en CI.

## Trabajo trasladado

- Fase 4: tests de localización del motor `testEngineTextIsFullyTranslated` y `testSwapSuggestionsAreTranslated`.
- Fase 7: `Loc.date`, formato de fechas y detección del idioma del dispositivo en Android.
- Fase 8: `strings.xml` para el nombre de la app y textos de permisos de Health Connect.

## Validación

Ejecutar `python3 tools/i18n/export_kotlin.py --strict` y `python3 tools/i18n/export_exercises_kotlin.py`; revisar `:shared:jvmTest` y `:shared:iosSimulatorArm64Test` fuera del sandbox Codex.
