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

## Ergänzungen: Fähigkeiten / Glyphen System (Empfehlungen & Tasks)
Diese Sektion fasst Änderungen zusammen, die ich für Release-kritisch bzw. wichtig halte, inklusive konkreter Quick‑Wins und Architekturvorschläge.

### A. Release‑kritisch (P1) — sofort umsetzen
- Einheitliche Attribution: Vor jedem Schaden, der durch eine Fähigkeit/Glyphe verursacht wird, muss `ms_ability_key` gesetzt werden (oder bei Projektilen persistent auf dem Projectile). Damit werden RoundStats und DPS‑Metriken korrekt zugeordnet.
- Glyph‑Visibility & Reporting: Jede Glyphe, die proct (auch wenn kein Schaden entsteht), muss `glyphProcNotify(...)` und `RoundStatsManager.recordSourceObserved(...)` aufrufen, damit Reports vollständig sind.
- Robustere FX: Partikelerzeugung über `ParticleUtil.spawnSafeThrottled(...)` mit Fallbacks; fehlende FX (z. B. Genkidama) prüfen und sicherstellen, dass zentral sichtbare Partikel/Sound abgespielt werden.
- Config‑Driven Tuning: Alle Basis‑Werte (Damage, Cooldowns, Multipliers) müssen via `config.yml` überschreibbar sein (Release‑Konfig: `skills.*` keys). Kein harter Code‑Tuning kurz vor Release.

### B. Kurzfristig (P1/P2) — Quick‑Wins (low risk)
- Implementiere folgende Glyph‑Procs, die momentan nur als StatModifier registriert sind:
  - `ab_ranged:headshot` — Chance auf doppelten Schaden beim Treffer; `ms_ability_key` = `ab_ranged:headshot` + glyph notify.
  - `ab_fire:inferno`, `ab_fire:combust`, `ab_fire:phoenix` — Inferno verlängert Branddauer/AoE, Combust periodischer Burst, Phoenix = On‑Kill Explosion.
  - `ab_heal_totem:pulse` — zusätzliche Heilpulses bei Totem‑Aktivierung.
- Standard Single‑Target Multiplier: bereits implementiert; expose `skills.single_target.multiplier` in default config und setze Default auf 1.20 (done).

### C. Mittelfristig (P2) — Architektur & Sauberkeit
- Trennung von StatModifier vs Proc‑Behavior:
  - Führe ein `GlyphBehavior` Interface (onHit/onTick/onKill/onActivate) ein und registriere Implementierungen in `GlyphCatalog` oder einer neuen Registry.
  - `SkillManager` ruft die Behaviors zentral statt verstreuter `if glyphs.contains(...)` Blöcke.
- Projectile Attribution:
  - Wenn ein Ability ein Projectile spawnt, setze PersistentData/Metadata auf dem Projectile (z. B. NamespacedKey "ms_ability") und handle ProjectileHit einheitlich in `SkillListener`.
- RoundStats Erweiterung:
  - `recordDamage(sourceKey, ownerUuid, amount)` implementieren (per-source/per-owner breakdown).
  - Exports im `exports/` Verzeichnis halten; beim Serverstart alte automatische Reports coden (aber die exports behalten).

### D. Langfristig / Post‑Release (P3)
- GUI/Analytics: Admin‑GUI mit Chart‑Mini‑SVGs (inline) für schnelle Visualisierung (Total DMG, Breakdown per glyph/ability, Kills, Coins, Lootchests).
- Telemetry & A/B Testing (Opt‑in): Experimentelle Balance‑Änderungen sammeln und auswerten.
- Unit/Integration Tests (MockBukkit) für Procs, Projektile, RoundStats Attribution.

### E. Empfohlene Config‑Keys (für `config.yml` defaults)
- skills.single_target.multiplier: 1.20
- skills.genkidama.chance: 0.05
- skills.genkidama.visual-enabled: true
- skills.fire.inferno.duration-mult: 1.25
- heal_totem.aegis.proc-chance: 0.12
- glyphs.enable-visuals: true
- exports.dir: "exports"
- stats.clear-on-start: true

### F. Quick‑Wins: konkrete Schritte, die ich sofort umsetzen kann
- Headshot: `shootRangedProjectile` erweitert: wenn Spieler `ab_ranged:headshot` hat, rolle Chance → doppelter Schaden, glyphProcNotify.
- Fire glyphs: `runWFire` erweitert: `inferno` erhöht FireTicks & AoE pulse, `combust` macht periodische Burst (every Nth tick).
- Projectile metadata: setze `ms_ability_key` auf Projectile (PersistentData) für saubere Attribution.

### G. Tests & KPIs (Balancing)
- RoundStats Metriken pro Runde:
  - totalDamageBySource, damagePerUse, killsPerUse, coinsPerUse, lootchestsPickedUp
  - normalized: damagePerMinute, damagePerPlayerLevel
- Zielwerte (Beispiel für early game): Starter‑Abilities tragen jeweils ~20–40% Team‑Damage; passen, wenn Abweichung > ±25%.

### H. Release‑Workflow (Empfohlen)
1. Implementiere P1 Fixes + Quick‑Wins.
2. Erstelle default config mit neuen keys und README‑Eintrag.
3. Testlauf: 3 Durchläufe mit 1, 3, 6 Spielern; sammle RoundStats exports.
4. Quick‑Balance Pass basierend auf Stats (1–3 Iterationen), dann Release‑Candidate.


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

## Nächste Entscheidung / Auswahl für nächsten Sprint
Wähle 1–2 P1/P2 Features für den nächsten Sprint (Empfehlung: Endboss QA + Tests/CI oder Endboss QA + Balancing-Tuning).

### Vorschlag: Was ich jetzt sofort implementieren kann (Quick‑Wins). Wähle eine Option:
- Option A (Empfohlen, schnell): Implementiere `ab_ranged:headshot`, `ab_fire` minimal procs (inferno/combust), Projectile‑metadata attribution; commit lokal, du testest, ich analysiere RoundStats.
- Option B (Konservativ): Du testest die bereits gemachten Buffs (+20% in `AbilityCatalog`, single-target multiplier 1.20). Ich warte auf RoundStats Exporte und mache dann zielgenaue Anpassungen.
- Option C (Riesiger Refactor): Beginn des Refactors (GlyphBehavior + RoundStats per‑owner breakdown). Längerer Rework, sinnvoll vor major release.

Bitte antworte mit A, B oder C — ich setze die Option dann sofort um und committe lokal (nicht push).
