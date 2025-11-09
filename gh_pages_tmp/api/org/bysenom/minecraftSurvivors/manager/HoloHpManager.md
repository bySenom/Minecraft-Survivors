# HoloHpManager

Package: `org.bysenom.minecraftSurvivors.manager`

Zeigt eine ruckelfreie, angeheftete HP-Bar: unsichtbarer ArmorStand als Passenger auf dem Mob.
- Ein Overlay pro Mob
- Text wird aktualisiert und Timeout wird verlängert
- Periodischer Cleanup entfernt abgelaufene/ungültige Overlays

## Public Methods

- `updateBar(LivingEntity mob, double hp, double max)`
