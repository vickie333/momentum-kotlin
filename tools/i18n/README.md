# Traducciones para Kotlin

Las tablas `strings_*.py` y `exercises.py` son la fuente de verdad. Edítalas y regenera los JSON con:

```bash
python3 tools/i18n/export_kotlin.py --strict
python3 tools/i18n/export_exercises_kotlin.py
```

Los archivos resultantes son `data/i18n/strings.json` y `data/i18n/exercises.json`. `--strict` falla si falta una clave usada en Kotlin, si los idiomas tienen claves distintas o si cambian los marcadores `{0}`, `{1}`, etc. El job CI `translations` comprueba que `data/i18n` está sincronizado con las tablas.

`export.py`, `export_exercises.py` y `extract.py` son herramientas del proyecto Swift; no se usan para generar los JSON de Kotlin.

## Rules

Las claves deben ser cadenas literales en `L("…")`, `Lp(n, "…", "…")` o `Loc.text("…")` (y en la llamada interna `text("…", listOf(…))`). No se permiten plantillas. `--strict` falla ante llamadas con claves no literales, traducciones es/pt faltantes, marcadores de posición distintos, valores vacíos o redefiniciones conflictivas.
