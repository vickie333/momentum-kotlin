# Fase 3 — Localización ⬜

Plan maestro §5 Fase 3 y riesgo 6. Depende de: Fase 2.

## Estado

`tools/i18n/` copiado (`extract.py`, `export.py`, `export_exercises.py`, `strings_*.py`, `exercises.py`) — genera Swift. No hay exportadores Kotlin ni código `i18n` en `shared`.

## Alcance

1. `com.momentum.i18n`: `AppLanguage { en, es, pt }`, `detect(preferred: List<String>)`, `Loc.language` (idioma propio de la app), `L(key, vararg args)` con `{0}`, `{1}` y fallback a inglés. La clave es la frase en inglés.
2. Nuevos `tools/i18n/export_kotlin.py` y `export_exercises_kotlin.py`: generan JSON en `shared/src/commonMain/resources/i18n/` (preferible a `.kt` por límite de tamaño de método JVM) desde las mismas tablas; conservar `--strict`.
3. Interfaz `DateFormatter` en `shared` (`"EEEE"`, `"MMMMd"`, `"jm"`); implementación Android con `DateFormat.getBestDateTimePattern` y `Locale` `es-419` / `pt-BR`; implementación JVM para tests.
4. `strings.xml` solo para lo que exige el manifest (nombre de la app, textos de permisos de Salud) — equivalente a `InfoPlist.strings`.
5. Conectar las propiedades de modelos que en Fase 2 quedaron devolviendo la clave inglesa.

## Tests

Portar `LocalizationTests`: completitud de traducciones y placeholders coherentes entre idiomas.

## Validación

`jvmTest` en verde; job CI `translations` regenera y falla con `git diff --exit-code` si las tablas están desactualizadas (ver Fase 9).
