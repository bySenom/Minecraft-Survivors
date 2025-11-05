# MinecraftSurvivors

Ein leichtgewichtiges Vampire-Survivors‑ähnliches Minigame für Minecraft.

Neueste Highlights
- Shop-GUI: Upgrades mit Coins kaufen (Damage %, Radius %, Paladin-Heal u. a.). Anti-Doppelkauf pro Run, Limits pro Run/Tag konfigurierbar.
- Elite-Gegner: 50% mehr HP, leicht größer, seltene Spawns (Standard 8%).
- Lootchest: Mobs droppen gelegentlich eine Lootchest – Rechtsklick öffnet eine Slotmaschine und vergibt ein zufälliges Powerup.
- DPS/HPS-Anzeige: Actionbar, Bossbar (mit dynamischer Autoskala) oder Scoreboard-Modus. Umschaltbar per GUI oder /msstats.
- Continuous Spawns: Feineres Difficulty‑Tuning in 30s‑Schritten, Early‑Game reduziert, dynamische Caps je Spielerzahl.

Quickstart
- /msmenu öffnet das Hauptmenü
  - Klasse wählen (Start erst nach Auswahl möglich)
  - Shop (Coins durch Kills)
  - Party, Stats‑Modus, Config‑Presets
- Start löst einen 5‑Sekunden‑Countdown aus

Konfiguration (Auszug)
- Shop
  - shop.enabled: true
  - shop.limits.max-per-run: 5
  - shop.limits.max-per-day: 20
  - shop.categories.weapons: Liste von Items (type, value, price)
  - shop.categories.class.paladin: Spezial‑Upgrades pro Klasse
- Elite
  - spawn.elite.chance-percentage: 8
- Lootchest
  - spawn.loot.chest-drop-chance-percentage: 5
  - lootchest.roll-rows, lootchest.roll-steps, lootchest.rewards: visuelle/inhaltliche Kontrolle
- Stats
  - stats.mode: actionbar | bossbar | scoreboard | off
  - stats.dynamic-cap-enabled: true/false (Bossbar skaliert mit Max der letzten N Sek.)
  - stats.window-seconds: N (Fenstergröße in Sekunden)

Commands
- /msmenu – Hauptmenü
- /msstart – Startet das Spiel (mit Klassenpflicht + Countdown)
- /msstats mode <actionbar|bossbar|scoreboard|off>
- /msstats toggle
- /party invite <spieler>, /party accept, /party leave

Hinweise
- HPS zeigt nur die eigene Heilung (kein Empfang) – Paladin‑AoE heilt Teamkameraden, HPS wird dem Heiler gutgeschrieben.
- Scoreboard: Kompakte, unicode‑freundliche Anzeige (ohne Wave‑Zeile), kann per scoreboard.fancy geschmackvoller gestaltet werden.

Roadmap (Kurz)
- Lootchest: mehr Itempools pro Klasse, animierte Reel‑Symbole pro Effekt
- Mehr Klassen + Waffen‑Evolutionspfade
- Bossbar: Feintuning der dynamischen Skala (Glättung, Sticky Caps)
- Itemshop: Daily‑Refresh mit Pool‑Rotation

Lizenz
- MIT (oder projektintern festlegen)

