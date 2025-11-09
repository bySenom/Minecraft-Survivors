# AbilityCatalog

Package: `org.bysenom.minecraftSurvivors.ability`

Zentrale Registry der Abilities: Key -> Definition (Name, Item, Stat-Formeln).

## Public Fields

- `double damage`
- `double attacksPerSec`
- `double radius`
- `double size`
- `double duration`
- `String key`
- `String display`
- `Material icon`
- `String desc`

## Public Methods

- `compute(SurvivorPlayer sp, int level)`
- `buildLore(SurvivorPlayer sp, int level)`
- `buildDeltaLore(SurvivorPlayer sp, int level, double rarMult)`
- `get(String key)`
- `all()`
