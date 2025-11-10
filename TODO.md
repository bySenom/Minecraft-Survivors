# Minecraft Survivors TODO

## Prioritätsskala
- P1 = Release-kritisch (vor erstem öffentlichen Release erledigen)
- P2 = Wichtige Qualitäts-/Spielerlebnisverbesserung (bald danach)
- P3 = Komfort / Nice-to-have / spätere Version

## Übersicht
Dies ist die aktive Aufgabenliste für das Kernspiel (Minecraft Survivors). Lobby-spezifische Aufgaben liegen in `lobby/TODO.md`.

---
Status-Update (2025-11-10)
- P1.2: Geschlossen — Glyphen-UX, Persistenz-Basics, Replace-Flow und CI-Checks implementiert.
- P1.3: In Arbeit — Fokus: QA, Balancing, Tests & Doku.

Erledigt (Code)
- RoundStats: per-round exports (JSON/CSV/HTML) + `exports/YYYY-MM-DD/` folder.
- RoundStats: `/msroundstats export|summary|show` + GUI skeleton implemented.
- RoundStats: player name mapping added to exports (JSON/CSV/HTML) for offline-friendly reports.
- RoundStats: lingering_void damage attribution implemented (ab_void_nova:lingering_void) via temporary metadata so DPS breakdown is accurate.
- RoundStats: `openfile` command now searches `exports/` recursively; command uses recursive export lookup for recent JSONs.
- Auto-report cleanup on plugin start for `roundstats_auto_*` files.
- Balancing config keys added and applied: crit cap, lifesteal cap, shield regen delay (tick-based fallback to seconds).
- README: RoundStats admin documentation added.

Kurz: Viele P1.3-Features wurden implementiert (siehe Erledigt). Unten stehen die verbleibenden, konkreten Tasks.

## Offene P1 (Release-kritisch) Tasks
Diese Tasks müssen vor Release adressiert werden.

1) QA & Bugfixes Endboss
   - Finales Balancing prüfen (Spawn Counts, Schäden, Phase-Trigger).
   - Visuelle Telegraphs und Cleanup unter Last verifizieren (inkl. Projectile/FallingBlock-Cleanup).
   - Smoke-tests: Boss-Spawn + Kill unter 1–8 Spieler-Szenarien (manuell/Remote).

2) Wellen / Continuous Mode Stabilität
   - Reproduzierbare QA-Szenarien für Continuous Mode erstellen; edge-cases (mid-run restart, player reconnect) prüfen.
   - Verify: alle laufenden Schedulers werden beim Stop gecancelt (no leaking runnables).

3) Tests & CI
   - Stabilisierung von `SurvivorPlayerTest` und zentralen CombatEngine-Tests (Integration / MockBukkit ggf.).
   - Sicherstellen, dass Remote CI (GitHub Actions) grün läuft (Build, Spotless, Tests).

4) Dokumentation & Release-Checklist
   - README: Getting Started, Config-Cheatsheet, neue Stats (erweitern).
   - Release-Checklist: smoke-tests, TPS-check, Endboss QA, DB/PlayerData Backup-Plan.

5) Balancing-Iteration (Konfigurierbar)
   - Tuning: Crit/Lifesteal/Shield Caps (grundsätzliche keys implementiert). Weitere Feintuning + defaults prüfen.

---

Priorität für den Sprint: 1 → QA/Smoke (Endboss) + Tests/CI, danach Dokumentation und Balancing-Tuning.

(Die Lobby-spezifischen TODOs sind in `lobby/TODO.md`.)
