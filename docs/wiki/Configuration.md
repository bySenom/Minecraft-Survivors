# Konfiguration

Die Hauptkonfigurationsdatei befindet sich in `plugins/MinecraftSurvivors/config.yml`.

Wichtige Sektionen

- `spawn.*` — Distance, continuous settings, glow, elite
- `spawn.continuous.*` — Warmup, steps, caps, ticks-per-cycle
- `scaling.*` — health/damage/speed multipliers per minute
- `levelup.*` — Upgrade‑werte, rarity multipliers
- `party.*` — party/ready flow, vote seconds
- `stats.*` — DPS/HPS window, display mode
- `data.*` — autosave interval

Beispiel: `spawn.elite.size-scale: 1.5`

Tipps

- Teste Änderungen lokal mit geringeren Spawn‑Dichten (`spawn.continuous.base-per-player`) bevor du sie live setzt.
- Bei Problemen aktiviere in `paper.yml` das Debug‑Logging für Plugins oder prüfe `logs/latest.log`.
