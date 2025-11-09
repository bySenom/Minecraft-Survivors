# Minecraft Survivors TODO

## Prioritätsskala
- P1 = Release-kritisch (vor erstem öffentlichen Release erledigen)
- P2 = Wichtige Qualitäts-/Spielerlebnisverbesserung (bald danach)
- P3 = Komfort / Nice-to-have / spätere Version

## Übersicht
Dies ist die Aufgabenliste für das Kernspiel (Minecraft Survivors). Das LobbySystem hat eine eigene TODO unter `lobby/TODO.md`.

---
Status-Update (2025-11-09)
- Erledigt (Code): MAX_HEALTH Reset; CombatEngine zentralisiert; Endboss: Meteor-Cleanup, Telegraphs, ArmorStand-Name Fixes; Persistenz: `PlayerDataManager` & `SurvivorPlayer` (Grundlegende Felder + save/load) implemented; viele Particle-Aufrufe wurden auf `ParticleUtil` umgestellt.
- In Arbeit: FX-Throttling Rate-Limiter Feintuning; Wellen/Continuous Stabilität (Monitor/cleanup für Runnables); Dokumentation (README/Docs).

## P1 (Release-kritisch)

### 1. Endboss (100% Enrage) fertigstellen
Beschreibung: Custom Boss mit Phasen (P1/P2/P3), Mechaniken (Meteor Barrage, Lightning Beam, Summons), klare Telegraphs.
Akzeptanzkriterien (aktuell offen/teilweise):
- Boss spawnt verlässlich, Phasenwechsel funktionieren.  # (Teilweise implementiert: Phasen & Wechsel-Logik vorhanden)
- Angriffsschaden/Tempo pro Phase balanciert (OFFEN: Balancing-Feinschliff).
- Balancing & Final QA (OFFEN).

### 2. Neue Stats in UI/Progression verfügbar
Beschreibung: LevelUp/Shop/Loot bieten und zeigen neue Stats (Max Health, HP Regen, Shield, Armor, Evasion, Lifesteal, Thorns, Crit Chance, Crit Damage, Projectile Count, Attack Speed, Projectile Bounce, Size, Duration, Damage vs. Elites/Bosses, Knockback, Jump Height, XP Gain, Elite Spawn Increase, Powerup Multiplier).
Akzeptanzkriterien:
- Anzeigen im LevelUp-/Shop-Menü.
- Persistenz in PlayerData.  # (Grundlegende Persistenz implementiert; UI noch offen)
- Tooltips kurz und verständlich.
- Statistics after each round: Chat summary + GUI mit Details (Damage breakdown, Kills, Coins, Lootchests).

### 3. Performance & FX-Throttling
Beschreibung: FX abhängig von Distanz/FX-Setting drosseln.
Akzeptanzkriterien:
- Globaler Regler / Spieler-FX Toggle ergänzt um Rate-Limiter.  # (Basis: `ParticleUtil.shouldThrottle` implementiert — Feintuning offen)
- Große Kämpfe verursachen keine Lags.

### 4. Persistenz & Reset der neuen Stats
Beschreibung: Sicheres Laden/Speichern aller neuen Stats + Reset bei Run-Beginn.
Akzeptanzkriterien:
- PlayerData migriert (Backward-Compat).  # (Grundlegende Migration & Backwards-Compat-Handling implementiert)
- Start eines Runs setzt temporäre Boni korrekt zurück.  # (softReset / softResetPreserveSkills vorhanden und genutzt)
- Bessere Glyphen-Logik: wenn alle Slots belegt, dann Levelup der Fähigkeit; nur Glyphen anzeigen, die noch nicht gesockelt sind. (OFFEN)

### 5. Stabilität Wellen/Continuous Mode
Beschreibung: Keine Zombie-Tasks; Start/Stop räumt sauber auf.
Akzeptanzkriterien:
- Alle BukkitRunnable werden bei Stop gecancelt.  # (Teilweise: viele Tasks gecancelt; Audit & gezielte Cancels noch offen)
- Continuous und Wave-Mode laufen ohne Deadlocks.
- Spieler sollen Solo und im Party-Modus spielen können.

### 6. Doku (README/Docs)
Beschreibung: Installation, Konfiguration, neue Stats, FX-Commands (/fx …) kurz dokumentiert.
Akzeptanzkriterien:
- README Abschnitt „Getting Started" + „Config cheatsheet".

## P2 (Wichtig, nach erstem Release)

### 7. Balancing-Pass
Beschreibung: Schadensskalierung, Crit Caps, Lifesteal Grenzen, Shield-Regen-Delay.
Akzeptanzkriterien:
- Konfigurierbare Caps (ConfigUtil Keys), dokumentiert.

### 8. Elite-/Boss-Damage Multipliers + Anzeige
Beschreibung: Separate Multiplikatoren gegen Elites/Bosse, UI Anzeige.
Akzeptanzkriterien:
- Stat wirkt in CombatEngine; HUD zeigt kurzen Hinweis/Symbol.

### 9. Erweiterte FX (optional abschaltbar)
Beschreibung: Fancy Visuals für Shaman/Holy/Venom/Ranged/Fire ausbauen, inkl. Toggle & Distanzcheck.

### 10. Test-Kommandos bündeln (/msdebug)
Beschreibung: Einheitlicher Debug-Einstieg: FX-Tests, Spawn, Stats-Print.

### 11. Analytics/StatsMeter
Beschreibung: Laufzeit, Kills, Schaden/Heilung, meistgenutzte Abilities.

### 12. Presets (easy/normal/hard)
Beschreibung: Konfig-Presets für schnelle Server-Anpassung.

## P3 (Nice-to-have / später)

### 13. Modulare Boss-Fähigkeiten (Katalog + Weights)
### 14. Seasons/Meta-Progression Ausbau
### 15. Cloud-Speicher für PlayerData (optional)
### 16. Mod-/Resourcenpack für FX (höhere Qualität)

## Qualitätssicherung (QS)
- Unit/Integration Tests: CombatEngine Kernfälle + Persistence.
- Manuelle Tests: Multi-User, Disconnect/Reconnect, FX Performance.
- Profiling: Ticks-per-Second unter Load > 18 TPS.

---
## Umsetzungsvorschlag Reihenfolge
1. Endboss Finalisierung
2. Performance/FX Throttle
3. Persistenz/Reset
4. Doku + Presets
5. Balancing

Release, wenn P1 abgeschlossen.
