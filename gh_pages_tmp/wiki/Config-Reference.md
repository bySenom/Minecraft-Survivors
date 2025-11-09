# Konfigurationsreferenz (auto-generated)

Diese Seite wurde automatisch aus `src/main/resources/config.yml` generiert.

---

## Top-level keys

- `wave`
- `spawn`
- `spawnMobTypes`
- `scaling`
- `lootchest`
- `ability`
- `shaman`
- `pyromancer`
- `ranger`
- `paladin`
- `levelup`
- `debug`
- `presets`
- `stats`
- `party`
- `shop`
- `skills`
- `evo`
- `meta`
- `data`
- `enrage`
- `endgame`
- `tablist`

---

## wave

| Key | Value (example) | Type |
|---|---|---|
| interval-seconds | `10` | number |
| spawn-per-player-base | `1` | number |
| scale-per-wave | `0.5` | number |

## spawn

| Key | Value (example) | Type |
|---|---|---|
| continuous | `{"enabled":true,"ticks-per-cycle":20,"base-per-player":0.35,"use-steps":true,"step-seconds":30,"growth-per-step":0.2,"growth-per-minute":0.25,"warmup-seconds":100,"warmup-mult-start":0.22,"max-per-player":8,"cap":{"dynamic":true,"base-per-player":60,"add-per-player":30,"max-per-player":180},"cap-total-nearby-per-player":60,"radius-per-player":40,"min-distance":8,"max-distance":16,"rest-every-seconds":0,"rest-duration-seconds":0}` | object |
| glowing | `true` | boolean |
| glowing-duration-ticks | `6000` | number |
| particle | `"FLAME"` | string |
| particle-duration | `10` | number |
| particle-points | `16` | number |
| particle-count | `2` | number |
| particle-spread | `0.1` | number |
| redstone-r | `255` | number |
| redstone-g | `255` | number |
| redstone-b | `255` | number |
| min-distance | `8` | number |
| max-distance | `16` | number |
| freeze-radius | `10` | number |
| elite | `{"chance-percentage":8,"size-scale":1.25,"base-health-mult":1.5,"extra-health-mult-per-minute":0.03,"extra-chance-per-minute":0}` | object |

## spawn.continuous

| Key | Value (example) | Type |
|---|---|---|
| enabled | `true` | boolean |
| ticks-per-cycle | `20` | number |
| base-per-player | `0.35` | number |
| use-steps | `true` | boolean |
| step-seconds | `30` | number |
| growth-per-step | `0.2` | number |
| growth-per-minute | `0.25` | number |
| warmup-seconds | `100` | number |
| warmup-mult-start | `0.22` | number |
| max-per-player | `8` | number |
| cap | `{"dynamic":true,"base-per-player":60,"add-per-player":30,"max-per-player":180}` | object |
| cap-total-nearby-per-player | `60` | number |
| radius-per-player | `40` | number |
| min-distance | `8` | number |
| max-distance | `16` | number |
| rest-every-seconds | `0` | number |
| rest-duration-seconds | `0` | number |

## spawn.continuous.cap

| Key | Value (example) | Type |
|---|---|---|
| dynamic | `true` | boolean |
| base-per-player | `60` | number |
| add-per-player | `30` | number |
| max-per-player | `180` | number |

## spawn.elite

| Key | Value (example) | Type |
|---|---|---|
| chance-percentage | `8` | number |
| size-scale | `1.25` | number |
| base-health-mult | `1.5` | number |
| extra-health-mult-per-minute | `0.03` | number |
| extra-chance-per-minute | `0` | number |

## scaling

| Key | Value (example) | Type |
|---|---|---|
| health-mult-per-minute | `0.18` | number |
| health-mid-minute | `3` | number |
| health-late-minute | `8` | number |
| health-mid-multiplier | `1.45` | number |
| health-late-multiplier | `1.9` | number |
| speed-mult-per-minute | `0.08` | number |
| damage-add-per-minute | `0.8` | number |
| damage-mid-multiplier | `1.2` | number |
| damage-late-multiplier | `1.35` | number |

## lootchest

