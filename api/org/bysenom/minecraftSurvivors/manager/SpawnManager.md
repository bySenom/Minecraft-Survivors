# SpawnManager

Package: `org.bysenom.minecraftSurvivors.manager`

SpawnManager: Spawnt Wellen und verwaltet das "Einfrieren" von Mobs ausschließlich über AI-Deaktivierung.
Zusätzlich: Continuous-Spawn (Vampire Survivors Style) mit Gewichtungen & Scaling.

## Public Methods

- `spawnWave(int waveNumber)`
- `clearWaveMobs()`
- `run()`
- `getNearbyWaveMobs(Location center, double radius)`
- `strikeLightningAtTarget(LivingEntity target, double damage, Player source)`
- `startContinuousIfEnabled()`
- `startContinuous()`
- `run()`
- `pauseContinuous()`
- `resumeContinuous()`
- `stopContinuous()`
- `cancelAllScheduledTasks()`
- `getElapsedMinutes()`
- `getEnrageProgress()`
- `getEnemyPowerIndex()`
- `markAsWave(org.bukkit.entity.LivingEntity mob)`
- `repelMobsAround(org.bukkit.entity.Player p, double radius, double strength, boolean onlyWave)`
