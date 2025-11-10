// File: src/main/java/org/bysenom/minecraftSurvivors/listener/EntityDeathListener.java
package org.bysenom.minecraftSurvivors.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bysenom.minecraftSurvivors.ability.AbilityCatalog;
import org.bysenom.minecraftSurvivors.gui.GuiManager;
import org.bysenom.minecraftSurvivors.manager.PlayerManager;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;
import org.bysenom.minecraftSurvivors.util.ConfigUtil;

public class EntityDeathListener implements Listener {

    private final PlayerManager playerManager;
    private final GuiManager guiManager;
    private final ConfigUtil config;

    public EntityDeathListener(PlayerManager playerManager, GuiManager guiManager, ConfigUtil config) {
        this.playerManager = playerManager;
        this.guiManager = guiManager;
        this.config = config;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = null;
        if (e.getEntity().getKiller() instanceof Player) {
            killer = (Player) e.getEntity().getKiller();
            SurvivorPlayer sp = playerManager.get(killer.getUniqueId());
            sp.addKill();
            try { var rsm = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getRoundStatsManager(); if (rsm != null) rsm.recordKill(killer.getUniqueId(), e.getEntity().getType().name()); } catch (Throwable ignored) {}
            sp.addCoins(1); // einfache Belohnung
            try { var rsm = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getRoundStatsManager(); if (rsm != null) rsm.recordCoins(killer.getUniqueId(), 1); } catch (Throwable ignored) {}
            try { killer.sendActionBar(Component.text("Kills: " + sp.getKills() + "  Coins: " + sp.getCoins())); } catch (Throwable ignored) {}

            int xpGain = 1;
            try { if (config != null) xpGain = config.getInt("levelup.xp-per-kill", 1); } catch (Throwable ignored) {}

            // Party-Share
            org.bysenom.minecraftSurvivors.manager.PartyManager pm = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getPartyManager();
            java.util.List<java.util.UUID> targets = new java.util.ArrayList<>();
            if (pm != null) {
                org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm.getPartyOf(killer.getUniqueId());
                if (party != null) targets = pm.onlineMembers(party);
            }
            if (targets.isEmpty()) {
                // no party: solo xp
                handleXpGain(killer, sp, xpGain);
            } else {
                // even split among online party members
                int members = Math.max(1, targets.size());
                int per = Math.max(1, xpGain / members);
                int remainder = Math.max(0, xpGain - per * members);
                for (java.util.UUID u : targets) {
                    Player pl = org.bukkit.Bukkit.getPlayer(u);
                    if (pl == null) continue;
                    SurvivorPlayer memberSp = playerManager.get(u);
                    int give = per + (remainder > 0 ? 1 : 0);
                    if (remainder > 0) remainder--;
                    handleXpGain(pl, memberSp, give);
                }
            }
        }
        // Loot chest drop chance (spawn floating loot chest with hologram)
        try {
            org.bukkit.entity.LivingEntity dead = e.getEntity();
            org.bukkit.World w = dead.getWorld();
            int chance = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getConfigUtil().getInt("spawn.loot.chest-drop-chance-percentage", 5);
            if (chance > 0 && new java.util.Random().nextInt(100) < chance) {
                org.bukkit.Location loc = dead.getLocation();
                // Spawne schwebende Lootchest (ItemDisplay + TextDisplay); Öffnen per Nähe
                org.bysenom.minecraftSurvivors.listener.LootchestListener.spawnLootChest(loc);
            }

            // Glyph drop chance (configurable). Picks a random ability from AbilityCatalog and spawns a glyph like a lootchest
            int glyphChance = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getConfigUtil().getInt("spawn.glyph.drop-chance-percentage", 2);
            if (glyphChance > 0 && new java.util.Random().nextInt(100) < glyphChance) {
                org.bukkit.Location loc = dead.getLocation();
                java.util.List<AbilityCatalog.Def> allDefs = new java.util.ArrayList<>(AbilityCatalog.all());
                java.util.List<AbilityCatalog.Def> pool = new java.util.ArrayList<>();
                // Prefer glyphs the killer actually has/unlocked. Also ensure equipment compatibility if configured.
                try {
                    if (killer != null) {
                        org.bysenom.minecraftSurvivors.model.SurvivorPlayer spk = playerManager.get(killer.getUniqueId());
                        for (AbilityCatalog.Def d : allDefs) {
                            try {
                                // Only include abilities the player has (active) or explicitly unlocked
                                boolean owned = spk != null && (spk.hasAbility(d.key) || spk.hasUnlockedAbility(d.key));
                                if (!owned) continue;
                                // Optional: still respect equipment compatibility as a secondary check
                                if (isAbilityCompatibleWithPlayer(d, killer)) pool.add(d);
                                else pool.add(d); // keep for now to not over-filter; change if you want stricter behavior
                            } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
                if (pool.isEmpty()) pool = allDefs; // fallback: if nothing matched, keep original behaviour
                if (!pool.isEmpty()) {
                    AbilityCatalog.Def pick = pool.get(new java.util.Random().nextInt(pool.size()));
                    try { org.bysenom.minecraftSurvivors.listener.GlyphPickupListener.spawnGlyph(loc, pick.key); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    private void handleXpGain(Player player, SurvivorPlayer sp, int xpGain) {
        int beforeLevel = sp.getClassLevel();
        boolean leveled = sp.addXp(xpGain);
        int afterLevel = sp.getClassLevel();
        if (leveled) {
            org.bysenom.minecraftSurvivors.manager.GameManager gm = org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getGameManager();
            if (gm != null && gm.isPlayerPaused(player.getUniqueId())) {
                gm.enqueueLevelUp(player.getUniqueId(), afterLevel);
            } else if (gm != null) {
                gm.enqueueLevelUp(player.getUniqueId(), afterLevel);
                gm.tryOpenNextQueued(player.getUniqueId());
            }
            if (afterLevel > beforeLevel) player.sendMessage(Component.text("§aLevel up! Du bist jetzt Level " + afterLevel));
            else player.sendMessage(Component.text("§aLevel up!"));
            try {
                // Show quick ActionBar with core stat snapshot
                String snapshot = String.format("DMG: +%.0f%%  Crit: %.0f%%/x%.0f  Shield: %.1f  HP/s: %.2f",
                        sp.getEffectiveDamageMult()*100.0, sp.getCritChance()*100.0, sp.getCritDamage()*100.0, sp.getShieldMax(), sp.getHpRegen());
                try { player.sendActionBar(Component.text(snapshot)); } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
            // Persist player data asynchronously so rewards/choices are saved immediately
            try { org.bysenom.minecraftSurvivors.MinecraftSurvivors.getInstance().getPlayerDataManager().saveAsync(sp); } catch (Throwable ignored) {}
        }
    }

    // Determine if an ability makes sense to spawn for a given player based on their equipment
    private boolean isAbilityCompatibleWithPlayer(AbilityCatalog.Def def, Player p) {
        if (def == null || p == null) return true;
        try {
            org.bukkit.Material icon = def.icon;
            if (icon == null) return true;
            // If ability uses BOW as icon, require player to have BOW/CROSSBOW in main/offhand or hotbar
            if (icon == org.bukkit.Material.BOW) {
                // check main/off hand
                org.bukkit.inventory.ItemStack main = p.getInventory().getItemInMainHand();
                org.bukkit.inventory.ItemStack off = p.getInventory().getItemInOffHand();
                if (main != null && (main.getType() == org.bukkit.Material.BOW || main.getType() == org.bukkit.Material.CROSSBOW)) return true;
                if (off != null && (off.getType() == org.bukkit.Material.BOW || off.getType() == org.bukkit.Material.CROSSBOW)) return true;
                // check hotbar
                for (int i = 0; i < 9; i++) {
                    org.bukkit.inventory.ItemStack it = p.getInventory().getItem(i);
                    if (it == null) continue;
                    org.bukkit.Material t = it.getType();
                    if (t == org.bukkit.Material.BOW || t == org.bukkit.Material.CROSSBOW) return true;
                }
                return false; // no bow/crossbow found
            }
            // Other icons/abilities are considered universal for now
            return true;
        } catch (Throwable ignored) {}
        return true;
    }
}
