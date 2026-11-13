# Fase 4 — Motor ⬜ (fase crítica)

Plan maestro §5 Fase 4, riesgos 1–4. Depende de: Fases 2 y 3. Origen: `fitness-app/Momentum/Engine/`.

## Orden (hojas → raíz), un PR o sub-handoff por bloque

| # | Archivo | Golden / tests |
|---|---|---|
| 4a | `TrainingCalendar` (kotlinx-datetime; `weekStart`, `weeksBetween`, `daysBetween`, `sameDay`) | tests de DST y semana que cruza año |
| 4b | `Periodization`, `Progression` | `ProgressionTests`, `progression.json` |
| 4c | `Assessment` | `AssessmentTests`, `assessment.json` |
| 4d | `Alternatives` | `alternatives.json` |
| 4e | `AdaptiveEngine` | `AdaptationTests` |
| 4f | `PlanGenerator` (721 líneas) | `PlanTests` (606), `InvariantTests`, `plans.json` |
| 4g | `WeeklyReview`, `Reminders` (solo `plan`, pura) | `WeeklyReviewTests`, `ReminderTests`, `weekly_review.json`, `reminders.json` |
| 4h | `DemoData` | `DemoDataTests`, `ExerciseProgressTests`, `HistoryTests` |
| 4i | `EngineSim` en `jvmMain` (`plans`, `sim [-v]`, `alts`) | `sim16w.json`, `diff` contra `sim16w.txt` |

## Reglas de portado

- Mismos nombres, firmas y estructura para comparar línea a línea.
- Cada `.rounded()` → `roundHalfAwayFromZero`.
- Cada `Dictionary`/`Set` iterado → orden determinista con desempate escrito en comentario.
- La fecha siempre es parámetro; nunca `Clock.System.now()` dentro del motor.
- Diferencias inevitables → `docs/ENGINE.md`.
- Portar `TestSupport` primero.

## Validación (aceptación)

- 100 % de tests portados en verde.
- 100 % de fixtures idénticos (tolerancia `1e-9` solo en `Double`), textos en `en`.
- `EngineSim sim` Kotlin = `sim16w.txt` byte a byte.

Effort sugerido: high por bloque.
