# Fase 7 — Pantallas ⬜

Plan maestro §5 Fase 7. Depende de: Fases 5 y 6. Origen: `fitness-app/Momentum/Views/**`. Un PR/handoff por sub-fase.

| # | Pantalla | Origen |
|---|---|---|
| 7a | Navegación raíz: `NavHost` + barra inferior (Entrenamientos, Explorar, Progreso, Ajustes), decisión onboarding/app; orientación vertical | `RootView` |
| 7b | Onboarding (objetivo, historial, chequeo de fuerza, días, duración, equipo + mancuerna más pesada, áreas, bajo impacto, datos corporales, vista previa, idioma) | 620 líneas |
| 7c | Entrenamientos: lista, detalle, `SwapSheet`, `ActiveWorkoutView` (series, descanso, temporizador, sensación) | 645 líneas la activa |
| 7d | Explorar: rápidos (10 min, sin equipo, movilidad), vídeos (`curatedVideos`, búsqueda YouTube con `ACTION_VIEW`) | `ExploreView` |
| 7e | Progreso: calorías/minutos, volumen por músculo, 1RM y récords, peso, racha, gráfico por ejercicio (`Canvas`), revisión semanal, historial | `ProgressTabView` 487, `HistoryView` |
| 7f | Ajustes: idioma, recordatorios, Salud, exportar/importar (`CreateDocument`/`OpenDocument`), CSV, borrar datos | `SettingsView` 407, `DataFileDocument` |

## Reglas

Todo texto por `L(...)`; `contentDescription`; toque ≥ 48 dp; escala de fuente; claro/oscuro.

## Validación por pantalla

UI test Compose con modo demo + capturas en en/es/pt. Recorrido manual (argent, emulador): onboarding → primer entrenamiento → terminar → progreso → exportar y restaurar.
