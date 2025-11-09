# Konfigurationsreferenz — Minecraft Survivors

Diese Seite dokumentiert die relevanten Konfigurations‑Keys aus der Standard‑`config.yml` (siehe `src/main/resources/config.yml`). Für jeden Eintrag findest du den Default‑Wert und eine kurze Beschreibung.

Hinweis: Werte können im Live‑Server unter `plugins/MinecraftSurvivors/config.yml` angepasst werden.

---

## Übersicht (Top‑Level Keys)
- `wave` — Wave‑spezifische Einstellungen (legacy)
- `spawn` — Continuous/Spawn‑Einstellungen (Hauptteil des Spawn‑Systems)
- `spawnMobTypes` — Gewichtete Mob‑Typen (EntityType, weight, minMinute)
- `scaling` — Gegner‑Scaling pro Minute (HP/Speed/Damage)
- `lootchest` — Lootchest / Slotmaschine Konfiguration
- `ability` — Globale Ability‑Ticking Einstellungen
- Klassen: `shaman`, `pyromancer`, `ranger`, `paladin` — klassen‑spezifische Werte
- `levelup` — LevelUp / Upgrade Defaults & Raritätseinstellungen
- `debug` — Debug‑Flags
- `presets` — Partikel‑/Spawn‑Presets
- `stats` — DPS/HPS Anzeige‑Konfiguration
- `party` — Party / XP‑Share Einstellungen
- `shop` — Shop, NPC und Limits
- `skills` — globale Skill‑Tuning‑Werte
- `evo` — Evolutions/Upgrade‑Grenzwerte
- `meta` — Meta‑Progression (Shop & Endrun)

---

## `wave`
```yaml
wave:
  interval-seconds: 10
  spawn-per-player-base: 1
  scale-per-wave: 0.5
```
- `interval-seconds` (int): Sekundentakt für Wave‑System (legacy). Default: `10`.
- `spawn-per-player-base` (int): Basis‑Anzahl Mobs pro Spieler in Wave‑Spawns. Default: `1`.
- `scale-per-wave` (double): zusätzliche Mobs pro Wave (perPlayer = base + floor(wave * scale)). Default: `0.5`.

> Hinweis: Wenn `spawn.continuous.enabled: true` ist, verwendet das System Continuous‑Spawns statt Waves.

---

## `spawn` (Hauptkonfiguration)
Wichtige Unterkeys und Defaults:
```yaml
spawn:
  continuous:
    enabled: true
    ticks-per-cycle: 20
    base-per-player: 0.35
    use-steps: true
    step-seconds: 30
    growth-per-step: 0.2
    growth-per-minute: 0.25
    warmup-seconds: 100
    warmup-mult-start: 0.22
    max-per-player: 8
    cap:
      dynamic: true
      base-per-player: 60
      add-per-player: 30
      max-per-player: 180
    cap-total-nearby-per-player: 60
    radius-per-player: 40.0
    min-distance: 8.0
    max-distance: 16.0
    rest-every-seconds: 0
    rest-duration-seconds: 0
  glowing: true
  glowing-duration-ticks: 6000
  particle: FLAME
  particle-duration: 10
  particle-points: 16
  particle-count: 2
  particle-spread: 0.1
  redstone-r/g/b: 255
  freeze-radius: 10.0
  elite:
    chance-percentage: 8
    size-scale: 1.25
    base-health-mult: 1.50
    extra-health-mult-per-minute: 0.03
    extra-chance-per-minute: 0.0
```
Erklärungen (Auswahl):
- `continuous.enabled` (bool): Schaltet Continuous‑Spawns an/aus.
- `ticks-per-cycle` (int): Ticks pro Spawn‑Cycle; 20 Ticks = 1s.
- `base-per-player` (double): Basis‑Spawnbudget pro Spieler und Zyklus.
- `use-steps`/`step-seconds`/`growth-per-step`: Wenn `use-steps=true` wächst Spawn‑Rate in diskreten Schritten (z. B. alle 30s) um `growth-per-step` per Step. Wenn `false`, wird `growth-per-minute` linear verwendet.
- `warmup-seconds`/`warmup-mult-start`: Sanfter Warmup‑Ramp für Early Game (Skalierung in den ersten Sekunden).
- `max-per-player`: harte Obergrenze der gleichzeitig nahen Mobs pro Spieler.
- `cap.dynamic`: wenn `true` werden Caps pro Spielerzahl berechnet (Base + add-per-player * (online-1)), sonst `cap-total-nearby-per-player` verwenden.
- `radius-per-player`: Radius zur Dichtezählung von Mobs (für Caps).
- `min-distance`/`max-distance`: Spawnabstände relativ zum Spieler.
- `rest-every-seconds`/`rest-duration-seconds`: Optionale Ruhephasen (keine Spawns) im Continuous‑Modus.
- `glowing` & `glowing-duration-ticks`: Standard‑Glow für neu gespawnte Wave/Continuous Mobs.
- `particle.*`: Partikeltyp/-intensität für Spawn‑Animationen; `particle` akzeptiert gängige `Particle` Enum‑Namen.
- `freeze-radius`: Radius, in dem beim Pausieren (z. B. LevelUp) mobs für den pausierten Spieler eingefroren werden.
- `elite.*`: Elite‑Spawn‑Konfiguration: Chance (%), `size-scale` (visuelle Skalierung), HP‑Multiplikatoren und optionale zeitliche Erhöhung der Elite‑Chance.

