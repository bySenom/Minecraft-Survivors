package org.bysenom.minecraftSurvivors.listener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

public class LootchestListener implements Listener {

    private final MinecraftSurvivors plugin;
    private final Random rnd = new Random();

    // Active floating lootchests in the world
    private static final Map<UUID, ActiveChest> CHESTS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> DISPLAY_TO_CHEST = new ConcurrentHashMap<>();
    private static org.bukkit.scheduler.BukkitTask animateTask;
    private static final java.util.Set<java.util.UUID> LOOT_PAUSED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static final class ActiveChest {
        final UUID id; final World world; final Location base;
        final UUID itemDisplayId; final UUID textDisplayId;
        final long expiresAt;
        ActiveChest(UUID id, World w, Location base, UUID itemId, UUID textId, long expiresAt) {
            this.id = id; this.world = w; this.base = base.clone(); this.itemDisplayId = itemId; this.textDisplayId = textId; this.expiresAt = expiresAt;
        }
    }

    public LootchestListener(MinecraftSurvivors plugin) { this.plugin = plugin; }

    // ---- Public API ----
    public static void spawnLootChest(Location loc) {
        MinecraftSurvivors pl = MinecraftSurvivors.getInstance();
        if (pl == null || loc == null || loc.getWorld() == null) return;
        World w = loc.getWorld();
        Location itemLoc = loc.clone().add(0, 0.6, 0);
        Location textLoc = loc.clone().add(0, 1.6, 0);
        // Item display (floating chest item)
        ItemDisplay item = w.spawn(itemLoc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.CHEST));
            try { d.setBillboard(Display.Billboard.CENTER); } catch (Throwable ignored) {}
            try { d.setInterpolationDuration(0); } catch (Throwable ignored) {}
            try { d.setRotation(0f, 0f); } catch (Throwable ignored) {}
            d.setPersistent(true);
            d.setInvulnerable(true);
        });
        // Text hologram
        TextDisplay text = w.spawn(textLoc, TextDisplay.class, t -> {
            t.text(Component.text("Lootchest").color(NamedTextColor.GOLD));
            try { t.setAlignment(TextDisplay.TextAlignment.CENTER); } catch (Throwable ignored) {}
            try { t.setBillboard(Display.Billboard.CENTER); } catch (Throwable ignored) {}
            t.setShadowed(true);
            t.setSeeThrough(true);
            t.setPersistent(true);
            t.setInvulnerable(true);
        });
        UUID id = UUID.randomUUID();
        int lifeSec = pl.getConfigUtil().getInt("lootchest.lifetime-seconds", 45);
        long expiresAt = System.currentTimeMillis() + Math.max(5, lifeSec) * 1000L;
        CHESTS.put(id, new ActiveChest(id, w, loc, item.getUniqueId(), text.getUniqueId(), expiresAt));
        DISPLAY_TO_CHEST.put(item.getUniqueId(), id);
        DISPLAY_TO_CHEST.put(text.getUniqueId(), id);
        ensureAnimateTask(pl);
    }

    private static void ensureAnimateTask(MinecraftSurvivors pl) {
        if (animateTask != null && !animateTask.isCancelled()) return;
        animateTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                int periodTicks = Math.max(10, pl.getConfigUtil().getInt("lootchest.animation.period-ticks", 80));
                long periodMs = periodTicks * 50L;
                double amplitude = pl.getConfigUtil().getDouble("lootchest.animation.bob-amplitude", 0.15);
                boolean pEnabled = pl.getConfigUtil().getBoolean("lootchest.particles.enabled", true);
                String pName = pl.getConfigUtil().getString("lootchest.particles.type", "END_ROD");
                int pCount = pl.getConfigUtil().getInt("lootchest.particles.count", 2);
                double pSpread = pl.getConfigUtil().getDouble("lootchest.particles.spread", 0.15);
                double pSpeed = pl.getConfigUtil().getDouble("lootchest.particles.speed", 0.0);
                int pChance = Math.max(1, pl.getConfigUtil().getInt("lootchest.particles.chance-1-in", 40));
                Particle pType = Particle.END_ROD;
                try { pType = Particle.valueOf(String.valueOf(pName).toUpperCase(Locale.ROOT)); } catch (Throwable ignored) {}
                int rr = pl.getConfigUtil().getInt("lootchest.particles.redstone-r", 255);
                int rg = pl.getConfigUtil().getInt("lootchest.particles.redstone-g", 255);
                int rb = pl.getConfigUtil().getInt("lootchest.particles.redstone-b", 255);
                float redstoneSize = (float) pl.getConfigUtil().getDouble("lootchest.particles.redstone-size", 1.0);

                // Animation
                for (Map.Entry<UUID, ActiveChest> en : CHESTS.entrySet()) {
                    ActiveChest ac = en.getValue();
                    if (ac == null) continue;
                    Entity item = Bukkit.getEntity(ac.itemDisplayId);
                    Entity text = Bukkit.getEntity(ac.textDisplayId);
                    if (item == null || text == null) continue;
                    double t = periodMs <= 0 ? 0.0 : ((now % periodMs) / (double) periodMs);
                    double bob = Math.sin(t * Math.PI * 2) * amplitude;
                    Location base = ac.base;
                    try {
                        item.teleport(base.clone().add(0, 0.6 + bob, 0));
                        text.teleport(base.clone().add(0, 1.6 + bob, 0));
                        item.setRotation((float)(t * 360.0), 0f);
                        if (pEnabled && java.util.concurrent.ThreadLocalRandom.current().nextInt(pChance) == 0) {
                            Location ploc = base.clone().add(0, 1.0 + bob, 0);
                            String pname = pType.name();
                            boolean isRedstone = "REDSTONE".equalsIgnoreCase(pname) || "DUST".equalsIgnoreCase(pname);
                            if (isRedstone) {
                                Particle.DustOptions dust = new Particle.DustOptions(org.bukkit.Color.fromRGB(Math.max(0, Math.min(255, rr)), Math.max(0, Math.min(255, rg)), Math.max(0, Math.min(255, rb))), Math.max(0.01f, redstoneSize));
                                base.getWorld().spawnParticle(pType, ploc, pCount, pSpread, pSpread, pSpread, 0.0, dust);
                            } else {
                                base.getWorld().spawnParticle(pType, ploc, pCount, pSpread, pSpread, pSpread, pSpeed);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                // Proximity Scan als Fallback (falls PlayerMove nicht feuert)
                if (!CHESTS.isEmpty()) {
                    double trigger = pl.getConfigUtil().getDouble("lootchest.trigger-radius", 1.6);
                    double r2 = trigger * trigger;
                    java.util.Iterator<Map.Entry<UUID, ActiveChest>> it = CHESTS.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<UUID, ActiveChest> en = it.next();
                        ActiveChest ac = en.getValue();
                        if (ac == null) { it.remove(); continue; }
                        if (now > ac.expiresAt) { despawn(en.getKey(), ac); it.remove(); continue; }
                        // Suche einen nahen Spieler in der gleichen Welt
                        Collection<? extends Player> players;
                        try {
                            players = ac.world.getPlayers();
                        } catch (Throwable t) {
                            players = Bukkit.getOnlinePlayers();
                        }
                        Player nearest = null;
                        for (Player p : players) {
                            if (!p.isOnline()) continue;
                            if (!p.getWorld().equals(ac.world)) continue;
                            Location l = p.getLocation();
                            double dx = l.getX() - ac.base.getX();
                            double dz = l.getZ() - ac.base.getZ();
                            if (dx*dx + dz*dz <= r2) { nearest = p; break; }
                        }
                        if (nearest != null) {
                            UUID chestId = en.getKey();
                            it.remove();
                            despawn(chestId, ac);
                            if (pl.getConfigUtil().getBoolean("debug.lootchest", false)) {
                                pl.getLogger().info("Lootchest open(fallback): "+nearest.getName()+" @ "+ac.base);
                            }
                            openForPlayer(pl, nearest);
                            break; // pro Tick nur eine Kiste öffnen
                        }
                    }
                }
            }
        }.runTaskTimer(pl, 0L, 2L);
    }

    private static void openForPlayer(MinecraftSurvivors pl, Player p) {
        try {
            if (pl.getGameManager() != null && pl.getGameManager().isPlayerPaused(p.getUniqueId())) {
                pl.getGameManager().enqueueLoot(p.getUniqueId());
            } else {
                pauseForLoot(pl, p);
                new LootchestListener(pl).openLootGui(p);
            }
        } catch (Throwable ignored) {}
    }

    public static void openQueued(Player p) {
        if (p == null) return;
        MinecraftSurvivors pl = MinecraftSurvivors.getInstance();
        if (pl == null) return;
        try {
            pauseForLoot(pl, p);
        } catch (Throwable ignored) {}
        new LootchestListener(pl).openLootGui(p);
    }

    // ---- Proximity trigger ----
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent e) {
        if (CHESTS.isEmpty()) return;
        Player p = e.getPlayer();
        Location to = e.getTo(); if (to == null) return;
        double trigger = plugin.getConfigUtil().getDouble("lootchest.trigger-radius", 1.6);
        double r2 = trigger * trigger;
        long now = System.currentTimeMillis();
        UUID hitId = null;
        for (Map.Entry<UUID, ActiveChest> en : CHESTS.entrySet()) {
            ActiveChest ac = en.getValue();
            if (ac == null) continue;
            if (!ac.world.equals(to.getWorld())) {
                if (now > ac.expiresAt) { despawn(en.getKey(), ac); }
                continue;
            }
            if (now > ac.expiresAt) { despawn(en.getKey(), ac); continue; }
            double dx = to.getX() - ac.base.getX();
            double dz = to.getZ() - ac.base.getZ();
            if (dx*dx + dz*dz <= r2) { hitId = en.getKey(); break; }
        }
        if (hitId != null) {
            ActiveChest ac = CHESTS.remove(hitId);
            if (ac != null) despawn(hitId, ac);
            if (plugin.getConfigUtil().getBoolean("debug.lootchest", false)) {
                plugin.getLogger().info("Lootchest open(move): "+p.getName()+" @ "+(ac!=null?ac.base:to));
            }
            openForPlayer(plugin, p);
        }
    }

    private static void despawn(UUID id, ActiveChest ac) {
        if (ac == null) return;
        try { Entity ei = Bukkit.getEntity(ac.itemDisplayId); if (ei != null) ei.remove(); DISPLAY_TO_CHEST.remove(ac.itemDisplayId); } catch (Throwable ignored) {}
        try { Entity et = Bukkit.getEntity(ac.textDisplayId); if (et != null) et.remove(); DISPLAY_TO_CHEST.remove(ac.textDisplayId); } catch (Throwable ignored) {}
        CHESTS.remove(id);
    }

    // ---- GUI (Casino-Style) ----
    private void openLootGui(Player p) {
        List<WeightedReward> pool = loadWeightedRewards();
        WeightedReward hit = pickWeighted(pool);
        List<ItemStack> symbols = buildSymbolIcons(pool);
        Inventory inv = Bukkit.createInventory(new LootGuiHolder(), 27, Component.text("Lootchest").color(NamedTextColor.GOLD));
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        p.openInventory(inv);
        new ReelsTask(p, inv, symbols, hit).runTaskTimer(plugin, 0L, 1L);
    }

    private static class LootGuiHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof LootGuiHolder)) return;
        e.setCancelled(true);
    }

    @org.bukkit.event.EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof LootGuiHolder)) return;
        java.util.UUID uid = e.getPlayer().getUniqueId();
        try {
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getPlayer();
            plugin.getGameManager().getSpawnManager().repelMobsAround(p, 8.0, 1.2, true);
            try { int t = Math.max(1, plugin.getConfigUtil().getInt("spawn.repel-protect-ticks", 12)); plugin.getGameManager().protectPlayer(p.getUniqueId(), t); } catch(Throwable ignored){}
        } catch (Throwable ignored) {}
        // Nur resuming, wenn wir selbst pausiert haben
        if (LOOT_PAUSED.remove(uid)) {
            try { plugin.getGameManager().resumeForPlayer(uid); } catch (Throwable ignored) {}
        }
        try { plugin.getGameManager().tryOpenNextQueued(uid); } catch (Throwable ignored) {}
    }

    private List<ItemStack> buildSymbolIcons(List<WeightedReward> pool) {
        LinkedHashMap<String, ItemStack> map = new LinkedHashMap<>();
        for (WeightedReward w : pool) {
            String key = w.type.toUpperCase();
            if (!map.containsKey(key)) map.put(key, iconFor(w));
        }
        if (map.isEmpty()) map.put("PAPER", new ItemStack(Material.PAPER));
        return new ArrayList<>(map.values());
    }

    private final class ReelsTask extends org.bukkit.scheduler.BukkitRunnable {
        private final Player player; private final Inventory inv; private final List<ItemStack> symbols; private final WeightedReward hit;
        private int tick = 0;
        private int posA = rnd.nextInt(1000), posB = rnd.nextInt(2000), posC = rnd.nextInt(3000);
        private int intervalA = 1, intervalB = 1, intervalC = 1;
        private int nextAdvA = 0, nextAdvB = 0, nextAdvC = 0;
        private final int stopA = 40, stopB = 60, stopC = 80;
        private boolean stoppedA = false, stoppedB = false, stoppedC = false;
        private final int centerA = 11, centerB = 13, centerC = 15;
        ReelsTask(Player p, Inventory inv, List<ItemStack> symbols, WeightedReward hit) { this.player=p; this.inv=inv; this.symbols=symbols; this.hit=hit; }
        @Override public void run() {
            try {
                if (inv.getViewers().isEmpty() || !player.isOnline()) { cancel(); return; }
                if (!stoppedA) { if (tick >= nextAdvA) { posA++; renderColumn(centerA, posA); nextAdvA = tick + intervalA; if (tick > stopA*0.6 && intervalA < 6) intervalA++; } if (tick >= stopA) { stoppedA = true; setFinal(centerA); try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.12f, 1.6f);} catch(Throwable ignored){} } }
                if (!stoppedB) { if (tick >= nextAdvB) { posB++; renderColumn(centerB, posB); nextAdvB = tick + intervalB; if (tick > stopB*0.6 && intervalB < 6) intervalB++; } if (tick >= stopB) { stoppedB = true; setFinal(centerB); try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.12f, 1.4f);} catch(Throwable ignored){} } }
                if (!stoppedC) { if (tick >= nextAdvC) { posC++; renderColumn(centerC, posC); nextAdvC = tick + intervalC; if (tick > stopC*0.6 && intervalC < 6) intervalC++; } if (tick >= stopC) { stoppedC = true; setFinal(centerC); try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.12f, 1.2f);} catch(Throwable ignored){} } }
                if (stoppedA && stoppedB && stoppedC) { try { player.playSound(player.getLocation(), stopSoundFor(hit.type), 0.9f, 1.0f);} catch(Throwable ignored){} LootchestListener.this.applyLootReward(player, toMap(hit)); org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> player.closeInventory(), 18L); cancel(); return; }
                tick++;
            } catch (Throwable t) { cancel(); }
        }
        private void renderColumn(int center, int pos) {
            int sz = Math.max(1, symbols.size());
            ItemStack cur = symbols.get(Math.floorMod(pos, sz));
            ItemStack up = symbols.get(Math.floorMod(pos-1, sz));
            ItemStack down = symbols.get(Math.floorMod(pos+1, sz));
            safeSet(inv, center, cur);
            safeSet(inv, center-9, dim(up));
            safeSet(inv, center+9, dim(down));
        }
        private ItemStack dim(ItemStack base) {
            try {
                ItemStack clone = base.clone();
                ItemMeta im = clone.getItemMeta();
                String label = null;
                if (im != null && im.displayName() != null) {
                    label = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(im.displayName());
                }
                if (label == null || label.isEmpty()) label = clone.getType().name();
                if (im != null) { im.displayName(Component.text("· "+label+" ·").color(NamedTextColor.DARK_GRAY)); clone.setItemMeta(im); }
                return clone;
            } catch (Throwable ignored) {}
            return base;
        }
        private void setFinal(int center) {
            int sz = Math.max(1, symbols.size());
            ItemStack cur = symbols.get(Math.floorMod(center==11?posA:(center==13?posB:posC), sz));
            inv.setItem(center, cur);
        }
        @Override public void cancel() { super.cancel(); }
        // Replace earlier call: create map via outer helper
        private Map<String,Object> toMap(WeightedReward wr){ Map<String,Object> m = new HashMap<>(); m.put("type", wr.type); m.put("value", wr.value); m.put("name", wr.name); return m; }
    }
    // ---- Rewards / Icons / Sounds ----
    private static class WeightedReward { final String type; final double value; final String name; final int weight; WeightedReward(String t,double v,String n,int w){type=t;value=v;name=n;weight=w;} }

    private List<WeightedReward> loadWeightedRewards() {
        List<Map<?, ?>> list = plugin.getConfigUtil().getConfig().getMapList("lootchest.rewards");
        List<WeightedReward> out = new ArrayList<>();
        for (Map<?, ?> m : list) {
            Object oType = m.get("type");
            Object oVal = m.get("value");
            Object oName = m.get("name");
            Object oW = m.get("weight");
            String type = (oType == null) ? "UNKNOWN" : String.valueOf(oType);
            double value = (oVal == null) ? 0.0 : parseDoubleSafe(oVal);
            String name = (oName == null) ? type : String.valueOf(oName);
            int weight = (oW == null) ? 1 : parseIntSafe(oW);
            if (weight > 0) out.add(new WeightedReward(type, value, name, weight));
        }
        if (out.isEmpty()) out.add(new WeightedReward("DAMAGE_MULT", 0.1, "Attackpower +10%", 1));
        return out;
    }
    private int parseIntSafe(Object o){ try{ return (o instanceof Number)? ((Number)o).intValue(): Integer.parseInt(String.valueOf(o)); }catch(Throwable t){return 1;} }
    private double parseDoubleSafe(Object o){ try{ return (o instanceof Number)? ((Number)o).doubleValue(): Double.parseDouble(String.valueOf(o)); }catch(Throwable t){return 0.0;} }

    private WeightedReward pickWeighted(List<WeightedReward> list) {
        int total = 0; for (WeightedReward w : list) total += Math.max(0, w.weight);
        if (total <= 0) return list.get(rnd.nextInt(Math.max(1, list.size())));
        int r = rnd.nextInt(total), cur = 0; for (WeightedReward w : list){ cur += Math.max(0, w.weight); if (r < cur) return w; }
        return list.get(list.size()-1);
    }

    private ItemStack iconFor(WeightedReward wr) {
        Material m;
        switch (wr.type.toUpperCase()) {
            case "DAMAGE_MULT": m = Material.BLAZE_POWDER; break;
            case "DAMAGE_ADD": m = Material.IRON_SWORD; break;
            case "FLAT_DAMAGE": m = Material.ANVIL; break;
            case "RADIUS_MULT": m = Material.HEART_OF_THE_SEA; break;
            case "PALADIN_HEAL": m = Material.GOLDEN_APPLE; break;
            case "SPEED": m = Material.SUGAR; break;
            case "ATTACK_SPEED": m = Material.FEATHER; break;
            case "RESIST": m = Material.SHIELD; break;
            case "LUCK": m = Material.RABBIT_FOOT; break;
            case "HEALTH_HEARTS": m = Material.APPLE; break;
            case "MAX_HEALTH": m = Material.GOLDEN_CARROT; break;
            case "HP_REGEN": m = Material.HONEY_BOTTLE; break;
            case "SHIELD": m = Material.SHIELD; break;
            case "ARMOR": m = Material.DIAMOND_CHESTPLATE; break;
            case "EVASION": m = Material.RABBIT_HIDE; break;
            case "LIFESTEAL": m = Material.GHAST_TEAR; break;
            case "THORNS": m = Material.CACTUS; break;
            case "CRIT_CHANCE": m = Material.SPYGLASS; break;
            case "CRIT_DAMAGE": m = Material.NETHERITE_SWORD; break;
            case "PROJECTILE_COUNT": m = Material.FIREWORK_ROCKET; break;
            case "PROJECTILE_BOUNCE": m = Material.SLIME_BALL; break;
            case "SIZE": m = Material.SLIME_BLOCK; break;
            case "DURATION": m = Material.CLOCK; break;
            case "DAMAGE_ELITE_BOSS": m = Material.DRAGON_BREATH; break;
            case "KNOCKBACK": m = Material.PISTON; break;
            case "JUMP_HEIGHT": m = Material.RABBIT_FOOT; break;
            case "XP_GAIN": m = Material.EXPERIENCE_BOTTLE; break;
            case "ELITE_SPAWN_INCREASE": m = Material.WITHER_ROSE; break;
            case "POWERUP_MULT": m = Material.ENCHANTING_TABLE; break;
            default: m = Material.PAPER; break;
        }
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        if (im != null) { NamedTextColor c = wr.weight >= 5 ? NamedTextColor.GOLD : (wr.weight >= 2 ? NamedTextColor.AQUA : NamedTextColor.WHITE); im.displayName(Component.text(wr.name).color(c)); it.setItemMeta(im);} return it;
    }
    private ItemStack fadedIconFor(WeightedReward wr){ ItemStack it = iconFor(wr).clone(); try{ ItemMeta im = it.getItemMeta(); if(im!=null){ im.displayName(Component.text("· "+wr.name+" ·").color(NamedTextColor.GRAY)); it.setItemMeta(im);} }catch(Throwable ignored){} return it; }

    // ---- GUI utils ----
    private void fill(Inventory inv, Material mat) { ItemStack pane = new ItemStack(mat); ItemMeta im = pane.getItemMeta(); if (im != null) { im.displayName(Component.text(" ")); pane.setItemMeta(im);} for (int i=0;i<inv.getSize();i++) inv.setItem(i, pane); }
    private void clearReelColumns(Inventory inv, int... centers){ for (int c: centers){ safeSet(inv,c-9,new ItemStack(Material.GRAY_STAINED_GLASS_PANE)); safeSet(inv,c,new ItemStack(Material.GRAY_STAINED_GLASS_PANE)); safeSet(inv,c+9,new ItemStack(Material.GRAY_STAINED_GLASS_PANE)); } }
    private void safeSet(Inventory inv, int slot, ItemStack item){ if(slot<0||slot>=inv.getSize()) return; try{ inv.setItem(slot,item);}catch(Throwable ignored){} }
    private Sound stopSoundFor(String type){ switch(type.toUpperCase()){ case "DAMAGE_MULT": return Sound.ENTITY_BLAZE_AMBIENT; case "DAMAGE_ADD": return Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR; case "FLAT_DAMAGE": return Sound.BLOCK_ANVIL_PLACE; case "RADIUS_MULT": return Sound.ENTITY_DOLPHIN_AMBIENT; case "PALADIN_HEAL": return Sound.ITEM_TOTEM_USE; case "SPEED": return Sound.ENTITY_HORSE_GALLOP; case "ATTACK_SPEED": return Sound.ENTITY_PLAYER_ATTACK_SWEEP; case "RESIST": return Sound.ITEM_SHIELD_BLOCK; case "LUCK": return Sound.ENTITY_EXPERIENCE_ORB_PICKUP; case "HEALTH_HEARTS": return Sound.BLOCK_BEACON_POWER_SELECT; default: return Sound.UI_TOAST_CHALLENGE_COMPLETE; } }

    // ---- Fallback für Klick auf ItemDisplay/TextDisplay ----
    @EventHandler(ignoreCancelled = true)
    public void onDisplayInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent e) {
        Entity ent = e.getRightClicked();
        if (!(ent instanceof ItemDisplay) && !(ent instanceof TextDisplay)) return;
        UUID chestId = DISPLAY_TO_CHEST.get(ent.getUniqueId());
        if (chestId == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        ActiveChest ac = CHESTS.remove(chestId);
        if (ac != null) despawn(chestId, ac);
        openForPlayer(plugin, p);
    }

    // ---- Despawn helpers ----
    public static void despawnAll() {
        for (Map.Entry<UUID, ActiveChest> en : CHESTS.entrySet()) {
            try { despawn(en.getKey(), en.getValue()); } catch (Throwable ignored) {}
        }
        CHESTS.clear();
        DISPLAY_TO_CHEST.clear();
    }
    public static void despawnAllInWorld(org.bukkit.World world) {
        if (world == null) { despawnAll(); return; }
        java.util.Iterator<Map.Entry<UUID, ActiveChest>> it = CHESTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveChest> en = it.next();
            ActiveChest ac = en.getValue();
            if (ac == null) { it.remove(); continue; }
            if (world.equals(ac.world)) {
                try { despawn(en.getKey(), ac); } catch (Throwable ignored) {}
                it.remove();
            }
        }
    }

    // Stop animation task and clear caches (call on plugin disable)
    public static void stopAll() {
        try {
            if (animateTask != null) { try { animateTask.cancel(); } catch (Throwable ignored) {} animateTask = null; }
        } catch (Throwable ignored) {}
        try { despawnAll(); } catch (Throwable ignored) {}
        try { LOOT_PAUSED.clear(); } catch (Throwable ignored) {}
    }

    // ---- Player death: despawn remaining lootchests in that world ----
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        try {
            boolean enabled = plugin.getConfigUtil().getBoolean("lootchest.despawn-on-player-death", true);
            if (!enabled) return;
        } catch (Throwable ignored) {}
        org.bukkit.entity.Player dead = e.getEntity();
        try {
            if (plugin.getConfigUtil().getBoolean("debug.lootchest", false)) {
                plugin.getLogger().info("Lootchest: player died -> despawn in world " + dead.getWorld().getName());
            }
        } catch (Throwable ignored) {}
        despawnAllInWorld(dead.getWorld());
    }

    private static void pauseForLoot(MinecraftSurvivors pl, Player p) {
        try {
            if (pl == null || p == null) return;
            java.util.UUID uid = p.getUniqueId();
            if (pl.getGameManager() == null) return;
            if (pl.getGameManager().isPlayerPaused(uid)) return; // bereits pausiert
            pl.getGameManager().pauseForPlayer(uid);
            LOOT_PAUSED.add(uid);
        } catch (Throwable ignored) {}
    }

    // Mapping & Anwendung der Reward-StatModifier (zentral, damit ReelsTask nur Auswahl triggert)
    private void applyLootReward(org.bukkit.entity.Player p, java.util.Map<String,Object> reward) {
        if (p == null || reward == null) { if (p!=null) p.sendMessage("§eDie Kiste war leer…"); return; }
        String type = String.valueOf(reward.getOrDefault("type","UNKNOWN"));
        double val = 0.0; try { Object v = reward.get("value"); if (v instanceof Number) val=((Number)v).doubleValue(); else if (v!=null) val = Double.parseDouble(String.valueOf(v)); } catch (Throwable ignored) {}
        String nm = String.valueOf(reward.getOrDefault("name", type));
        var sp = plugin.getPlayerManager().get(p.getUniqueId());
        if (sp == null) { p.sendMessage("§cKein Spielerprofil"); return; }
        String up = type.toUpperCase(java.util.Locale.ROOT);
        if ("FLAT_DAMAGE".equals(up)) up = "DAMAGE_ADD";
        if ("PALADIN_HEAL".equals(up)) up = "HP_REGEN";
        org.bysenom.minecraftSurvivors.model.StatType st = switch (up) {
            case "DAMAGE_MULT" -> org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_MULT;
            case "DAMAGE_ADD" -> org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ADD;
            case "RADIUS_MULT" -> org.bysenom.minecraftSurvivors.model.StatType.RADIUS_MULT;
            case "SPEED" -> org.bysenom.minecraftSurvivors.model.StatType.SPEED;
            case "ATTACK_SPEED" -> org.bysenom.minecraftSurvivors.model.StatType.ATTACK_SPEED;
            case "RESIST" -> org.bysenom.minecraftSurvivors.model.StatType.RESIST;
            case "LUCK" -> org.bysenom.minecraftSurvivors.model.StatType.LUCK;
            case "HEALTH_HEARTS" -> org.bysenom.minecraftSurvivors.model.StatType.HEALTH_HEARTS;
            case "MAX_HEALTH" -> org.bysenom.minecraftSurvivors.model.StatType.MAX_HEALTH;
            case "HP_REGEN" -> org.bysenom.minecraftSurvivors.model.StatType.HP_REGEN;
            case "SHIELD" -> org.bysenom.minecraftSurvivors.model.StatType.SHIELD;
            case "ARMOR" -> org.bysenom.minecraftSurvivors.model.StatType.ARMOR;
            case "EVASION" -> org.bysenom.minecraftSurvivors.model.StatType.EVASION;
            case "LIFESTEAL" -> org.bysenom.minecraftSurvivors.model.StatType.LIFESTEAL;
            case "THORNS" -> org.bysenom.minecraftSurvivors.model.StatType.THORNS;
            case "CRIT_CHANCE" -> org.bysenom.minecraftSurvivors.model.StatType.CRIT_CHANCE;
            case "CRIT_DAMAGE" -> org.bysenom.minecraftSurvivors.model.StatType.CRIT_DAMAGE;
            case "PROJECTILE_COUNT" -> org.bysenom.minecraftSurvivors.model.StatType.PROJECTILE_COUNT;
            case "PROJECTILE_BOUNCE" -> org.bysenom.minecraftSurvivors.model.StatType.PROJECTILE_BOUNCE;
            case "SIZE" -> org.bysenom.minecraftSurvivors.model.StatType.SIZE;
            case "DURATION" -> org.bysenom.minecraftSurvivors.model.StatType.DURATION;
            case "DAMAGE_ELITE_BOSS" -> org.bysenom.minecraftSurvivors.model.StatType.DAMAGE_ELITE_BOSS;
            case "KNOCKBACK" -> org.bysenom.minecraftSurvivors.model.StatType.KNOCKBACK;
            case "JUMP_HEIGHT" -> org.bysenom.minecraftSurvivors.model.StatType.JUMP_HEIGHT;
            case "XP_GAIN" -> org.bysenom.minecraftSurvivors.model.StatType.XP_GAIN;
            case "ELITE_SPAWN_INCREASE" -> org.bysenom.minecraftSurvivors.model.StatType.ELITE_SPAWN_INCREASE;
            case "POWERUP_MULT" -> org.bysenom.minecraftSurvivors.model.StatType.POWERUP_MULT;
            default -> null;
        };
        if (st == null) { p.sendMessage("§eUnbekannte Belohnung: "+type); return; }
        if (st == org.bysenom.minecraftSurvivors.model.StatType.PROJECTILE_COUNT || st == org.bysenom.minecraftSurvivors.model.StatType.PROJECTILE_BOUNCE || st == org.bysenom.minecraftSurvivors.model.StatType.HEALTH_HEARTS) {
            val = Math.max(1.0, Math.round(val));
        }
        org.bysenom.minecraftSurvivors.model.StatModifier mod = new org.bysenom.minecraftSurvivors.model.StatModifier(st, val, "lootchest:"+nm);
        sp.addStatModifier(mod);
        try { plugin.getPlayerDataManager().saveAsync(sp); } catch (Throwable ignored) {}
        try { p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f); } catch (Throwable ignored) {}
        p.sendMessage("§aGewonnen: "+nm+formatSuffix(st,val));
    }
    private String formatSuffix(org.bysenom.minecraftSurvivors.model.StatType st,double val){ return switch (st){
        case DAMAGE_MULT, RADIUS_MULT, SPEED, ATTACK_SPEED, RESIST, LUCK, CRIT_CHANCE, CRIT_DAMAGE, SIZE, DURATION, DAMAGE_ELITE_BOSS, KNOCKBACK, XP_GAIN, ELITE_SPAWN_INCREASE, POWERUP_MULT, EVASION, LIFESTEAL, THORNS, JUMP_HEIGHT -> " ("+percent(val)+")";
        case DAMAGE_ADD -> " (+"+round2(val)+" dmg)";
        case HEALTH_HEARTS -> " (+"+(int)val+" Herz)";
        case MAX_HEALTH -> " (+"+round2(val)+" MaxHP)";
        case HP_REGEN -> " (+"+round2(val)+" HP/s)";
        case SHIELD -> " (+"+round2(val)+" Schild)";
        case ARMOR -> " ("+percent(val)+" Armor)";
        case PROJECTILE_COUNT -> " (+"+(int)val+" Projektil)";
        case PROJECTILE_BOUNCE -> " (+"+(int)val+" Bounce)";
        default -> " (+"+round2(val)+")"; };
    }
    private String percent(double v){ return (int)Math.round(v*100.0)+"%"; }
    private double round2(double v){ return Math.round(v*100.0)/100.0; }
}
