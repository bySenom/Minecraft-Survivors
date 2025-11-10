package org.bysenom.minecraftSurvivors.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

/**
 * Utility helper to apply damage with proper ms_ability_key attribution.
 */
public final class DamageUtil {
    private DamageUtil() {}

    public static void damageWithAttribution(MinecraftSurvivors plugin, Player owner, LivingEntity target, double amount, String abilityKey) {
        if (owner == null || target == null) {
            try { target.damage(amount); } catch (Throwable ignored) {}
            return;
        }
        try {
            try { owner.setMetadata("ms_ability_key", new FixedMetadataValue(plugin, abilityKey)); } catch (Throwable ignored) {}
            try { target.damage(amount, owner); } catch (Throwable ignored) {}
        } finally {
            try { owner.removeMetadata("ms_ability_key", plugin); } catch (Throwable ignored) {}
        }
    }

    // fallback helper when owner may be null
    public static void damageWithAttributionNullable(MinecraftSurvivors plugin, Player owner, LivingEntity target, double amount, String abilityKey) {
        if (owner == null) {
            try { target.damage(amount); } catch (Throwable ignored) {}
            return;
        }
        damageWithAttribution(plugin, owner, target, amount, abilityKey);
    }
}