---

## `spawnMobTypes`
Listet gewichtete Mob‑Typen mit Mindest‑Minute:
```yaml
spawnMobTypes:
  - type: ZOMBIE
    weight: 5
    minMinute: 0
  - type: SKELETON
    weight: 3
    minMinute: 1
  # ...
```
- `type`: EntityType Name (z. B. `ZOMBIE`, `SKELETON`, `STRAY`), must be a living mob.
- `weight`: relative Wahrscheinlichkeit für den Pool.
- `minMinute`: erst nach dieser Spielminute ist der Typ verfügbar.

---

## `scaling`
```yaml
scaling:
  health-mult-per-minute: 0.18
  health-mid-minute: 3.0
  health-late-minute: 8.0
  health-mid-multiplier: 1.45
  health-late-multiplier: 1.90
  speed-mult-per-minute: 0.08
  damage-add-per-minute: 0.8
  damage-mid-multiplier: 1.20
  damage-late-multiplier: 1.35
```
- `health-mult-per-minute` (double): Basis‑HP‑Wachstum pro Minute.
- `health-mid-minute` / `health-late-minute`: Minuten für Mid/Late‑Gates.
- `health-mid-multiplier` / `health-late-multiplier`: zusätzliche Multiplikatoren für Mid/Late Phasen.
- `speed-mult-per-minute`, `damage-add-per-minute`: Speed/Flat‑Damage Zuwächse pro Minute.
- `damage-mid/late-multiplier`: multiplikative Verstärkung der Schadenszunahme in späteren Phasen.

---

## `lootchest` (Slotmaschine / Lootchest)
Wesentliche Subkeys:
```yaml
lootchest:
  roll-steps: 30
  lifetime-seconds: 45
  trigger-radius: 1.6
  despawn-on-player-death: true
  animation:
    period-ticks: 80
    bob-amplitude: 0.15
  particles:
    enabled: true
    type: END_ROD
    count: 2
    spread: 0.15
    speed: 0.0
    chance-1-in: 40
  rewards:
    - type: DAMAGE_MULT
      value: 0.10
      name: "+10% Damage"
      weight: 4
    # ...
```
- `roll-steps`: Länge/Komplexität der Slotmaschine (Legacy).
- `lifetime-seconds`: Wie lange die Kiste in der Welt bleibt.
- `trigger-radius`: Radius in dem die Kiste geöffnet wird.
- `despawn-on-player-death`: Entfernt Kisten beim Tod des Spielers (boolean).
- `animation.*`: visuelle Details der Slotmaschine.
- `particles.*`: Partikelkonfiguration; `chance-1-in` steuert Zufalls-Partikelemission im Tick.
- `rewards`: Liste möglicher Loot‑Rewards mit `type`, `value`, `name`, `weight` (Gewichtung für Zufallswahl).

---

## `ability`
```yaml
ability:
  interval-ticks: 30
```
- `interval-ticks` (int): Basis‑Tickdauer für das Ability‑System (z. B. 30 ticks = 1.5s). Beeinflusst, wie oft AbilityManager tickt.

---

## Klassen‑Sektionen (`shaman`, `pyromancer`, `ranger`, `paladin`)
Beispiele (Defaults aus config.yml):

### `shaman`
```yaml
shaman:
  base-damage: 6.0
  radius: 10.0
  allow-player-damage: false
  strikes-per-tick: 1
```
- `base-damage`: Basisschaden pro Strike.
- `radius`: Suchradius um Spieler für Ziele.
- `allow-player-damage`: ob die Fähigkeit Spieler treffen kann.
- `strikes-per-tick`: Anzahl Ziele pro Ability‑Tick (kann durch Upgrades erhöht werden).

### `pyromancer`
```yaml
pyromancer:
  base-damage: 4.0
  radius: 8.0
  targets-per-tick: 2
  ignite-ticks: 60
```
- `ignite-ticks`: Dauer des Brenn‑(DoT) Effekts in Ticks.

### `ranger`
```yaml
ranger:
  base-damage: 7.0
  min-range: 8.0
  max-range: 20.0
```
- `min-range` / `max-range`: Bereich in dem Ranger‑Projete Ziele priorisiert.

### `paladin`
```yaml
paladin:
  base-damage: 2.5
  radius: 9.0
  heal: 1.2
  implosion-enabled: true
```
- `heal`: Heilwert (HPS relevante Werte).
- `implosion-enabled`: schaltet kurze Implosion/Endwave ein.

---