| Key | Value (example) | Type |
|---|---|---|
| roll-steps | `30` | number |
| lifetime-seconds | `45` | number |
| trigger-radius | `1.6` | number |
| despawn-on-player-death | `true` | boolean |
| animation | `{"period-ticks":80,"bob-amplitude":0.15}` | object |
| particles | `{"enabled":true,"type":"END_ROD","count":2,"spread":0.15,"speed":0,"chance-1-in":40,"redstone-r":255,"redstone-g":255,"redstone-b":255,"redstone-size":1}` | object |
| rewards | `[{"type":"DAMAGE_MULT","value":0.1,"name":"+10% Damage","weight":4},{"type":"DAMAGE_ADD","value":1.5,"name":"+1.5 Schaden","weight":3},{"type":"FLAT_DAMAGE","value":2,"name":"+2 Flat","weight":2},{"type":"SPEED","value":0.05,"name":"+5% Movement","weight":3},{"type":"ATTACK_SPEED","value":0.07,"name":"+7% Attack Speed","weight":2},{"type":"RESIST","value":0.05,"name":"+5% Resist","weight":2},{"type":"LUCK","value":0.05,"name":"+5% Luck","weight":2},{"type":"HEALTH_HEARTS","value":2,"name":"+1 Herz","weight":1},{"type":"RADIUS_MULT","value":0.1,"name":"+10% Radius","weight":2},{"type":"PALADIN_HEAL","value":0.5,"name":"Paladin-Heal +0.5","weight":1}]` | array |

## lootchest.animation

| Key | Value (example) | Type |
|---|---|---|
| period-ticks | `80` | number |
| bob-amplitude | `0.15` | number |

## lootchest.particles

| Key | Value (example) | Type |
|---|---|---|
| enabled | `true` | boolean |
| type | `"END_ROD"` | string |
| count | `2` | number |
| spread | `0.15` | number |
| speed | `0` | number |
| chance-1-in | `40` | number |
| redstone-r | `255` | number |
| redstone-g | `255` | number |
| redstone-b | `255` | number |
| redstone-size | `1` | number |

## ability

| Key | Value (example) | Type |
|---|---|---|
| interval-ticks | `30` | number |

## shaman

| Key | Value (example) | Type |
|---|---|---|
| base-damage | `6` | number |
| radius | `10` | number |
| allow-player-damage | `false` | boolean |
| strikes-per-tick | `1` | number |

## pyromancer

| Key | Value (example) | Type |
|---|---|---|
| base-damage | `4` | number |
| radius | `8` | number |
| targets-per-tick | `2` | number |
| ignite-ticks | `60` | number |

## ranger

| Key | Value (example) | Type |
|---|---|---|
| base-damage | `7` | number |
| min-range | `8` | number |
| max-range | `20` | number |

## paladin

| Key | Value (example) | Type |
|---|---|---|
| base-damage | `2.5` | number |
| radius | `9` | number |
| heal | `1.2` | number |
| implosion-enabled | `true` | boolean |

## levelup

| Key | Value (example) | Type |
|---|---|---|
| xp-per-kill | `1` | number |
| values | `{"bonus-damage":1.5,"bonus-strikes":1,"flat-damage":2,"extra-hearts":2,"move-speed":0.05,"attack-speed":0.07,"resist":0.05,"luck":0.05}` | object |
| weapon | `{"size-step":0.15,"attackpower-step":0.2}` | object |
| pyromancer | `{"ignite-step":20}` | object |
| ranger | `{"knockback-step":0.1}` | object |
| paladin | `{"heal-step":0.5}` | object |
| rarity | `{"common":65,"rare":25,"epic":10}` | object |
| hud-interval-ticks | `100` | number |
| choice-max-seconds | `20` | number |

## levelup.values

| Key | Value (example) | Type |
|---|---|---|
| bonus-damage | `1.5` | number |
| bonus-strikes | `1` | number |
| flat-damage | `2` | number |
| extra-hearts | `2` | number |
| move-speed | `0.05` | number |
| attack-speed | `0.07` | number |
| resist | `0.05` | number |
| luck | `0.05` | number |

## levelup.weapon

| Key | Value (example) | Type |
|---|---|---|
| size-step | `0.15` | number |
| attackpower-step | `0.2` | number |

## levelup.pyromancer

| Key | Value (example) | Type |
|---|---|---|
| ignite-step | `20` | number |

## levelup.ranger

| Key | Value (example) | Type |
|---|---|---|
| knockback-step | `0.1` | number |

## levelup.paladin

| Key | Value (example) | Type |
|---|---|---|
| heal-step | `0.5` | number |

## levelup.rarity

| Key | Value (example) | Type |
|---|---|---|
| common | `65` | number |
| rare | `25` | number |
| epic | `10` | number |

## debug

| Key | Value (example) | Type |
|---|---|---|
| shaman-log | `true` | boolean |
| lootchest | `false` | boolean |

## presets

| Key | Value (example) | Type |
|---|---|---|
| subtle | `{"particle":"END_ROD","particle-duration":8,"particle-points":12,"particle-count":1,"particle-spread":0.06,"redstone-r":200,"redstone-g":200,"redstone-b":255}` | object |
| flashy | `{"particle":"FLAME","particle-duration":14,"particle-points":20,"particle-count":3,"particle-spread":0.12,"redstone-r":255,"redstone-g":180,"redstone-b":60}` | object |
| epic | `{"particle":"REDSTONE","particle-duration":20,"particle-points":28,"particle-count":4,"particle-spread":0.18,"redstone-r":128,"redstone-g":50,"redstone-b":200}` | object |

