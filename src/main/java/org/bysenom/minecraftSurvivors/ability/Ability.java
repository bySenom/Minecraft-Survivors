// File: src/main/java/org/bysenom/minecraftSurvivors/ability/Ability.java
package org.bysenom.minecraftSurvivors.ability;

import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

public interface Ability {
    /**
     * Wird periodisch aufgerufen (z.\u00a0B. alle 1.5s).
     * @param player der Bukkit-Player
     * @param sp das zugeordnete SurvivorPlayer-Modell
     */
    void tick(Player player, SurvivorPlayer sp);
}
