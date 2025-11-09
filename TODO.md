# Minecraft Survivors TODO

## Prioritätsskala
- P1 = Release-kritisch (vor erstem öffentlichen Release erledigen)
- P2 = Wichtige Qualitäts-/Spielerlebnisverbesserung (bald danach)
- P3 = Komfort / Nice-to-have / spätere Version

## Übersicht
Dies ist die Aufgabenliste für das Kernspiel (Minecraft Survivors). Das LobbySystem hat eine eigene TODO unter `lobby/TODO.md`.

---
Status-Update (2025-11-09)
- Erledigt: MAX_HEALTH Reset; CombatEngine zentralisiert.
- Endboss (100% Enrage): Grundgerüst aktiv – Meteor Block Cleanup noch OFFEN.
- FX-Throttling: TEILWEISE (Spieler-FX Toggle vorhanden), Rate-Limiter noch OFFEN.
- Persistenz/Reset neuer Stats: TEILWEISE (Speicher vorhanden), Reset-Logik für neue Stats konsolidieren OFFEN.
- Wellen/Continuous Stabilität: LAUFEND; Monitor für Zombie-Tasks noch OFFEN.
- Doku: OFFEN.

## P1 (Release-kritisch)


### 1. Endboss (100% Enrage) fertigstellen
Beschreibung: Custom Boss mit Phasen (P1/P2/P3), Mechaniken (Meteor Barrage, Lightning Beam, Summons), klaren Telegraphs.
Akzeptanzkriterien:
- Boss spawnt verlässlich, Phasenwechsel funktionieren.
- Telegraphs (Particles/Sounds) + Angriffsschaden/Tempo ausgewogen.
- Kein Wither-Placeholder mehr.
- Remove placed blocks from meteo on Playerdeath/Round end or after killed the boss

### 2. Neue Stats in UI/Progression verfügbar
Beschreibung: LevelUp/Shop/Loot bieten und zeigen neue Stats (Max Health, HP Regen, Shield, Armor, Evasion, Lifesteal, Thorns, Crit Chance, Crit Damage, Projectile Count, Attack Speed, Projectile Bounce, Size, Duration, Damage vs. Elites/Bosses, Knockback, Jump Height, XP Gain, Elite Spawn Increase, Powerup Multiplier).
Akzeptanzkriterien:
- Anzeigen im LevelUp-/Shop-Menü.
- Persistenz in PlayerData.
- Tooltips kurz und verständlich.

### 3. Performance & FX-Throttling
Beschreibung: FX abhängig von Distanz/FX-Setting drosseln.
Akzeptanzkriterien:
- Globaler Regler / Spieler-FX Toggle (vorhanden) ergänzt um Rate-Limiter.
- Große Kämpfe verursachen keine Lags.

### 4. Persistenz & Reset der neuen Stats
Beschreibung: Sicheres Laden/Speichern aller neuen Stats + Reset bei Run-Beginn.
Akzeptanzkriterien:
- PlayerData migriert (Backward-Compat).
- Start eines Runs setzt temporäre Boni korrekt zurück.

### 5. Stabilität Wellen/Continuous Mode
Beschreibung: Keine Zombie-Tasks; Start/Stop räumt sauber auf.
Akzeptanzkriterien:
- Alle BukkitRunnable werden bei Stop gecancelt.
- Continuous und Wave-Mode laufen ohne Deadlocks.

### 6. Doku (README/Docs)
Beschreibung: Installation, Konfiguration, neue Stats, FX-Commands (/fx …) kurz dokumentiert.
Akzeptanzkriterien:
- README Abschnitt „Getting Started“ + „Config cheatsheet“.

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
