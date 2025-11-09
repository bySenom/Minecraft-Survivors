# StatsMeterManager

Package: `org.bysenom.minecraftSurvivors.manager`

Erfasst pro Spieler den verursachten Schaden und die Heilung in einem Sliding-Window (sekundengenau).

## Public Methods

- `recordDamage(UUID player, double amount)`
- `recordHeal(UUID player, double amount)`
- `getDps(UUID player)`
- `getHps(UUID player)`
- `getDpsMax(UUID player)`
- `getHpsMax(UUID player)`
- `reset(UUID player)`
- `resetAll()`
