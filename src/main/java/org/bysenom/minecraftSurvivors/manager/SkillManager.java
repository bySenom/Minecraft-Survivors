package org.bysenom.minecraftSurvivors.manager;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.model.SurvivorPlayer;

/**
 * Tick-Manager für passive/aktive Zusatz-Skills (bis zu 5) pro Spieler.
 * + Passive Weapons (Vampire Survivors-Style): w_lightning, w_fire, w_ranged, w_holy
 */
public class SkillManager {
    private final MinecraftSurvivors plugin;
    private org.bukkit.scheduler.BukkitTask task;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    // Zusatz: temporale Anker für Temporal Rift (Entity -> ursprüngliche Position)
    private final java.util.Map<java.util.UUID, java.util.Map<org.bukkit.entity.LivingEntity, org.bukkit.Location>> temporalAnchors = new java.util.HashMap<>();
    // Zusatz: Lingering Void Felder (Center + Ablaufzeit)
    private final java.util.List<LingeringVoidField> lingeringVoidFields = new java.util.ArrayList<>();
    private final java.util.Map<java.util.UUID, java.util.Map<String, Long>> lastGlyphMsg = new java.util.HashMap<>();

    // Tracking für Aegis Schilde (virtueller Absorptionswert) pro Spieler
    private final java.util.Map<java.util.UUID, Double> aegisShields = new java.util.concurrent.ConcurrentHashMap<>();
    private org.bukkit.scheduler.BukkitTask aegisDecayTask;

    private static final class LingeringVoidField {
        final org.bukkit.Location center; final long expireAt; final double radius; final double damage; final java.util.UUID owner;
        LingeringVoidField(org.bukkit.Location c, long expireAt, double radius, double damage, java.util.UUID owner) { this.center=c; this.expireAt=expireAt; this.radius=radius; this.damage=damage; this.owner=owner; }
    }

