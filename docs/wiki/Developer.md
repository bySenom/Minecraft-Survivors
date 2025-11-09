# Developer / Beitragende

Projektstruktur (Kurz)

- `src/main/java/org/bysenom/minecraftSurvivors/manager` — Kernlogik (GameManager, SpawnManager...)
- `src/main/java/org/bysenom/minecraftSurvivors/gui` — GUI/Inventar‑UIs
- `src/main/java/org/bysenom/minecraftSurvivors/listener` — Event‑Listener
- `src/main/java/org/bysenom/minecraftSurvivors/glyph` — Glyph‑Catalog

Lokales Entwickeln

- Formatieren: `./gradlew spotlessApply`
- Build: `./gradlew clean build`
- Tests: `./gradlew test`

Code‑Style & PR

- Bitte `spotlessApply` vor jedem Commit ausführen.
- PRs: Kleiner Umfang, erklärender Description, ggf. Screenshots/Logs.

Debug Tipps

- Verwende `plugin.getLogger().info(...)` an relevanten Stellen.
- Bei Problemen: starte den Server lokal, reproduziere das Problem und hänge `logs/latest.log` an den PR/Issue.
