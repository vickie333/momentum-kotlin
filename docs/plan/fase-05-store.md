# Fase 5 — Almacén de estado ⬜

Plan maestro §5 Fase 5. Depende de: Fase 4. Origen: `fitness-app/Momentum/Store/` (`AppStore.swift`, `LaunchMode`).

## Alcance

1. `com.momentum.store.AppStore`: `StateFlow<PersonalData>` (`profile`, `sessions`, `weights`), guarda tras cada cambio; mismos ayudantes de plan/estadísticas que usan las vistas.
2. Interfaz `Storage { read(): String?; write(String) }` en `shared`; `InMemoryStorage` para tests; `FileStorage` en `androidApp/data` (`filesDir/momentum.json`, escritura atómica: temporal + rename).
3. `LaunchMode`: equivalentes de `-uiTesting`, `-demoData`, `-demoLanguage` vía extras de `Intent` en debug; nunca toca datos reales.
4. JSON corrupto → copia `.corrupt` y arranque vacío.

## Tests

Guardar → reabrir → mismo estado; JSON corrupto no borra datos; abrir un guardado iOS (`golden/backups/current.json` como `PersonalData`).

## Validación

`jvmTest` en verde.
