# Swift engine golden fixtures

Regenerate from the repository root:

```sh
swift run EngineSim golden
```

The export uses English, UTC, 5 January 2026 at 09:00 for profile and session dates, a fixed random seed, and fixed UUIDs. JSON keys are sorted. Each case contains `input` and `expected` so the Kotlin implementation can run the same call and compare its result structurally. Optional values are omitted unless the Swift result contains an explicit null. Compare doubles within `1e-9`; every other value must match.

Files:

- `plans.json`: 36 profile and week combinations, with full exercise prescriptions and explanations.
- `sim16w.json`: session-by-session workouts, resulting profiles, messages and deload status for the two 16-week athletes and the 12-week advanced athlete in `EngineSim sim`.
- `sim16w.txt`: the exact English, UTC table printed by `EngineSim sim`.
- `alternatives.json`: all five swap reasons across 24 planned exercises, including pain areas.
- `progression.json`: first use, successful top range, missed minimum and held load decisions for five exercises and six profiles.
- `assessment.json`, `weekly_review.json`, `reminders.json`: fixed-date engine cases.
- `backups/current.json`: an actual v1 `Backup.export` file with a completed workout and weight entry. `legacy.json` removes newer optional profile fields; `future.json` has a newer version and must be rejected.

`EngineSim sim` previously printed `Set(notes)` in arbitrary order, so its table varied between processes. The CLI now sorts those notes. App engine decisions are unchanged. The CLI `sim` and `golden` modes set UTC and English before building profiles; other modes retain their prior settings.

Check repeatability after regeneration by comparing hashes of all files from two runs. `sim16w.txt` should also be byte-identical to `swift run EngineSim sim` output.