## presets.subtle

| Key | Value (example) | Type |
|---|---|---|
| particle | `"END_ROD"` | string |
| particle-duration | `8` | number |
| particle-points | `12` | number |
| particle-count | `1` | number |
| particle-spread | `0.06` | number |
| redstone-r | `200` | number |
| redstone-g | `200` | number |
| redstone-b | `255` | number |

## presets.flashy

| Key | Value (example) | Type |
|---|---|---|
| particle | `"FLAME"` | string |
| particle-duration | `14` | number |
| particle-points | `20` | number |
| particle-count | `3` | number |
| particle-spread | `0.12` | number |
| redstone-r | `255` | number |
| redstone-g | `180` | number |
| redstone-b | `60` | number |

## presets.epic

| Key | Value (example) | Type |
|---|---|---|
| particle | `"REDSTONE"` | string |
| particle-duration | `20` | number |
| particle-points | `28` | number |
| particle-count | `4` | number |
| particle-spread | `0.18` | number |
| redstone-r | `128` | number |
| redstone-g | `50` | number |
| redstone-b | `200` | number |

## stats

| Key | Value (example) | Type |
|---|---|---|
| window-seconds | `10` | number |
| mode | `"actionbar"` | string |
| update-interval-ticks | `20` | number |
| auto-cap | `{"dps":50,"hps":30}` | object |
| broadcast-top | `{"enabled":false,"interval-seconds":30,"n":3}` | object |
| dynamic-cap-window-seconds | `20` | number |
| dynamic-cap-enabled | `true` | boolean |
| dynamic-cap-smoothing | `0.2` | number |

## stats.auto-cap

| Key | Value (example) | Type |
|---|---|---|
| dps | `50` | number |
| hps | `30` | number |

## stats.broadcast-top

| Key | Value (example) | Type |
|---|---|---|
| enabled | `false` | boolean |
| interval-seconds | `30` | number |
| n | `3` | number |

## party

| Key | Value (example) | Type |
|---|---|---|
| xp-share | `{"enabled":true,"split-evenly":true,"min-per-member":1}` | object |

## party.xp-share

| Key | Value (example) | Type |
|---|---|---|
| enabled | `true` | boolean |
| split-evenly | `true` | boolean |
| min-per-member | `1` | number |

## shop

| Key | Value (example) | Type |
|---|---|---|
| enabled | `true` | boolean |
| limits | `{"max-per-run":5,"max-per-day":20}` | object |
| item-limits | `{"weapons-per-run":0,"weapons-per-day":0}` | object |
| daily | `{"max-weapons":0}` | object |
| categories | `{"gear":[{"name":"Helm (Eisen)","type":"ARMOR_HELMET","price":15,"rarity":"common","stats":{"prot":2,"health":1}},{"name":"Brust (Eisen)","type":"ARMOR_CHEST","price":25,"rarity":"rare","stats":{"prot":3,"health":1.5}},{"name":"Hose (Eisen)","type":"ARMOR_LEGS","price":20,"rarity":"common","stats":{"prot":2}},{"name":"Stiefel (Eisen)","type":"ARMOR_BOOTS","price":15,"rarity":"epic","stats":{"speed":0.03}}],"skills":[{"name":"Shockwave (Skill-Item)","type":"SKILL_SHOCKWAVE","price":30}],"class":{"paladin":[{"name":"+0.5 Heilung","type":"PALADIN_HEAL","value":0.5,"price":18}]}}` | object |
| npc | `{"spawn-mode":"worldspawn","world":"world","x":0,"y":64,"z":0,"yaw":0,"pitch":0,"name":"Händler","type":"PLAINS","profession":"ARMORER"}` | object |

## shop.limits

| Key | Value (example) | Type |
|---|---|---|
| max-per-run | `5` | number |
| max-per-day | `20` | number |

## shop.item-limits

| Key | Value (example) | Type |
|---|---|---|
| weapons-per-run | `0` | number |
| weapons-per-day | `0` | number |

## shop.daily

| Key | Value (example) | Type |
|---|---|---|
| max-weapons | `0` | number |

## shop.categories

| Key | Value (example) | Type |
|---|---|---|
| gear | `[{"name":"Helm (Eisen)","type":"ARMOR_HELMET","price":15,"rarity":"common","stats":{"prot":2,"health":1}},{"name":"Brust (Eisen)","type":"ARMOR_CHEST","price":25,"rarity":"rare","stats":{"prot":3,"health":1.5}},{"name":"Hose (Eisen)","type":"ARMOR_LEGS","price":20,"rarity":"common","stats":{"prot":2}},{"name":"Stiefel (Eisen)","type":"ARMOR_BOOTS","price":15,"rarity":"epic","stats":{"speed":0.03}}]` | array |
| skills | `[{"name":"Shockwave (Skill-Item)","type":"SKILL_SHOCKWAVE","price":30}]` | array |
| class | `{"paladin":[{"name":"+0.5 Heilung","type":"PALADIN_HEAL","value":0.5,"price":18}]}` | object |

