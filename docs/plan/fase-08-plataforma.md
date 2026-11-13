# Fase 8 — Recordatorios y Salud ⬜

Plan maestro §5 Fase 8, riesgos 7–8. Depende de: Fase 5 (y 7f para la UI de ajustes).

## 8a Recordatorios (v1)

- `androidApp/reminders/ReminderScheduler`: notificaciones locales en días de entrenamiento según `Reminders.plan` (de `shared`), título/cuerpo localizados.
- Canal silencioso; `POST_NOTIFICATIONS` pedido solo al activar; alarma inexacta aceptable.
- Reprogramar en `BOOT_COMPLETED` y al cambiar el perfil.
- Validación: recordatorio disparado con `adb` en emulador.

## 8b Salud (aplazable tras el corte)

- `shared/health/HealthGateway`: `requestAuthorization`, `saveWorkout`, `saveWeight`, `readSteps`, `readActiveEnergy`.
- `androidApp/health/HealthConnectGateway` con `HealthConnectClient`; `getSdkStatus`; permisos en manifest; pantalla de justificación de privacidad. Sin Health Connect → ocultar sección sin error.
- `PRIVACY.md` para Android.
- Validación: guardar entrenamiento y verlo en Health Connect en emulador.
