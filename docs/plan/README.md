# Estado de la migración a Kotlin

Plan maestro: [`../PLAN-MIGRACION-KOTLIN.md`](../PLAN-MIGRACION-KOTLIN.md). Cada fase tiene su subplan aquí; cuando una fase vaya a implementarse, su subplan se convierte en un plan de `feature-plan` autocontenido y se manda a Codex con `/handoff`.

Última revisión: 2026-11-13.

| Fase | Subplan | Estado | Evidencia |
|---|---|---|---|
| 0 Fixtures de oro (repo Swift) | [fase-00-golden.md](fase-00-golden.md) | ✅ Hecha (falta push en `fitness-app`) | commit `34637d9` en `fitness-app`; fixtures copiados a `shared/src/commonTest/resources/golden/` |
| 1 Esqueleto Gradle/KMP | [fase-01-esqueleto.md](fase-01-esqueleto.md) | ✅ Hecha | `./gradlew :shared:jvmTest :androidApp:assembleDebug` en verde (2026-11-13) |
| 2 Modelos y serialización | [fase-02-modelos.md](fase-02-modelos.md) | ⏳ Siguiente | solo `exercises.json`/`motions.json` copiados a recursos |
| 3 Localización | [fase-03-i18n.md](fase-03-i18n.md) | ⬜ Pendiente | `tools/i18n/` copiado, sin exportadores Kotlin |
| 4 Motor | [fase-04-motor.md](fase-04-motor.md) | ⬜ Pendiente | — |
| 5 Almacén de estado | [fase-05-store.md](fase-05-store.md) | ⬜ Pendiente | — |
| 6 Tema, componentes, figuras | [fase-06-tema-figuras.md](fase-06-tema-figuras.md) | ⬜ Pendiente | — |
| 7 Pantallas | [fase-07-pantallas.md](fase-07-pantallas.md) | ⬜ Pendiente | `MainActivity` muestra solo "Momentum" |
| 8 Recordatorios y Salud | [fase-08-plataforma.md](fase-08-plataforma.md) | ⬜ Pendiente | — |
| 9 Herramientas, CI, docs | [fase-09-ci-docs.md](fase-09-ci-docs.md) | 🟡 Parcial | `core.yml` existe; faltan `android.yml`, `screenshots.yml`, `translations`, docs |
| 10 Paridad y corte | [fase-10-paridad.md](fase-10-paridad.md) | ⬜ Pendiente | — |

## Dependencias

```
2 Modelos ──┬─> 3 i18n ──> 4 Motor ──> 5 Store ──> 7 Pantallas ──> 10 Paridad
            └─> 6 Tema/figuras ─────────────────────┘
                                   8 Plataforma (tras 5; Salud aplazable)
9 CI/docs: incremental en cada fase
```

El motor (4) y el tema/figuras (6) pueden avanzar en paralelo en cuanto existan los modelos (2).

## Decisiones abiertas (del plan maestro §8)

- Opción B (KMP + Compose Android): **adoptada de hecho** — `shared` ya declara `android`, `jvm`, `iosArm64`, `iosSimulatorArm64`.
- Paquete: `com.momentum` / `applicationId = com.momentum.app` — **adoptado**, confirmar antes de Play Store.
- Licencia: pendiente.
- Salud en v1: recomendado aplazar (Fase 8b).
- Repo nuevo: **adoptado** (`momentum-kotlin`).
