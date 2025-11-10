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

Erledigt (Kurz)
- RoundStats core: per-round exports (JSON/CSV/HTML) in `exports/YYYY-MM-DD/`.
- RoundStats commands: `/msroundstats export|summary|show` and a GUI skeleton.
- RoundStats: player name mapping added to exports for offline-friendly reports.
- RoundStats: lingering_void damage attribution implemented (owner attribution -> `ab_void_nova:lingering_void`).
- RoundStats: auto-report cleanup on plugin start (`roundstats_auto_*` files cleared).
- RoundStats: exports lookup (`openfile` command) now searches `exports/` recursively.
- Balancing config keys added (crit cap, lifesteal cap, shield regen delay) and applied where relevant.
- README: RoundStats admin documentation added.

Kurz: Viele P1.3-Features rund um RoundStats wurden implementiert; verbleibende P1-Aufgaben sind unten.

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
   - Tuning: Crit/Lifesteal/Shield Caps (defaults prüfen). Weitere Feintuning + defaults prüfen.

---

## P2 (Wichtig, nach erstem Release)
- VIP-Prio
- /queue gui (Lobby) – bleibt in `lobby/TODO.md`
- messages.yml (Mehrsprachigkeit)
- Webhook/Externes Logging
- Performance/Profiler Hook
- Konsolen-Command für Mass-Clear

## P3 (Nice-to-have / später)
- Weighted Fair Queue / Segmentierung
- Historie zuletzt zugelassener Spieler (/queue history)
- Visuelle Admission-Effekte (konfigurierbar)
- Freundeslisten-Bevorzugung (optional)
- API Events (QueueJoinEvent, QueueLeaveEvent, QueueAdmitEvent)
- Web-Dashboard / REST Status
- Priority Re-Entry für zuletzt gespielte

---

## Tracking / Metriken (technisch)
- Wartezeitstart = Zeitstempel beim `join()`
- Wartezeitende = Zeit bei `admitNext()`
- avgWaitTime = Summe / Anzahl

---

## Qualitätssicherung (QS)
- Test: Offline während admission, Persistenz-Datei korrupt, Rejoin-Cooldown.

---

## Nächste Entscheidung
Wähle 1–2 P1/P2 Features für den nächsten Sprint (Empfehlung: Endboss QA + Tests/CI oder Endboss QA + Balancing-Tuning).
