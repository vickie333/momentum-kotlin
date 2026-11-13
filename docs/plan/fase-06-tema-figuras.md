# Fase 6 — Tema, componentes y figuras animadas ⬜

Plan maestro §5 Fase 6, riesgo 9. Depende de: Fase 2 (modelos `Motion`). Puede ir en paralelo con Fase 4.
Origen: `fitness-app/Momentum/Design/` (`Theme.swift`, `Components.swift` 194, `FigureView.swift`), `Assets.xcassets/AccentColor`.

## Alcance

1. `androidApp/.../ui/theme/Theme.kt`: Material 3 claro/oscuro con colores y tipografía de `Theme.swift`.
2. `ui/components/Components.kt`: port de `Components.swift`.
3. `ui/components/FigureView.kt`: `FigureRenderer` → `DrawScope`; 14 valores por fotograma, interpolación por `dur`, `held`, props (`bench`, `chair`, `bar`, `dumbbell`, `barbell`, cables, `plate`), misma transformación de `bounds`. Estático si `ANIMATOR_DURATION_SCALE == 0`.
4. Pantalla de galería de depuración con los 75 ejercicios.

## Validación

Revisión visual de la galería contra `fitness-app/docs/exercise-animations.png` (sin diferencias de forma). Usar argent (emulador Android) para capturas.
