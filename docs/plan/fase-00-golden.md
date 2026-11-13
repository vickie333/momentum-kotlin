# Fase 0 — Fixtures de oro ✅

Plan maestro §5 Fase 0.

## Hecho

- `fitness-app`: comando `swift run EngineSim golden` y commit `34637d9` "Golden fixtures para la migración a Kotlin".
- Fixtures en `fitness-app/docs/golden/`, copiados a `shared/src/commonTest/resources/golden/`:
  - `plans.json` (36 casos), `sim16w.json` + `sim16w.txt`, `alternatives.json` (5 motivos × 24 ejercicios), `progression.json`, `assessment.json`, `weekly_review.json`, `reminders.json`.
  - `backups/current.json`, `legacy.json`, `future.json`.
- Entradas fijas: inglés, UTC, 2026-01-05 09:00, semilla y UUID fijos, claves JSON ordenadas.
- Hallazgo de no determinismo: `EngineSim sim` imprimía `Set(notes)` en orden arbitrario; ahora se ordenan en la CLI. Las decisiones del motor de la app no cambian. Ver `golden/README.md`.

## Pendiente

- [ ] El commit `34637d9` está en la rama `claude/fervent-feynman-74fe50` de `fitness-app`, **1 commit por delante de origin** y no en `main`. Hacer push y abrir PR en `fitness-app` (plan maestro §7, PR 1).
- [ ] Anotar el resultado de `swift test` (170+ tests) como línea base en el PR.
