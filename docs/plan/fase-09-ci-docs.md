# Fase 9 — Herramientas, CI y documentación 🟡

Plan maestro §5 Fase 9. Se avanza de forma incremental junto a cada fase.

## Hecho

- `core.yml`: `:shared:jvmTest` + `:androidApp:assembleDebug` en push y PR.

## Pendiente

- [ ] `EngineSim` en `jvmMain` (lo hace la Fase 4i).
- [ ] Job `translations` (tras Fase 3): regenerar con `tools/i18n` y `git diff --exit-code`.
- [ ] `android.yml`: build + `testDebugUnitTest` + tests instrumentados (Gradle Managed Devices).
- [ ] `screenshots.yml`: capturas en/es/pt como artefacto (no se commitean).
- [ ] Docs: `docs/ARCHITECTURE.md`, `docs/ENGINE.md` (copiado + diferencias), `CHANGELOG.md`, guía para añadir pantallas/traducciones/ejercicios, compilación y firma; ampliar `README.md`.
- [ ] Script de instalación: `./gradlew :androidApp:installDebug`.
- [ ] Licencia (decisión abierta).

## Validación

Clon limpio compila y pasa todos los jobs; añadir un ejercicio en `data/exercises.json` se refleja en ambas apps tras regenerar.