    public SkillManager(MinecraftSurvivors plugin) { this.plugin = plugin; }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
        // Aegis Schild-Decay Task alle 2s (konfigurierbar später)
        aegisDecayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double decay = plugin.getConfigUtil().getDouble("heal_totem.aegis.decay-per-2s", 0.0); // Standard 0 = kein Decay
            if (decay <= 0.0) return;
            for (var entry : aegisShields.entrySet()) {
                double v = entry.getValue();
                if (v <= 0) continue;
                double nv = Math.max(0.0, v - decay);
                entry.setValue(nv);
            }
            // Visuell updaten
            for (java.util.UUID id : aegisShields.keySet()) updateAegisVisual(id);
        }, 40L, 40L);
        // Damage Abfang für Aegis: Listener registrieren falls nicht vorhanden
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler(ignoreCancelled = true)
            public void onDamage(org.bukkit.event.entity.EntityDamageEvent e) {
                if (!(e.getEntity() instanceof org.bukkit.entity.Player pl)) return;
                java.util.UUID id = pl.getUniqueId();
                double shield = aegisShields.getOrDefault(id, 0.0);
                if (shield <= 0.0) return;
                double dmg = e.getFinalDamage();
                if (dmg <= 0) return;
                if (shield >= dmg) {
                    // kompletter Schaden vom Schild absorbiert
                    aegisShields.put(id, shield - dmg);
                    e.setCancelled(true);
                    try { pl.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, pl.getLocation().add(0,1.0,0), 6, 0.3,0.3,0.3, 0.01); } catch (Throwable ignored) {}
                    updateAegisVisual(id);
                } else {
                    // Teilabsorption
                    double remain = dmg - shield;
                    aegisShields.put(id, 0.0);
                    e.setDamage(remain);
                    try { pl.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, pl.getLocation().add(0,1.0,0), 10, 0.4,0.4,0.4, 0.02); } catch (Throwable ignored) {}
                    updateAegisVisual(id);
                }
            }
        }, plugin);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        if (aegisDecayTask != null) { aegisDecayTask.cancel(); aegisDecayTask = null; }
    }

    public void clearLingeringEffects() {
        lingeringVoidFields.clear();
        temporalAnchors.clear();
        aegisShields.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        org.bysenom.minecraftSurvivors.model.GameState gs = null;
        try { gs = plugin.getGameManager().getState(); } catch (Throwable ignored) {}
        boolean running = gs == org.bysenom.minecraftSurvivors.model.GameState.RUNNING;
        for (Player p : Bukkit.getOnlinePlayers()) {
            SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
            if (sp == null) continue;
            if (plugin.getGameManager().isPlayerPaused(p.getUniqueId())) continue;
            // Vor Spielstart keine automatischen Klassenfähigkeiten ticken
            if (!running || sp.getSelectedClass() == null) {
                renderHotbar(p, sp); // nur Anzeige, keine Fähigkeiten
                continue;
            }
            renderHotbar(p, sp);
            for (String ab : sp.getAbilities()) {
                int lvl = sp.getAbilityLevel(ab);
                runAbility(p, sp, ab, lvl);
            }
        }
        // Nach allen Spieler-Fähigkeiten lingering Felder ticken
        tickLingeringVoidFields();
    }

    private void renderHotbar(Player p, SurvivorPlayer sp) {
        java.util.List<String> list = sp.getAbilities();
        int idx = 0;
        for (String ab : list) {
            if (idx >= 5) break;
            org.bysenom.minecraftSurvivors.ability.AbilityCatalog.Def def = org.bysenom.minecraftSurvivors.ability.AbilityCatalog.get(ab);
            if (def == null) continue;
            int lvl = sp.getAbilityLevel(ab);
            org.bysenom.minecraftSurvivors.ability.AbilityCatalog.Stats stats = def.compute(sp, lvl);
            org.bukkit.inventory.ItemStack it = new org.bukkit.inventory.ItemStack(def.icon);
            org.bukkit.inventory.meta.ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.Component.text(def.display + " Lv." + lvl).color(net.kyori.adventure.text.format.NamedTextColor.AQUA));
                java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                lore.add(net.kyori.adventure.text.Component.text("§8———"));
                lore.add(net.kyori.adventure.text.Component.text("LEVEL: "+lvl, net.kyori.adventure.text.format.NamedTextColor.AQUA));
                lore.add(net.kyori.adventure.text.Component.text(String.format("⚔ DMG: %.2f", stats.damage), net.kyori.adventure.text.format.NamedTextColor.GOLD));
                lore.add(net.kyori.adventure.text.Component.text(String.format("⟳ ATK SPD: %.2f/s", stats.attacksPerSec), net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                if (stats.radius > 0.0) lore.add(net.kyori.adventure.text.Component.text(String.format("◯ RADIUS: %.1f", stats.radius), net.kyori.adventure.text.format.NamedTextColor.GREEN));
                if (stats.duration > 0.0) lore.add(net.kyori.adventure.text.Component.text(String.format("⏱ DURATION: %.1fs", stats.duration), net.kyori.adventure.text.format.NamedTextColor.GRAY));

                // Sockel-Bereich (immer 3 Slots anzeigen)
                lore.add(net.kyori.adventure.text.Component.text("§8———"));
                lore.add(net.kyori.adventure.text.Component.text("SOCKEL:").color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
                java.util.List<String> glyphs = sp.getGlyphs(ab);
                for (int s = 0; s < 3; s++) {
                    String gk = (s < glyphs.size()) ? glyphs.get(s) : null;
                    if (gk != null && !gk.isEmpty()) {
                        org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.Def gd = org.bysenom.minecraftSurvivors.glyph.GlyphCatalog.get(gk);
                        String name = gd != null ? gd.name : gk;
                        lore.add(net.kyori.adventure.text.Component.text(String.format("◈ SOCKEL %d: %s", s+1, name)).color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
                        // zeige die glyph-beschreibung wie bei WoW items (klein, grau/italic)
                        if (gd != null) {
                            java.util.List<net.kyori.adventure.text.Component> glore = gd.lore(sp);
                            for (net.kyori.adventure.text.Component c : glore) {
                                // make glyph lore slightly indented/styled
                                lore.add(net.kyori.adventure.text.Component.text("  ").append(c.color(net.kyori.adventure.text.format.NamedTextColor.GRAY)).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, true));
                            }
                        }
                    } else {
                        lore.add(net.kyori.adventure.text.Component.text("SOCKEL: EMPTY").color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
                    }
                }

                lore.add(net.kyori.adventure.text.Component.text("§8———"));
                meta.lore(lore);
                try { meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true); meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);} catch (Throwable ignored) {}
                it.setItemMeta(meta);
            }
            // Hotbar Slots 0..4 reservieren
            p.getInventory().setItem(idx, it);
            idx++;
        }
    }

    private boolean onCd(UUID id, String key, long cdMs) {
        long now = System.currentTimeMillis();
        Map<String, Long> map = cooldowns.computeIfAbsent(id, k -> new HashMap<>());
        Long last = map.getOrDefault(key, 0L);
        if (now - last < cdMs) return true;
        map.put(key, now);
        return false;
    }

    private void runAbility(Player p, SurvivorPlayer sp, String ab, int lvl) {
        switch (ab) {
            case "ab_lightning": runWLightning(p, sp, lvl); break;
            case "ab_fire": runWFire(p, sp, lvl); break;
            case "ab_ranged": runWRanged(p, sp, lvl); break;
            case "ab_holy": runWHoly(p, sp, lvl); break;
            case "ab_shockwave": runShockwave(p, lvl); break;
            case "ab_frost_nova": runFrostNova(p, lvl); break;
            case "ab_heal_totem": runHealTotem(p, lvl); break;
            case "ab_void_nova": runVoidNova(p, sp, lvl); break;
            case "ab_time_rift": runTimeRift(p, sp, lvl); break;
            case "ab_venom_spire": runVenomSpire(p, sp, lvl); break;
            default: break;
        }
    }

    // --- Weapons ---
    private void runWLightning(Player p, SurvivorPlayer sp, int lvl) {
        long base = Math.max(400, 1600 - lvl * 120L);
        long cd = (long) Math.max(150.0, base / Math.max(1.0, 1.0 + sp.getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "w_lightning", cd)) return;
        double damage = 2.0 + lvl * 0.8 + sp.getFlatDamage();
        double radius = 8.0 * (1.0 + sp.getRadiusMult());
        List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(p.getLocation(), radius);
        if (mobs.isEmpty()) return;
        org.bukkit.entity.LivingEntity target = mobs.get(new java.util.Random().nextInt(mobs.size()));
        try {
            target.getWorld().strikeLightningEffect(target.getLocation());
            target.damage(damage * (1.0 + sp.getDamageMult()), p);
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0,1.0,0), 14, 0.3,0.3,0.3, 0.02);
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 2.0f);
        } catch (Throwable ignored) {}
        // Glyph-Trigger
        int hits = sp.incCounter("glc_lightning_hits", 1);
        java.util.List<String> glyphs = sp.getGlyphs("ab_lightning");
        if (!glyphs.isEmpty()) {
            // Überladung: alle 100 Hits massiver Schlag
            if (glyphs.contains("ab_lightning:overcharge") && hits % 100 == 0) {
                try {
                    for (org.bukkit.entity.LivingEntity le : mobs) {
                        le.damage(damage * 3.0 * (1.0 + sp.getDamageMult()), p);
                        le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0,1.0,0), 8, 0.3,0.3,0.3, 0.02);
                    }
                    p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 0.6f);
                } catch (Throwable ignored) {}
                glyphProcNotify(p, "ab_lightning:overcharge", target.getLocation());
            }
            // Genkidama: kleine Chance, großen Meteor zu rufen
            if (glyphs.contains("ab_lightning:genkidama") && java.util.concurrent.ThreadLocalRandom.current().nextInt(20) == 0) {
                org.bukkit.Location center = target.getLocation();
                for (int i=0;i<6;i++) {
                    org.bukkit.Location l = center.clone().add((java.util.concurrent.ThreadLocalRandom.current().nextDouble()-0.5)*3, 8+i*0.6, (java.util.concurrent.ThreadLocalRandom.current().nextDouble()-0.5)*3);
                    center.getWorld().spawnParticle(Particle.END_ROD, l, 4, 0.1,0.1,0.1, 0.0);
                }
                for (org.bukkit.entity.LivingEntity le : mobs) {
                    if (le.getLocation().distanceSquared(center) < 6*6) {
                        le.damage(damage * 2.2 * (1.0 + sp.getDamageMult()), p);
                    }
                }
                try { p.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);} catch (Throwable ignored) {}
                glyphProcNotify(p, "ab_lightning:genkidama", target.getLocation());
            }
            // Sturmkette
            if (glyphs.contains("ab_lightning:storm_chain") && mobs.size() > 1) {
                org.bukkit.entity.LivingEntity extra = mobs.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(mobs.size()));
                if (!extra.equals(target)) {
                    try { extra.damage(damage * 0.6 * (1.0 + sp.getDamageMult()), p); extra.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, extra.getLocation().add(0,1.0,0), 10, 0.2,0.2,0.2, 0.02);} catch (Throwable ignored) {}
                }
                glyphProcNotify(p, "ab_lightning:storm_chain", target.getLocation());
            }
        }
        // synergy: lightning + fire -> ignite
        if (sp.getWeapons().contains("w_fire")) {
            int igniteTicks = 40 + sp.getIgniteBonusTicks();
            try { target.setFireTicks(Math.max(target.getFireTicks(), igniteTicks)); } catch (Throwable ignored) {}
        }
    }

    private void runWFire(Player p, SurvivorPlayer sp, int lvl) {
        long base = Math.max(400, 1400 - lvl * 100L);
        long cd = (long) Math.max(150.0, base / Math.max(1.0, 1.0 + sp.getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "w_fire", cd)) return;
        double radius = 4.0 + lvl * 0.5;
        double damage = 0.8 + lvl * 0.4 + sp.getFlatDamage() * 0.3;
        List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(p.getLocation(), radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try {
                le.setFireTicks(Math.max(le.getFireTicks(), 40 + sp.getIgniteBonusTicks()));
                le.damage(damage * (1.0 + sp.getDamageMult()), p);
                le.getWorld().spawnParticle(Particle.FLAME, le.getLocation().add(0,1.0,0), 6, 0.25,0.25,0.25, 0.01);
            } catch (Throwable ignored) {}
        }
        try { p.playSound(p.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 0.3f, 1.6f); } catch (Throwable ignored) {}
    }

    private void runWRanged(Player p, SurvivorPlayer sp, int lvl) {
        long base = Math.max(300, 900 - lvl * 60L);
        long cd = (long) Math.max(120.0, base / Math.max(1.0, 1.0 + sp.getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "w_ranged", cd)) return;
        double range = 16.0 + lvl * 2.0;
        double damage = 1.8 + lvl * 0.6 + sp.getFlatDamage() * 0.6;
        List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(p.getLocation(), range);
        if (mobs.isEmpty()) return;
        org.bukkit.entity.LivingEntity target = mobs.get(0);
        org.bukkit.Location eye = p.getEyeLocation();
        org.bukkit.util.Vector dir = target.getLocation().toVector().subtract(eye.toVector()).normalize();
        org.bukkit.Location cur = eye.clone();
        double speed = 1.3 + 0.05 * lvl;
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0; @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                cur.add(dir.clone().multiply(speed));
                cur.getWorld().spawnParticle(Particle.CRIT, cur, 2, 0.02,0.02,0.02, 0.0);
                if (cur.distanceSquared(target.getLocation()) < 1.0) {
                    try { target.damage(damage * (1.0 + sp.getDamageMult()), p); } catch (Throwable ignored) {}
                    try { p.playSound(cur, Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.6f); } catch (Throwable ignored) {}
                    cancel(); return;
                }
                if (++t > 40) cancel();
            }}.runTaskTimer(plugin, 0L, 1L);
    }

    private void runWHoly(Player p, SurvivorPlayer sp, int lvl) {
        long base = Math.max(600, 1800 - lvl * 120L);
        long cd = (long) Math.max(200.0, base / Math.max(1.0, 1.0 + sp.getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "w_holy", cd)) return;
        double radius = 5.0 + lvl * 0.6 + sp.getRadiusMult() * 2.0;
        double damage = 1.2 + lvl * 0.5 + sp.getFlatDamage() * 0.4;
        org.bukkit.Location loc = p.getLocation();
        List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(loc, radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try { le.damage(damage * (1.0 + sp.getDamageMult()), p); } catch (Throwable ignored) {}
        }
        try {
            p.getWorld().spawnParticle(Particle.END_ROD, loc.add(0,1.0,0), 28, radius/2, 0.3, radius/2, 0.0);
            p.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.8f);
        } catch (Throwable ignored) {}
        // synergy: holy burst on kill handled in EntityDeathListener optional (future)
    }

    // --- Existing skills ---
    private void runShockwave(Player p, int lvl) {
        double base = plugin.getConfigUtil().getDouble("skills.shockwave.cooldown-ms", 800);
        long cd = (long) Math.max(150.0, base / Math.max(1.0, 1.0 + plugin.getPlayerManager().get(p.getUniqueId()).getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "shockwave", cd)) return;
        double radius = 4.0 + lvl * 0.6;
        double damage = 1.5 + lvl * 0.5;
        org.bukkit.Location loc = p.getLocation();
        java.util.List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(loc, radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try {
                le.damage(damage, p);
                org.bukkit.util.Vector v = le.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.6).setY(0.25);
                le.setVelocity(v);
            } catch (Throwable ignored) {}
        }
        try {
            p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.add(0,1.0,0), 18, 1.0, 0.2, 1.0, 0.0);
            p.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.0f);
        } catch (Throwable ignored) {}
    }

    private void runHealTotem(Player p, int lvl) {
        long base = 3000 - Math.min(2000, lvl * 200);
        long cd = (long) Math.max(300.0, base / Math.max(1.0, 1.0 + plugin.getPlayerManager().get(p.getUniqueId()).getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "heal_totem", cd)) return;
        double heal = 0.6 + lvl * 0.2;
        double radius = 6.0 + lvl * 0.5;
        java.util.List<String> glyphs = plugin.getPlayerManager().get(p.getUniqueId()).getGlyphs("ab_heal_totem");
        boolean aegis = glyphs.contains("ab_heal_totem:aegis");
        double aegisChance = plugin.getConfigUtil().getDouble("heal_totem.aegis.proc-chance", 0.12); // 12% default
        double aegisMultiplier = plugin.getConfigUtil().getDouble("heal_totem.aegis.multiplier", 2.5); // Schildwert = heal * multiplier
        for (Player other : p.getWorld().getPlayers()) {
            if (!other.getWorld().equals(p.getWorld())) continue;
            if (other.getLocation().distanceSquared(p.getLocation()) <= radius*radius) {
                try {
                    double before = other.getHealth();
                    double max = other.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    if (aegis && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < aegisChance) {
                        double shieldAmount = heal * aegisMultiplier;
                        addAegisShield(other, shieldAmount);
                        other.getWorld().spawnParticle(org.bukkit.Particle.SPELL_INSTANT, other.getLocation().add(0,1.0,0), 8, 0.3, 0.4, 0.3, 0.01);
                        other.playSound(other.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 0.5f, 1.2f);
                        if (other.equals(p)) glyphProcNotify(p, "ab_heal_totem:aegis", p.getLocation());
                    } else {
                        double newH = Math.min(max, before + heal);
                        other.setHealth(newH);
                        double healed = Math.max(0.0, newH - before);
                        if (healed > 0) {
                            try { plugin.getStatsMeterManager().recordHeal(p.getUniqueId(), healed); } catch (Throwable ignored) {}
                        }
                        other.spawnParticle(org.bukkit.Particle.HEART, other.getLocation().add(0,1.0,0), 4, 0.2, 0.2, 0.2, 0.0);
                    }
                } catch (Throwable ignored) {}
            }
        }
        try { p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.2f); } catch (Throwable ignored) {}
    }

    private void runFrostNova(Player p, int lvl) {
        long base = 5000 - Math.min(3000, lvl * 250);
        long cd = (long) Math.max(300.0, base / Math.max(1.0, 1.0 + plugin.getPlayerManager().get(p.getUniqueId()).getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "frost_nova", cd)) return;
        double radius = 5.0 + lvl * 0.5;
        double damage = 1.0 + lvl * 0.3;
        org.bukkit.Location loc = p.getLocation();
        java.util.List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(loc, radius);
        for (org.bukkit.entity.LivingEntity le : mobs) {
            try {
                le.damage(damage, p);
                le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40 + lvl*10, 1, false, false, true));
                le.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, le.getLocation().add(0,1.0,0), 8, 0.2, 0.4, 0.2, 0.01);
            } catch (Throwable ignored) {}
        }
        try { p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 0.5f, 1.6f); } catch (Throwable ignored) {}
    }

    private void runVoidNova(Player p, SurvivorPlayer sp, int lvl) {
        long base = Math.max(800, 2200 - lvl * 140L);
        long cd = (long) Math.max(200.0, base / Math.max(1.0, 1.0 + sp.getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "ab_void_nova", cd)) return;
        double radius = 5.0 + lvl * 0.7 + sp.getRadiusMult() * 2.0;
        double damage = (2.2 + lvl * 0.9 + sp.getFlatDamage() * 0.5) * (1.0 + sp.getDamageMult());
        double durationSec = 2.0 + Math.min(6.0, lvl * 0.25);
        org.bukkit.Location center = p.getLocation();
        java.util.List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(center, radius);
        // Glyphen
        java.util.List<String> glyphs = sp.getGlyphs("ab_void_nova");
        boolean gravityWell = glyphs.contains("ab_void_nova:gravity_well");
        boolean rupture = glyphs.contains("ab_void_nova:rupture");
        boolean lingering = glyphs.contains("ab_void_nova:lingering_void");
        if (gravityWell) glyphProcNotify(p, "ab_void_nova:gravity_well", center);
        // Partikel + Ticks
        int ticks = (int) Math.round(durationSec * 20);
        try { p.playSound(center, org.bukkit.Sound.ENTITY_SHULKER_BULLET_HIT, 0.6f, 0.4f); } catch (Throwable ignored) {}
        final int[] t = {0};
        org.bukkit.scheduler.BukkitRunnable task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (t[0] >= ticks) {
                    cancel();
                    if (rupture) {
                        try { center.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, center.clone().add(0,1,0), 120, radius/2, 0.6, radius/2, 0.2); } catch (Throwable ignored) {}
                        for (org.bukkit.entity.LivingEntity le : mobs) {
                            if (!le.isValid()) continue;
                            if (le.getLocation().distanceSquared(center) <= radius * radius) {
                                try { le.damage(damage * 1.5, p); } catch (Throwable ignored) {}
                            }
                        }
                        try { p.playSound(center, org.bukkit.Sound.ENTITY_ENDERMAN_SCREAM, 0.7f, 0.6f); } catch (Throwable ignored) {}
                        glyphProcNotify(p, "ab_void_nova:rupture", center);
                    }
                    if (lingering) {
                        // Lingering Void: Feld speichert Besitzer -> Damage Attribution für DPS
                        lingeringVoidFields.add(new LingeringVoidField(center.clone(), System.currentTimeMillis() + 3500, Math.max(2.5, radius * 0.6), damage * 0.25, p.getUniqueId()));
                        glyphProcNotify(p, "ab_void_nova:lingering_void", center);
                    }
                    return;
                }
                double prog = t[0] / (double) ticks; // 0..1
                double currentR = Math.max(0.5, radius * prog);
                // Puls Partikel-Ring
                for (int i=0;i<Math.max(32, (int)(currentR*10));i++) {
                    double ang = 2 * Math.PI * i / Math.max(32,(int)(currentR*10));
                    double x = center.getX() + Math.cos(ang) * currentR;
                    double z = center.getZ() + Math.sin(ang) * currentR;
                    org.bukkit.Location l = new org.bukkit.Location(center.getWorld(), x, center.getY()+0.2+prog*1.2, z);
                    try { center.getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, l, 1, 0.05,0.05,0.05, 0.0); } catch (Throwable ignored) {}
                }
                // Schaden + Pull
                for (org.bukkit.entity.LivingEntity le : plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(center, currentR)) {
                    if (!le.isValid()) continue;
                    try { le.damage(damage/Math.max(4, ticks/20.0), p); } catch (Throwable ignored) {}
                    if (gravityWell) {
                        org.bukkit.util.Vector pull = center.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.15);
                        pull.setY(0.02);
                        try { le.setVelocity(le.getVelocity().add(pull)); } catch (Throwable ignored) {}
                    }
                }
                t[0]++;
            }
        }; task.runTaskTimer(plugin, 0L, 1L);
    }

    private void runTimeRift(Player p, SurvivorPlayer sp, int lvl) {
        long base = Math.max(1000, 3000 - lvl * 160L);
        long cd = (long) Math.max(220.0, base / Math.max(1.0, 1.0 + sp.getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "ab_time_rift", cd)) return;
        double radius = 4.0 + lvl * 0.5;
        double durSec = 1.0 + Math.min(4.0, lvl * 0.15);
        org.bukkit.Location center = p.getLocation();
        java.util.List<String> glyphs = sp.getGlyphs("ab_time_rift");
        boolean hasteBurst = glyphs.contains("ab_time_rift:haste_burst");
        boolean slowField = glyphs.contains("ab_time_rift:slow_field");
        boolean anchor = glyphs.contains("ab_time_rift:temporal_anchor");
        if (hasteBurst) glyphProcNotify(p, "ab_time_rift:haste_burst", center);
        if (slowField) glyphProcNotify(p, "ab_time_rift:slow_field", center);
        if (anchor) glyphProcNotify(p, "ab_time_rift:temporal_anchor", center);
        int ticks = (int)Math.round(durSec*20);
        // Speichere Anker-Positionen
        if (anchor) {
            java.util.Map<org.bukkit.entity.LivingEntity, org.bukkit.Location> map = new java.util.HashMap<>();
            for (org.bukkit.entity.LivingEntity le : plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(center, radius)) {
                map.put(le, le.getLocation().clone());
            }
            temporalAnchors.put(p.getUniqueId(), map);
        }
        try { p.playSound(center, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 0.3f); } catch (Throwable ignored) {}
        // Haste Burst -> kurzer effekt
        if (hasteBurst) {
            try { p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, Math.max(40, ticks), 1, false, false, true)); } catch (Throwable ignored) {}
        }
        final int[] t = {0};
        org.bukkit.scheduler.BukkitRunnable task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() {
                if (!p.isOnline()) { cancel(); return; }
                if (t[0] >= ticks) {
                    cancel();
                    if (anchor) {
                        java.util.Map<org.bukkit.entity.LivingEntity, org.bukkit.Location> map = temporalAnchors.remove(p.getUniqueId());
                        if (map != null) {
                            for (var e : map.entrySet()) {
                                org.bukkit.entity.LivingEntity le = e.getKey();
                                if (le != null && le.isValid()) {
                                    try { le.teleport(e.getValue()); } catch (Throwable ignored) {}
                                }
                            }
                            try { p.playSound(center, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.4f); } catch (Throwable ignored) {}
                        }
                    }
                    return;
                }
                double prog = t[0]/(double)ticks;
                double r = radius;
                for (int i=0;i<24;i++) {
                    double ang = 2*Math.PI*i/24.0 + prog*Math.PI*2;
                    double x = center.getX()+Math.cos(ang)*r;
                    double z = center.getZ()+Math.sin(ang)*r;
                    org.bukkit.Location l = new org.bukkit.Location(center.getWorld(), x, center.getY()+0.2, z);
                    try { center.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, l, 1, 0.05,0.05,0.05,0.0); } catch (Throwable ignored) {}
                }
                // Effekte auf Mobs: Slowness verstärkt durch slowField
                for (org.bukkit.entity.LivingEntity le : plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(center, r)) {
                    try {
                        int amp = slowField ? 2 : 1;
                        le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 20, amp, false, false, true));
                    } catch (Throwable ignored) {}
                }
                t[0]++;
            }
        }; task.runTaskTimer(plugin, 0L, 1L);
    }

    private void runVenomSpire(Player p, SurvivorPlayer sp, int lvl) {
        long base = Math.max(600, 2000 - lvl * 130L);
        long cd = (long) Math.max(180.0, base / Math.max(1.0, 1.0 + sp.getAttackSpeedMult()));
        if (onCd(p.getUniqueId(), "ab_venom_spire", cd)) return;
        // Suche Gegner im Umkreis (Suchradius)
        double searchRadius = 12.0 + lvl * 0.8 + sp.getRadiusMult() * 2.0;
        java.util.List<org.bukkit.entity.LivingEntity> mobs = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(p.getLocation(), searchRadius);
        if (mobs.isEmpty()) return;
        // Anzahl Spitzen abhängig vom Level (1..5)
        int spireCount = Math.min(5, 1 + (lvl / 2));
        // Glyphen
        java.util.List<String> glyphs = sp.getGlyphs("ab_venom_spire");
        boolean toxicBloom = glyphs.contains("ab_venom_spire:toxic_bloom");
        boolean neurotoxin = glyphs.contains("ab_venom_spire:neurotoxin");
        boolean corrosive = glyphs.contains("ab_venom_spire:corrosive_venom");
        if (toxicBloom) glyphProcNotify(p, "ab_venom_spire:toxic_bloom", p.getLocation());
        // neurotoxin/corrosive procs im Tick bei Anwendung
        // Schaden und Dauer
        double baseDps = (0.9 + lvl * 0.45 + sp.getFlatDamage() * 0.25) * (1.0 + sp.getDamageMult());
        double durSec = 2.5 + Math.min(5.0, lvl * 0.25);
        int totalTicks = (int) Math.round(durSec * 20);
        // Wähle Ziel-Gegner (näheste zuerst)
        mobs.sort(java.util.Comparator.comparingDouble(m -> m.getLocation().distanceSquared(p.getLocation())));
        int spawned = 0;
        for (org.bukkit.entity.LivingEntity target : mobs) {
            if (spawned >= spireCount) break;
            if (target == null || !target.isValid() || target.getWorld() != p.getWorld()) continue;
            org.bukkit.Location baseLoc = target.getLocation().clone();
            try { baseLoc.setY(baseLoc.getWorld().getHighestBlockYAt(baseLoc) + 1); } catch (Throwable ignored) {}
            final org.bukkit.Location c = baseLoc;
            final double spireRadius = Math.max(1.0, (toxicBloom ? 1.6 : 1.2));
            final double perTickDamage = Math.max(0.05, baseDps / Math.max(5.0, totalTicks / 20.0));
            try { p.playSound(c, org.bukkit.Sound.BLOCK_BASALT_BREAK, 0.7f, 0.8f); } catch (Throwable ignored) {}
            final int[] t = {0};
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override public void run() {
                    if (!p.isOnline()) { cancel(); return; }
                    if (t[0] >= totalTicks) { cancel(); return; }
                    double prog = t[0] / (double) totalTicks; // 0..1
                    // Spire-Visual: Partikel-Säule + Bodenrisse
                    int column = 6;
                    for (int i=0;i<column;i++) {
                        org.bukkit.Location l = c.clone().add(0, 0.2 + i*0.3, 0);
                        try { c.getWorld().spawnParticle(org.bukkit.Particle.CRIT, l, 2, 0.02,0.02,0.02, 0.0); } catch (Throwable ignored) {}
                    }
                    int ringPts = 10;
                    double rr = spireRadius;
                    for (int i=0;i<ringPts;i++) {
                        double ang = 2*Math.PI*i/ringPts;
                        double x = c.getX()+Math.cos(ang)*rr;
                        double z = c.getZ()+Math.sin(ang)*rr;
                        org.bukkit.Location l = new org.bukkit.Location(c.getWorld(), x, c.getY()+0.1+Math.sin(prog*10)*0.1, z);
                        try { c.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, l, 1, 0.03,0.03,0.03, 0.0); } catch (Throwable ignored) {}
                    }
                    // Schaden/Effects für Gegner in Spire-Nähe (fokussiert)
                    java.util.List<org.bukkit.entity.LivingEntity> near = plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(c, spireRadius + 0.4);
                    for (org.bukkit.entity.LivingEntity le : near) {
                        if (!le.isValid()) continue;
                        try { le.damage(perTickDamage, p); } catch (Throwable ignored) {}
                        try { le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 40, 0, false, false, true)); } catch (Throwable ignored) {}
                        if (neurotoxin && java.util.concurrent.ThreadLocalRandom.current().nextInt(20) == 0) {
                            try { le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 2, false, false, true)); } catch (Throwable ignored) {}
                            glyphProcNotify(p, "ab_venom_spire:neurotoxin", le.getLocation());
                        }
                        if (corrosive && java.util.concurrent.ThreadLocalRandom.current().nextInt(16) == 0) {
                            try { le.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 80, 1, false, false, true)); } catch (Throwable ignored) {}
                            glyphProcNotify(p, "ab_venom_spire:corrosive_venom", le.getLocation());
                        }
                    }
                    if (t[0] % 10 == 0) { try { c.getWorld().playSound(c, org.bukkit.Sound.BLOCK_STONE_BREAK, 0.4f, 0.9f); } catch (Throwable ignored) {} }
                    t[0]++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
            spawned++;
        }
    }

    // Tick für Lingering Void Felder (in bestehendem tick() integrieren)
    private void tickLingeringVoidFields() {
        long now = System.currentTimeMillis();
        if (lingeringVoidFields.isEmpty()) return;
        java.util.Iterator<LingeringVoidField> it = lingeringVoidFields.iterator();
        while (it.hasNext()) {
            LingeringVoidField f = it.next();
            if (now >= f.expireAt) { it.remove(); continue; }
            // Partikelteppich
            for (int i=0;i<12;i++) {
                double ang = 2*Math.PI*i/12.0 + (now%2000)/2000.0*2*Math.PI;
                double x = f.center.getX()+Math.cos(ang)*f.radius;
                double z = f.center.getZ()+Math.sin(ang)*f.radius;
                org.bukkit.Location l = new org.bukkit.Location(f.center.getWorld(), x, f.center.getY()+0.1, z);
                try { f.center.getWorld().spawnParticle(org.bukkit.Particle.ASH, l, 1, 0.02,0.02,0.02,0.0); } catch (Throwable ignored) {}
            }
            // Schaden anwenden (mit Attribution an Owner für DPS)
            org.bukkit.entity.Player ownerPl = f.owner == null ? null : org.bukkit.Bukkit.getPlayer(f.owner);
            for (org.bukkit.entity.LivingEntity le : plugin.getGameManager().getSpawnManager().getNearbyWaveMobs(f.center, f.radius)) {
                try {
                    if (ownerPl != null && ownerPl.isOnline()) {
                        le.damage(f.damage/2.0, ownerPl); // triggert EntityDamageByEntityEvent -> StatsMeterManager.recordDamage
                    } else {
                        le.damage(f.damage/2.0); // Fallback ohne Attribution
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private void glyphProcNotify(org.bukkit.entity.Player p, String glyphKey, org.bukkit.Location where) {
        if (p == null || glyphKey == null) return;
        long now = System.currentTimeMillis();
        java.util.Map<String, Long> m = lastGlyphMsg.computeIfAbsent(p.getUniqueId(), k -> new java.util.HashMap<>());
        Long last = m.getOrDefault(glyphKey, 0L);
        if (now - last < 1000L) return; // anti-spam 1s
        m.put(glyphKey, now);
        try { p.sendMessage(net.kyori.adventure.text.Component.text("Glyph aktiviert: "+glyphKey).color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)); } catch (Throwable ignored) {}
        // kleine Partikel-Signatur je Glyph
        try {
            org.bukkit.Particle particle = org.bukkit.Particle.END_ROD;
            if (glyphKey.endsWith("genkidama")) particle = org.bukkit.Particle.END_ROD; // GLOW ersetzt -> END_ROD
            else if (glyphKey.endsWith("storm_chain")) particle = org.bukkit.Particle.ELECTRIC_SPARK;
            else if (glyphKey.endsWith("overcharge")) particle = org.bukkit.Particle.CRIT;
            else if (glyphKey.endsWith("inferno") || glyphKey.endsWith("phoenix") || glyphKey.endsWith("combust")) particle = org.bukkit.Particle.FLAME;
            else if (glyphKey.endsWith("multishot") || glyphKey.endsWith("ricochet") || glyphKey.endsWith("headshot")) particle = org.bukkit.Particle.CRIT;
            else if (glyphKey.endsWith("consecration") || glyphKey.endsWith("penance")) particle = org.bukkit.Particle.END_ROD;
            else if (glyphKey.endsWith("divine_shield")) particle = org.bukkit.Particle.HEART;
            else if (glyphKey.endsWith("earthsplit") || glyphKey.endsWith("fracture")) particle = org.bukkit.Particle.SMOKE;
            else if (glyphKey.endsWith("vacuum")) particle = org.bukkit.Particle.PORTAL;
            else if (glyphKey.endsWith("brittle") || glyphKey.endsWith("glacier") || glyphKey.endsWith("shatter")) particle = org.bukkit.Particle.SNOWFLAKE;
            else if (glyphKey.endsWith("aegis") || glyphKey.endsWith("pulse") || glyphKey.endsWith("beacon")) particle = org.bukkit.Particle.END_ROD;
            else if (glyphKey.endsWith("gravity_well") || glyphKey.endsWith("rupture") || glyphKey.endsWith("lingering_void")) particle = org.bukkit.Particle.REVERSE_PORTAL;
            else if (glyphKey.endsWith("toxic_bloom") || glyphKey.endsWith("neurotoxin") || glyphKey.endsWith("corrosive_venom")) particle = Particle.ITEM_SLIME; // falls nicht vorhanden -> fallback unten
            // Fallback, wenn SLIME nicht existiert
            try { org.bukkit.Particle.valueOf("SLIME"); } catch (Throwable ex) { particle = Particle.HAPPY_VILLAGER; }
            org.bukkit.Location l = where != null ? where : p.getLocation();
            l.getWorld().spawnParticle(particle, l.clone().add(0,1.0,0), 12, 0.5,0.5,0.5, 0.02);
        } catch (Throwable ignored) {}
    }
}