## `levelup` (Upgrades & Raritäten)
```yaml
levelup:
  xp-per-kill: 1
  values:
    bonus-damage: 1.5
    bonus-strikes: 1
    flat-damage: 2.0
    extra-hearts: 2
    move-speed: 0.05
    attack-speed: 0.07
    resist: 0.05
    luck: 0.05
  weapon:
    size-step: 0.15
    attackpower-step: 0.20
  pyromancer:
    ignite-step: 20
  ranger:
    knockback-step: 0.10
  paladin:
    heal-step: 0.5
  rarity:
    common: 65
    rare: 25
    epic: 10
  hud-interval-ticks: 100
  choice-max-seconds: 20
```
- `xp-per-kill`: XP, die ein Kill gibt.
- `values.*`: Default‑Effektstärken für Auswahl‑Optionen (z. B. +Damage, +Speed).
- `weapon.*`: Waffenaufwertungswerte (Radius/AttackPower Schritte).
- `rarity`: Prozentgewichtung (Summe wird nicht geprüft; UI nutzt diese Werte für Anzeige/Verteilung).
- `hud-interval-ticks`: HUD Update Intervall.
- `choice-max-seconds`: Maximale Pause‑Zeit zum Treffen einer Entscheidung (LevelUp/LootPick).

---

## `debug`
```yaml
debug:
  shaman-log: true
  lootchest: false
```
- Schaltet zusätzliche Logs für bestimmte Subsysteme an.

---

## `presets`
Partikel‑/Spawn‑Presets, die per GUI/Command angewendet werden können (z. B. `subtle`, `flashy`, `epic`). Struktur spiegelt `particle.*` keys wider.

---

## `stats`
```yaml
stats:
  window-seconds: 10
  mode: actionbar
  update-interval-ticks: 20
  auto-cap:
    dps: 50.0
    hps: 30.0
  broadcast-top:
    enabled: false
    interval-seconds: 30
    n: 3
  dynamic-cap-window-seconds: 20
  dynamic-cap-enabled: true
  dynamic-cap-smoothing: 0.2
```
- `window-seconds`: Sliding window Größe (DPS/HPS Berechnung).
- `mode`: Anzeige-Modus (`actionbar`, `bossbar`, `scoreboard`, `off`).
- `auto-cap`: optionale Caps für Meter.
- `broadcast-top`: periodische Broadcasts des Top‑DPS/HPS (optional).
- `dynamic-cap-*`: smoothing und Fenster‑Größe für dynamische Caps.

---

## `party`
```yaml
party:
  xp-share:
    enabled: true
    split-evenly: true
    min-per-member: 1
```
- `xp-share.enabled`: xp per kill unter Partymitgliedern teilen.
- `split-evenly`: gleiches Teilen mit Restverteilung.
- `min-per-member`: Mindest‑XP pro Member (deckelnd berücksichtigt).

---

## `shop`
Große, flexible Struktur für NPC‑Shop, Daily Offers und Limits. Wichtige Werte:
```yaml
shop:
  enabled: true
  limits:
    max-per-run: 5
    max-per-day: 20
  item-limits: { weapons-per-run: 0, weapons-per-day: 0 }
  daily:
    max-weapons: 0
  categories:
    gear:
      - name: "Helm (Eisen)"
        type: ARMOR_HELMET
        price: 15
        rarity: common
        stats: { prot: 2, health: 1.0 }
    # ...
  npc:
    spawn-mode: worldspawn
    world: world
    x/y/z/yaw/pitch
    name: "Händler"
    type: PLAINS
    profession: ARMORER
```
- `shop.categories` ist eine Liste/Map von Items mit Typ, Preis, Rarity und Stats.
- `npc` konfiguriert Shop‑NPC Spawnmodus/Ort und Typ.

---

## `skills` / `evo` / `meta`
Enthält spezifische Tuning‑Werte für globale Skills, Evolutions‑Mindestwerte und Meta‑Progression (permanente Upgrades). Beispiele:
```yaml
skills:
  shockwave:
    cooldown-ms: 800
    radius: 6.0
    damage: 2.0
# meta.shop: Liste von permanenten Upgrades mit key, type, price, step, cap
```

---

## Empfehlungen & Best Practices
- Teste große Änderungen lokal mit reduzierter Spawn‑Dichte und kürzeren Warmup‑Werten.
- Bei Balancing‑Änderungen: erhöhe jeweils nur einen Parameter und beobachte Auswirkungen (z. B. `scaling.health-mult-per-minute` oder `spawn.continuous.growth-per-step`).
- Nutze `presets` für schnelle Optik‑Anpassungen (subtle/flashy/epic).
- Bei Problemen mit Start/Party‑Flow: prüfe `party.*` Werte und `GameManager` Logs.

---

Wenn du willst, kann ich diese Referenz automatisch aus `config.yml` generieren (Script), oder bei jeder Änderung die Wiki‑Seite per CI erneuern. Soll ich:

- [ ] ein kleines Node/Python‑Script hinzufügen, das `config.yml` parst und `docs/wiki/Config-Reference.md` automatisch erzeugt (und z. B. bei jedem commit ausgeführt wird)?
- [ ] die Seite direkt in Deutsch/Englisch erweitern (z. B. Beispiele, Use‑Cases)?

Sag mir, welchen der beiden Schritte du möchtest — dann implementiere ich ihn (inkl. CI‑Integration falls gewünscht).