## shop.categories.class

| Key | Value (example) | Type |
|---|---|---|
| paladin | `[{"name":"+0.5 Heilung","type":"PALADIN_HEAL","value":0.5,"price":18}]` | array |

## shop.npc

| Key | Value (example) | Type |
|---|---|---|
| spawn-mode | `"worldspawn"` | string |
| world | `"world"` | string |
| x | `0` | number |
| y | `64` | number |
| z | `0` | number |
| yaw | `0` | number |
| pitch | `0` | number |
| name | `"Händler"` | string |
| type | `"PLAINS"` | string |
| profession | `"ARMORER"` | string |

## skills

| Key | Value (example) | Type |
|---|---|---|
| shockwave | `{"cooldown-ms":800,"radius":6,"damage":2}` | object |
| dash | `{"cooldown-ms":2500,"cooldown-rclick-ms":3000}` | object |
| genkidama | `{"cooldown-ms":12000,"charge-max-ms":4000,"damage-base":6,"damage-per-second":4,"radius-base":3,"radius-per-second":1,"projectile-speed":1.1,"max-travel-ticks":80}` | object |

## skills.shockwave

| Key | Value (example) | Type |
|---|---|---|
| cooldown-ms | `800` | number |
| radius | `6` | number |
| damage | `2` | number |

## skills.dash

| Key | Value (example) | Type |
|---|---|---|
| cooldown-ms | `2500` | number |
| cooldown-rclick-ms | `3000` | number |

## skills.genkidama

| Key | Value (example) | Type |
|---|---|---|
| cooldown-ms | `12000` | number |
| charge-max-ms | `4000` | number |
| damage-base | `6` | number |
| damage-per-second | `4` | number |
| radius-base | `3` | number |
| radius-per-second | `1` | number |
| projectile-speed | `1.1` | number |
| max-travel-ticks | `80` | number |

## evo

| Key | Value (example) | Type |
|---|---|---|
| pyromancer | `{"ignite-ticks-min":60,"damage-mult-min":0.4}` | object |

## evo.pyromancer

| Key | Value (example) | Type |
|---|---|---|
| ignite-ticks-min | `60` | number |
| damage-mult-min | `0.4` | number |

## meta

| Key | Value (example) | Type |
|---|---|---|
| endrun | `{"essence-per-minute":2,"essence-per-kill":0}` | object |
| shop | `[{"key":"dmg_mult_small","name":"Permanenter Schaden I","type":"DAMAGE_MULT","price":10,"step":0.01,"cap":0.2},{"key":"move_speed_small","name":"Bewegungstempo I","type":"MOVE_SPEED","price":10,"step":0.01,"cap":0.15},{"key":"attack_speed_small","name":"Angriffstempo I","type":"ATTACK_SPEED","price":10,"step":0.01,"cap":0.2},{"key":"resist_small","name":"Resistenz I","type":"RESIST","price":10,"step":0.01,"cap":0.15},{"key":"luck_small","name":"Glück I","type":"LUCK","price":10,"step":0.01,"cap":0.2},{"key":"health_small","name":"+1 Herz (permanent)","type":"HEALTH_HEARTS","price":25,"step":2,"cap":6},{"key":"skill_slot","name":"Skill Slot +1 (permanent)","type":"SKILL_SLOT","price":50,"step":1,"cap":4}]` | array |

## meta.endrun

| Key | Value (example) | Type |
|---|---|---|
| essence-per-minute | `2` | number |
| essence-per-kill | `0` | number |

## data

| Key | Value (example) | Type |
|---|---|---|
| autosave-interval-seconds | `120` | number |

## enrage

| Key | Value (example) | Type |
|---|---|---|
| enabled | `true` | boolean |
| start-minute | `12` | number |
| ramp-minutes | `3` | number |
| health-mult-max | `3` | number |
| speed-mult-max | `1.6` | number |
| damage-mult-max | `2` | number |

## endgame

| Key | Value (example) | Type |
|---|---|---|
| enabled | `true` | boolean |
| boss | `{"type":"WITHER"}` | object |

## endgame.boss

| Key | Value (example) | Type |
|---|---|---|
| type | `"WITHER"` | string |

## tablist

| Key | Value (example) | Type |
|---|---|---|
| enabled | `true` | boolean |
| update-interval-ticks | `20` | number |
| header-title | `"Minecraft Survivors"` | string |
| show-enemy-power | `true` | boolean |
| show-party-hp | `true` | boolean |

