package org.bysenom.minecraftSurvivors.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;
import org.bysenom.minecraftSurvivors.manager.GameManager;

public class GuiManager {

    private final MinecraftSurvivors plugin;
    private final GameManager gameManager;
    private final NamespacedKey key;

    // Sichtbarer Titel des Hauptmenüs (gestylt)
    public static final Component MAIN_TITLE = Component.text("MinecraftSurvivors").color(NamedTextColor.AQUA).append(Component.text(" • Menu").color(NamedTextColor.WHITE));

    public GuiManager(MinecraftSurvivors plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.key = new NamespacedKey(plugin, "ms_gui");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Hauptmenü mit neuem Eintrag "Klasse wählen".
     */
    public void openMainMenu(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 45, MAIN_TITLE);
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        int coins = sp != null ? sp.getCoins() : 0;
        int lvl = sp != null ? sp.getClassLevel() : 1;
        inv.setItem(10, createGuiItem(Material.EMERALD, Component.text("Status").color(NamedTextColor.GOLD),
                java.util.List.of(Component.text("✦ Coins: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(coins)).color(NamedTextColor.WHITE)),
                        Component.text("⚑ Level: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(lvl)).color(NamedTextColor.WHITE))), "status", false));
        // Start führt jetzt durch den Setup-Wizard (Klassenwahl -> Skillwahl -> Countdown)
        inv.setItem(12, createGuiItem(Material.GREEN_STAINED_GLASS, Component.text("Spiel starten").color(NamedTextColor.GREEN),
                java.util.List.of(Component.text("Wähle zuerst eine Klasse, dann einen Zusatz-Skill").color(NamedTextColor.GRAY)), "start_wizard", false));
        inv.setItem(16, createGuiItem(Material.NETHER_STAR, Component.text("Powerups").color(NamedTextColor.LIGHT_PURPLE),
                java.util.List.of(Component.text("Später verfügbare Powerups & Items").color(NamedTextColor.GRAY)), "powerup", false));
        inv.setItem(20, createGuiItem(Material.PLAYER_HEAD, Component.text("Party").color(NamedTextColor.GOLD),
                java.util.List.of(Component.text("Erstelle/Verwalte deine Party").color(NamedTextColor.GRAY)), "party", false));
        inv.setItem(22, createGuiItem(Material.CLOCK, Component.text("Stats").color(NamedTextColor.YELLOW),
                java.util.List.of(Component.text("DPS/HPS Anzeige-Modus umschalten").color(NamedTextColor.GRAY)), "stats", false));
        if (p.hasPermission("minecraftsurvivors.admin")) {
            inv.setItem(24, createGuiItem(Material.COMPARATOR, Component.text("Admin Panel").color(NamedTextColor.RED),
                    java.util.List.of(Component.text("Debug & Test-Tools").color(NamedTextColor.GRAY)), "admin", false));
        }
        inv.setItem(29, createGuiItem(Material.NETHERITE_INGOT, Component.text("Meta").color(NamedTextColor.LIGHT_PURPLE),
                java.util.List.of(Component.text("Dauerhafte Upgrades kaufen").color(NamedTextColor.GRAY)), "meta", false));
        inv.setItem(36, createGuiItem(Material.PAPER, Component.text("Info").color(NamedTextColor.YELLOW),
                java.util.List.of(Component.text("V: Vampire Survivors like Mini-Game"), Component.text("bySenom").color(NamedTextColor.GRAY)), "info"));
        // Footer (Status-Zeile) unten rechts
        // Footer erweitern: Shop-Reset Timer + Daily-Offers Counter
        String mode = "-";
        try { mode = plugin.getStatsDisplayManager().getMode().name().toLowerCase(); } catch (Throwable ignored) {}
        java.util.List<Component> footerLore = new java.util.ArrayList<>();
        footerLore.add(Component.text("Stats: ").color(NamedTextColor.GRAY).append(Component.text(mode).color(NamedTextColor.AQUA)));
        String eta = plugin.getShopManager().formatRemainingHHMMSS();
        footerLore.add(Component.text("Shop Reset: ").color(NamedTextColor.GRAY).append(Component.text(eta).color(NamedTextColor.YELLOW)));
        int dailyN = plugin.getConfigUtil().getInt("shop.daily.max-weapons", 6);
        footerLore.add(Component.text("Daily Offers: ").color(NamedTextColor.GRAY).append(Component.text(dailyN).color(NamedTextColor.GOLD)));
        org.bysenom.minecraftSurvivors.manager.PartyManager pm = plugin.getPartyManager();
        org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm != null ? pm.getPartyOf(p.getUniqueId()) : null;
        if (party != null) {
            int online = pm.onlineMembers(party).size();
            int total = party.getMembers().size();
            footerLore.add(Component.text("Party: ").color(NamedTextColor.GRAY)
                    .append(Component.text(online + "/" + total).color(NamedTextColor.GREEN)));
        } else {
            footerLore.add(Component.text("Party: keine").color(NamedTextColor.DARK_GRAY));
        }
        // Meta essence quick info
        int essence = plugin.getMetaManager().get(p.getUniqueId()).getEssence();
        footerLore.add(Component.text("Essence: ").color(NamedTextColor.LIGHT_PURPLE).append(Component.text(String.valueOf(essence)).color(NamedTextColor.WHITE)));
        // Ready-Status
        int total = org.bukkit.Bukkit.getOnlinePlayers().size();
        int ready = 0;
        for (org.bukkit.entity.Player op : org.bukkit.Bukkit.getOnlinePlayers()) {
            try { org.bysenom.minecraftSurvivors.model.SurvivorPlayer osp = plugin.getPlayerManager().get(op.getUniqueId()); if (osp != null && osp.isReady()) ready++; } catch (Throwable ignored) {}
        }
        footerLore.add(Component.text("Ready: ").color(NamedTextColor.GRAY).append(Component.text(ready+"/"+total).color(NamedTextColor.GREEN)));
        inv.setItem(44, createGuiItem(Material.MAP, Component.text("Status").color(NamedTextColor.WHITE), footerLore, "noop", false));

        p.openInventory(inv);
    }

    public void openMetaMenu(Player p) {
        if (p == null) return;
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, Component.text("Meta-Progression").color(NamedTextColor.LIGHT_PURPLE));
        fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        java.util.List<java.util.Map<?, ?>> list = cfg.getMapList("meta.shop");
        int slot = 10;
        for (java.util.Map<?, ?> m : list) {
            Object keyObj = m.containsKey("key") ? m.get("key") : "";
            String keyId = String.valueOf(keyObj);
            Object nameObj = m.containsKey("name") ? m.get("name") : keyId;
            String name = String.valueOf(nameObj);
            Object typeObj = m.containsKey("type") ? m.get("type") : "";
            String type = String.valueOf(typeObj);
            Object priceObj = m.containsKey("price") ? m.get("price") : 10;
            int price = Integer.parseInt(String.valueOf(priceObj));
            Object stepObj = m.containsKey("step") ? m.get("step") : 0.01;
            double step = Double.parseDouble(String.valueOf(stepObj));
            Object capObj = m.containsKey("cap") ? m.get("cap") : 0.5;
            double cap = Double.parseDouble(String.valueOf(capObj));
            Material mat = Material.AMETHYST_SHARD;
            if (type.equalsIgnoreCase("DAMAGE_MULT")) mat = Material.DIAMOND_SWORD;
            else if (type.equalsIgnoreCase("MOVE_SPEED")) mat = Material.SUGAR;
            else if (type.equalsIgnoreCase("ATTACK_SPEED")) mat = Material.FEATHER;
            else if (type.equalsIgnoreCase("RESIST")) mat = Material.SHIELD;
            else if (type.equalsIgnoreCase("LUCK")) mat = Material.RABBIT_FOOT;
            else if (type.equalsIgnoreCase("HEALTH_HEARTS")) mat = Material.GOLDEN_APPLE;
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text(name).color(NamedTextColor.WHITE));
            lore.add(Component.text("Preis: "+price+" Essence").color(NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("Schritt: +"+step).color(NamedTextColor.GRAY));
            lore.add(Component.text("Cap: "+cap).color(NamedTextColor.DARK_GRAY));
            inv.setItem(slot, createGuiItem(mat, Component.text(keyId).color(NamedTextColor.AQUA), lore, "meta_buy:"+keyId, false));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    /**
     * Öffnet die Klassenwahl; aktuell ein Beispiel: Shamanen.
     */
    public void openClassSelection(Player p) {
        if (p == null) return;
        Component title = Component.text("Klassenwahl").color(NamedTextColor.AQUA);
        Inventory inv = Bukkit.createInventory(null, 27, title);
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        org.bysenom.minecraftSurvivors.model.PlayerClass current = sp != null ? sp.getSelectedClass() : null;
        inv.setItem(10, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN), Component.text("Shamanen ⚡").color(NamedTextColor.LIGHT_PURPLE),
                java.util.List.of(Component.text("Main: Blitz"), Component.text("Solider Allrounder").color(NamedTextColor.GRAY)), "select_shaman_wizard", current == org.bysenom.minecraftSurvivors.model.PlayerClass.SHAMAN));
        inv.setItem(12, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER), Component.text("Pyromant 🔥").color(NamedTextColor.GOLD),
                java.util.List.of(Component.text("Main: Feuer & DoT"), Component.text("Nahbereich-AoE").color(NamedTextColor.GRAY)), "select_pyromancer_wizard", current == org.bysenom.minecraftSurvivors.model.PlayerClass.PYROMANCER));
        inv.setItem(14, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER), Component.text("Waldläufer 🏹").color(NamedTextColor.GREEN),
                java.util.List.of(Component.text("Main: Fernschuss"), Component.text("Single-Target").color(NamedTextColor.GRAY)), "select_ranger_wizard", current == org.bysenom.minecraftSurvivors.model.PlayerClass.RANGER));
        inv.setItem(16, createGuiItem(materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN), Component.text("Paladin ✨").color(NamedTextColor.AQUA),
                java.util.List.of(Component.text("Main: Heilige Nova"), Component.text("AoE + Heal").color(NamedTextColor.GRAY)), "select_paladin_wizard", current == org.bysenom.minecraftSurvivors.model.PlayerClass.PALADIN));
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Abbrechen").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    public void openSkillSelection(Player p) {
        if (p == null) return;
        Component title = Component.text("Zusatz-Skill").color(NamedTextColor.GOLD);
        Inventory inv = Bukkit.createInventory(null, 27, title);
        fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE);
        inv.setItem(10, createGuiItem(Material.FEATHER, Component.text("Dash").color(NamedTextColor.WHITE), java.util.List.of(Component.text("Kurzer Sprint").color(NamedTextColor.GRAY)), "skill_pick:dash", false));
        inv.setItem(12, createGuiItem(Material.STICK, Component.text("Shockwave").color(NamedTextColor.AQUA), java.util.List.of(Component.text("Stoßwelle um dich").color(NamedTextColor.GRAY)), "skill_pick:shockwave", false));
        inv.setItem(14, createGuiItem(Material.TOTEM_OF_UNDYING, Component.text("Heal Totem").color(NamedTextColor.GREEN), java.util.List.of(Component.text("Periodischer Heal").color(NamedTextColor.GRAY)), "skill_pick:heal_totem", false));
        inv.setItem(16, createGuiItem(Material.SNOWBALL, Component.text("Frost Nova").color(NamedTextColor.AQUA), java.util.List.of(Component.text("AoE-Slow + Schaden").color(NamedTextColor.GRAY)), "skill_pick:frost_nova", false));
        inv.setItem(22, createGuiItem(Material.BARRIER, Component.text("Ohne Zusatz-Skill").color(NamedTextColor.RED), java.util.List.of(Component.text("Direkt starten")), "skill_pick:none", false));
        p.openInventory(inv);
    }

    private void fillBorder(Inventory inv, Material borderMat) {
        ItemStack border = new ItemStack(borderMat);
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == (inv.getSize()/9 -1) || col == 0 || col == 8) {
                inv.setItem(i, border);
            }
        }
    }

    private ItemStack createGuiItem(Material mat, Component name, List<Component> lore, String action) {
        return createGuiItem(mat, name, lore, action, false);
    }

    private ItemStack createGuiItem(Material mat, Component name, List<Component> lore, String action, boolean glow) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
            if (glow) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Behandelt die Auswahl im Level-Up-Menü (ItemStack-Variante).
     * Schließt das Inventar, vergibt ein kleines Beispiel-Reward und sendet eine Nachricht.
     */
    public void handleLevelChoice(org.bukkit.entity.Player player, org.bukkit.inventory.ItemStack display, int level) {
        if (player == null || display == null) return;
        player.closeInventory();
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(player.getUniqueId());
        org.bukkit.Material mat = display.getType();
        org.bukkit.NamespacedKey rarKey = new org.bukkit.NamespacedKey(plugin, "ms_rarmult");
        double rarMult = 1.0;
        try {
            org.bukkit.inventory.meta.ItemMeta md = display.getItemMeta();
            if (md != null) {
                String s = md.getPersistentDataContainer().get(rarKey, org.bukkit.persistence.PersistentDataType.STRING);
                if (s != null) rarMult = Double.parseDouble(s);
            }
        } catch (Throwable ignored) {}

        double cfgBonusDamage = plugin.getConfigUtil().getDouble("levelup.values.bonus-damage", 1.5) * rarMult;
        int cfgBonusStrikes = (int) Math.round(plugin.getConfigUtil().getInt("levelup.values.bonus-strikes", 1) * rarMult);
        double cfgFlatDamage = plugin.getConfigUtil().getDouble("levelup.values.flat-damage", 2.0) * rarMult;
        int cfgExtraHearts = plugin.getConfigUtil().getInt("levelup.values.extra-hearts", 2); // hearts not scaled by rarity by default

        double sizeStep = plugin.getConfigUtil().getDouble("levelup.weapon.size-step", 0.15);
        double atkMultStep = plugin.getConfigUtil().getDouble("levelup.weapon.attackpower-step", 0.20);
        int igniteStep = (int) Math.round(plugin.getConfigUtil().getInt("levelup.pyromancer.ignite-step", 20) * rarMult);
        double kbStep = plugin.getConfigUtil().getDouble("levelup.ranger.knockback-step", 0.10) * rarMult;
        double healStep = plugin.getConfigUtil().getDouble("levelup.paladin.heal-step", 0.5) * rarMult;

        double moveStep = plugin.getConfigUtil().getDouble("levelup.values.move-speed", 0.05) * rarMult;
        double asStep = plugin.getConfigUtil().getDouble("levelup.values.attack-speed", 0.07) * rarMult;
        double resistStep = plugin.getConfigUtil().getDouble("levelup.values.resist", 0.05) * rarMult;
        double luckStep = plugin.getConfigUtil().getDouble("levelup.values.luck", 0.05) * rarMult;

        switch (mat) {
            case DIAMOND_SWORD:
                sp.addBonusDamage(cfgBonusDamage);
                player.sendMessage("§a+" + cfgBonusDamage + " Bonus-Schaden");
                break;
            case TRIDENT:
                sp.addBonusStrikes(cfgBonusStrikes);
                player.sendMessage("§a+" + cfgBonusStrikes + " Treffer (Strikes)");
                break;
            case IRON_INGOT:
                sp.addFlatDamage(cfgFlatDamage);
                player.sendMessage("§a+" + cfgFlatDamage + " flacher Schaden");
                break;
            case APPLE:
                sp.addExtraHearts(cfgExtraHearts);
                try {
                    org.bukkit.attribute.AttributeInstance max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (max != null) {
                        double addHearts = cfgExtraHearts / 2.0;
                        double newBase = Math.max(1.0, max.getBaseValue() + (addHearts * 2.0));
                        max.setBaseValue(newBase);
                        player.setHealth(Math.min(newBase, player.getHealth() + (addHearts * 2.0)));
                    }
                } catch (Throwable ignored) {}
                player.sendMessage("§a+" + (cfgExtraHearts / 2.0) + " Herzen");
                break;
            case BLAZE_POWDER:
                sp.addRadiusMult(sizeStep);
                player.sendMessage("§6Radius +" + (int)(sizeStep * 100) + "%");
                break;
            case GOLDEN_SWORD:
                sp.addDamageMult(atkMultStep);
                player.sendMessage("§6Attackpower +" + (int)(atkMultStep * 100) + "%");
                break;
            case MAGMA_CREAM:
                sp.addIgniteBonusTicks(igniteStep);
                player.sendMessage("§6Burn Dauer +" + igniteStep + "t");
                break;
            case BOW:
                // Ambig: BOW kann auch Waffe: Fernschuss sein – Material-Konflikt vermeiden wir, indem wir den Display-Namen prüfen
                String dn = null; try { if (display.getItemMeta()!=null && display.getItemMeta().displayName()!=null) dn = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(display.getItemMeta().displayName()); } catch (Throwable ignored) {}
                if (dn != null && dn.toLowerCase(java.util.Locale.ROOT).contains("waffe: fern")) {
                    if (sp.addWeapon("w_ranged")) player.sendMessage("§bNeue Waffe: Fernschuss"); else player.sendMessage("§7Waffen-Slots voll – Level erhöht");
                } else {
                    sp.addKnockbackBonus(kbStep);
                    player.sendMessage("§6Knockback +" + (int)(kbStep * 100) + "%");
                }
                break;
            case GOLDEN_APPLE:
                sp.addHealBonus(healStep);
                player.sendMessage("§6Heilung +" + healStep);
                break;
            case SUGAR:
                sp.addMoveSpeedMult(moveStep);
                try {
                    org.bukkit.attribute.AttributeInstance mv = player.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                    if (mv != null) mv.setBaseValue(mv.getBaseValue() * (1.0 + moveStep));
                } catch (Throwable ignored) {}
                player.sendMessage("§bGeschwindigkeit +" + (int)(moveStep*100) + "%");
                break;
            case FEATHER:
                sp.addAttackSpeedMult(asStep);
                try {
                    org.bukkit.potion.PotionEffectType haste = org.bukkit.potion.PotionEffectType.HASTE;
                    if (haste == null) haste = org.bukkit.potion.PotionEffectType.SPEED;
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(haste, 20*60, 0, false, false, false));
                } catch (Throwable ignored) {}
                player.sendMessage("§bAngriffstempo +" + (int)(asStep*100) + "%");
                break;
            case SHIELD:
                sp.addDamageResist(resistStep);
                player.sendMessage("§bResistenz +" + (int)(resistStep*100) + "%");
                break;
            case RABBIT_FOOT:
                sp.addLuck(luckStep);
                player.sendMessage("§bGlück +" + (int)(luckStep*100) + "%");
                break;
            case LIGHTNING_ROD:
                if (sp.addWeapon("w_lightning")) player.sendMessage("§bNeue Waffe: Blitz"); else player.sendMessage("§7Waffen-Slots voll – Level erhöht");
                break;
            case FIRE_CHARGE:
                if (sp.addWeapon("w_fire")) player.sendMessage("§bNeue Waffe: Feuer"); else player.sendMessage("§7Waffen-Slots voll – Level erhöht");
                break;
            case HEART_OF_THE_SEA:
                if (sp.addWeapon("w_holy")) player.sendMessage("§bNeue Waffe: Heilige Nova"); else player.sendMessage("§7Waffen-Slots voll – Level erhöht");
                break;
            default:
                sp.addCoins(5);
                player.sendMessage("§a+5 Coins");
                break;
        }
    }

    /**
     * Behandelt die Auswahl im Level-Up-Menü (String-Variante).
     */
    public void handleLevelChoice(Player player, String displayName, int level) {
        if (player == null || displayName == null) return;
        player.closeInventory();
        try {
            plugin.getPlayerManager().get(player.getUniqueId()).addCoins(5);
            player.sendMessage(Component.text("§aAusgewählt: " + displayName + " (Level " + level + ") — §e+5 Münzen"));
        } catch (Exception ex) {
            player.sendMessage(Component.text("§aAusgewählt: " + displayName + " (Level " + level + ")"));
        }
    }

    public void openInfoMenu(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("MinecraftSurvivors - Info").color(NamedTextColor.YELLOW));
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, createGuiItem(Material.PAPER, Component.text("Über das Spiel").color(NamedTextColor.GOLD),
                List.of(Component.text("V: Vampire Survivors like Mini-Game"), Component.text("bySenom").color(NamedTextColor.GRAY)), "info_close"));
        p.openInventory(inv);
    }

    /**
     * Öffnet das Level-Up-Menü für einen Spieler mit einer Auswahl entsprechend `level`.
     */
    public void openLevelUpMenu(Player p, int level) {
        if (p == null) return;
        // Pause only this player while they choose a level reward (local pause)
        try {
            if (gameManager != null) gameManager.pauseForPlayer(p.getUniqueId());
        } catch (Throwable ignored) {}
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        LevelUpMenu menu = new LevelUpMenu(p, sp, level);
        p.openInventory(menu.getInventory());
    }

    public void openPartyMenu(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Party").color(NamedTextColor.GOLD));
        fillBorder(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        org.bysenom.minecraftSurvivors.manager.PartyManager pm = plugin.getPartyManager();
        java.util.UUID pendingLeader = pm != null ? pm.getPendingInviteLeader(p.getUniqueId()) : null;
        inv.setItem(10, createGuiItem(Material.SLIME_BALL, Component.text("Erstellen").color(NamedTextColor.GREEN), java.util.List.of(Component.text("Neue Party erstellen")), "party_create"));
        // Einladen per Liste
        inv.setItem(12, createGuiItem(Material.PLAYER_HEAD, Component.text("Einladen (Liste)").color(NamedTextColor.YELLOW), java.util.List.of(Component.text("Klicke, um Online-Spieler zu sehen").color(NamedTextColor.GRAY)), "party_invite_list"));
        if (pendingLeader != null) {
            org.bukkit.entity.Player lp = org.bukkit.Bukkit.getPlayer(pendingLeader);
            String name = lp != null ? lp.getName() : "Leader";
            inv.setItem(14, createGuiItem(Material.LIME_DYE, Component.text("Einladung annehmen").color(NamedTextColor.AQUA), java.util.List.of(Component.text("von "+name)), "party_join_invite"));
        } else {
            inv.setItem(14, createGuiItem(Material.GRAY_DYE, Component.text("Keine Einladung").color(NamedTextColor.GRAY), java.util.List.of(Component.text("Du hast derzeit keine offene Einladung")), "noop"));
        }
        // Mitglieder-Info
        java.util.List<Component> lore = new java.util.ArrayList<>();
        org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm != null ? pm.getPartyOf(p.getUniqueId()) : null;
        if (party != null) {
            lore.add(Component.text("Mitglieder:").color(NamedTextColor.GOLD));
            for (java.util.UUID u : party.getMembers()) {
                org.bukkit.entity.Player pl = org.bukkit.Bukkit.getPlayer(u);
                String name = pl != null ? pl.getName() : u.toString();
                lore.add(Component.text("- "+name).color(NamedTextColor.WHITE));
            }
        } else {
            lore.add(Component.text("Keine Party").color(NamedTextColor.GRAY));
        }
        inv.setItem(11, createGuiItem(Material.BOOK, Component.text("Mitglieder").color(NamedTextColor.AQUA), lore, "noop"));

        inv.setItem(16, createGuiItem(Material.BARRIER, Component.text("Verlassen/Auflösen").color(NamedTextColor.RED), java.util.List.of(Component.text("Party verlassen oder als Leader auflösen")), "party_leave"));
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    public void openPartyInviteList(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Spieler einladen").color(NamedTextColor.YELLOW));
        fillBorder(inv, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        java.util.Set<java.util.UUID> skip = new java.util.HashSet<>();
        org.bysenom.minecraftSurvivors.manager.PartyManager pm = plugin.getPartyManager();
        org.bysenom.minecraftSurvivors.manager.PartyManager.Party party = pm != null ? pm.getPartyOf(p.getUniqueId()) : null;
        if (party != null) skip.addAll(party.getMembers());
        int slot = 10;
        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(p.getUniqueId())) continue;
            if (skip.contains(online.getUniqueId())) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            try {
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                if (sm != null) {
                    sm.setOwningPlayer(online);
                    sm.displayName(Component.text(online.getName(), NamedTextColor.AQUA));
                    sm.lore(java.util.List.of(Component.text("Klicken zum Einladen").color(NamedTextColor.GRAY)));
                    sm.getPersistentDataContainer().set(key, PersistentDataType.STRING, "party_invite:" + online.getUniqueId());
                    head.setItemMeta(sm);
                }
            } catch (Throwable ignored) {}
            inv.setItem(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2; // skip border
            if (slot >= inv.getSize() - 9) break;
        }
        inv.setItem(49, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Party-Menü")), "party_back"));
        p.openInventory(inv);
    }

    public void openStatsMenu(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Stats Anzeige").color(NamedTextColor.YELLOW));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode mode = plugin.getStatsDisplayManager().getMode();
        inv.setItem(10, createGuiItem(Material.PAPER, Component.text("ActionBar").color(NamedTextColor.WHITE), java.util.List.of(Component.text("XP/Lvl + DPS/HPS in der ActionBar")), "stats_mode_actionbar", mode == org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.ACTIONBAR));
        inv.setItem(12, createGuiItem(Material.DRAGON_BREATH, Component.text("BossBar").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Zwei dezente BossBars (DPS/HPS)")), "stats_mode_bossbar", mode == org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.BOSSBAR));
        inv.setItem(14, createGuiItem(Material.OAK_SIGN, Component.text("Scoreboard").color(NamedTextColor.AQUA), java.util.List.of(Component.text("HUD nur im Scoreboard-Modus")), "stats_mode_scoreboard", mode == org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.SCOREBOARD));
        inv.setItem(16, createGuiItem(Material.BARRIER, Component.text("Aus").color(NamedTextColor.RED), java.util.List.of(Component.text("Anzeige deaktivieren")), "stats_mode_off", mode == org.bysenom.minecraftSurvivors.manager.StatsDisplayManager.Mode.OFF));
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    public void openConfigMenu(Player p) {
        if (p == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Konfiguration").color(NamedTextColor.AQUA));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(11, createGuiItem(Material.WRITABLE_BOOK, Component.text("Reload Config").color(NamedTextColor.GOLD), java.util.List.of(Component.text("/msconfig reload ausführen")), "config_reload"));
        inv.setItem(13, createGuiItem(Material.GLOWSTONE_DUST, Component.text("Preset: flashy").color(NamedTextColor.YELLOW), java.util.List.of(Component.text("Spawn-Visual-Preset anwenden")), "config_preset_flashy"));
        inv.setItem(15, createGuiItem(Material.ENDER_EYE, Component.text("Preset: epic").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Spawn-Visual-Preset anwenden")), "config_preset_epic"));
        inv.setItem(22, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück zum Hauptmenü")), "back"));
        p.openInventory(inv);
    }

    public void applyPreset(String name) {
        if (name == null) return;
        switch (name.toLowerCase()) {
            case "flashy":
                plugin.getConfigUtil().setValue("spawn.particle", "FLAME");
                plugin.getConfigUtil().setValue("spawn.particle-duration", 14);
                plugin.getConfigUtil().setValue("spawn.particle-points", 20);
                plugin.getConfigUtil().setValue("spawn.particle-count", 3);
                plugin.getConfigUtil().setValue("spawn.particle-spread", 0.12);
                break;
            case "epic":
                plugin.getConfigUtil().setValue("spawn.particle", "REDSTONE");
                plugin.getConfigUtil().setValue("spawn.particle-duration", 20);
                plugin.getConfigUtil().setValue("spawn.particle-points", 28);
                plugin.getConfigUtil().setValue("spawn.particle-count", 4);
                plugin.getConfigUtil().setValue("spawn.particle-spread", 0.18);
                break;
            default:
                // subtle as base
                plugin.getConfigUtil().setValue("spawn.particle", "END_ROD");
                plugin.getConfigUtil().setValue("spawn.particle-duration", 8);
                plugin.getConfigUtil().setValue("spawn.particle-points", 12);
                plugin.getConfigUtil().setValue("spawn.particle-count", 1);
                plugin.getConfigUtil().setValue("spawn.particle-spread", 0.06);
                break;
        }
    }

    private Material materialForClass(org.bysenom.minecraftSurvivors.model.PlayerClass pc) {
        if (pc == null) return Material.POPPED_CHORUS_FRUIT;
        switch (pc) {
            case SHAMAN: return Material.ENCHANTED_BOOK;
            case PYROMANCER: return Material.BLAZE_POWDER;
            case RANGER: return Material.BOW;
            case PALADIN: return Material.SHIELD;
            default: return Material.POPPED_CHORUS_FRUIT;
        }
    }

    public void openShop(Player p) {
        if (p == null) return;
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, net.kyori.adventure.text.Component.text("Händler - Ausrüstung").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        fillBorder(inv, org.bukkit.Material.ORANGE_STAINED_GLASS_PANE);
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        int slot = 10;
        // Gear only
        java.util.List<java.util.Map<?, ?>> gear = cfg.getMapList("shop.categories.gear");
        for (int i=0; i<gear.size() && slot < inv.getSize()-9; i++) {
            java.util.Map<?, ?> m = gear.get(i);
            String name = m.containsKey("name") ? String.valueOf(m.get("name")) : "Ausrüstung";
            String type = m.containsKey("type") ? String.valueOf(m.get("type")) : "ARMOR_CHEST";
            int price; try { price = Integer.parseInt(String.valueOf(m.get("price"))); } catch (Throwable t) { price = 1; }
            String rarity = m.containsKey("rarity") ? String.valueOf(m.get("rarity")) : "common";
            java.util.Map<?, ?> stats = null; Object so = m.get("stats"); if (so instanceof java.util.Map<?, ?>) stats = (java.util.Map<?, ?>) so;
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("Seltenheit: "+rarity).color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            lore.add(net.kyori.adventure.text.Component.text("Preis: "+price+" Coins").color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            if (stats != null) {
                Object prot = stats.get("prot"); if (prot != null) lore.add(net.kyori.adventure.text.Component.text("Schutz "+prot).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
                Object health = stats.get("health"); if (health != null) lore.add(net.kyori.adventure.text.Component.text("+"+health+"❤").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                Object speed = stats.get("speed"); if (speed != null) {
                    double spd; try { spd = Double.parseDouble(String.valueOf(speed)); } catch (Throwable t) { spd = 0.0; }
                    lore.add(net.kyori.adventure.text.Component.text("+"+(int)(spd*100)+"% Speed").color(net.kyori.adventure.text.format.NamedTextColor.BLUE));
                }
            }
            org.bukkit.Material mat = org.bukkit.Material.IRON_CHESTPLATE;
            if (type.equalsIgnoreCase("ARMOR_HELMET")) mat = org.bukkit.Material.IRON_HELMET;
            else if (type.equalsIgnoreCase("ARMOR_LEGS")) mat = org.bukkit.Material.IRON_LEGGINGS;
            else if (type.equalsIgnoreCase("ARMOR_BOOTS")) mat = org.bukkit.Material.IRON_BOOTS;
            boolean bought = sp.hasPurchased("gear:"+i);
            inv.setItem(slot++, createGuiItem(mat, net.kyori.adventure.text.Component.text(name).color(net.kyori.adventure.text.format.NamedTextColor.AQUA), lore, "shop_buy:gear:"+i, bought));
            if (slot % 9 == 8) slot += 2;
        }
        String eta = plugin.getShopManager().formatRemainingHHMMSS();
        inv.setItem(49, createGuiItem(org.bukkit.Material.CLOCK, net.kyori.adventure.text.Component.text("Reset in "+eta).color(net.kyori.adventure.text.format.NamedTextColor.YELLOW), java.util.List.of(net.kyori.adventure.text.Component.text("Täglicher Refresh um Mitternacht").color(net.kyori.adventure.text.format.NamedTextColor.GRAY)), "back"));
        p.openInventory(inv);
    }

    public boolean applyShopPurchase(Player p, String key) {
        if (p == null || key == null) return false;
        org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(p.getUniqueId());
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigUtil().getConfig();
        String[] parts = key.split(":");
        if (parts.length != 2) return false;
        String cat = parts[0];
        int idx;
        try { idx = Integer.parseInt(parts[1]); } catch (NumberFormatException ex) { return false; }
        if (!cat.equals("gear")) return false; // nur Ausrüstung
        java.util.List<java.util.Map<?, ?>> list = cfg.getMapList("shop.categories.gear");
        if (idx < 0 || idx >= list.size()) return false;
        java.util.Map<?, ?> m = list.get(idx);
        Object oType = m.containsKey("type") ? m.get("type") : "UNKNOWN";
        Object oPrice = m.containsKey("price") ? m.get("price") : 1;
        String type = String.valueOf(oType);
        int price = Integer.parseInt(String.valueOf(oPrice));
        String today = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        if (sp.getShopLastDay() == null || !today.equals(sp.getShopLastDay())) { sp.setShopLastDay(today); sp.setShopPurchasesToday(0); }
        if (sp.hasPurchased(key)) { org.bysenom.minecraftSurvivors.util.Msg.warn(p, "Bereits gekauft."); return false; }
        if (sp.getCoins() < price) { org.bysenom.minecraftSurvivors.util.Msg.err(p, "Nicht genug Coins."); return false; }
        int maxPerRun = cfg.getInt("shop.limits.max-per-run", 999);
        int maxPerDay = cfg.getInt("shop.limits.max-per-day", 999);
        if (sp.getShopPurchasesRun() >= maxPerRun) { org.bysenom.minecraftSurvivors.util.Msg.err(p, "Lauflimit erreicht (max "+maxPerRun+")."); return false; }
        if (sp.getShopPurchasesToday() >= maxPerDay) { org.bysenom.minecraftSurvivors.util.Msg.err(p, "Tageslimit erreicht (max "+maxPerDay+")."); return false; }
        // Withdraw
        sp.setCoins(sp.getCoins() - price);
        boolean applied = plugin.getShopManager().applyPurchaseGear(p, sp, type, m);
        if (!applied) { org.bysenom.minecraftSurvivors.util.Msg.warn(p, "Unbekannter Ausrüstungstyp: "+type); return false; }
        sp.markPurchased(key);
        sp.incrementShopRun(); sp.incrementShopToday(); sp.incPerRun(key); sp.incPerDay(key);
        try { p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f); } catch (Throwable ignored) {}
        org.bysenom.minecraftSurvivors.util.Msg.ok(p, "Gekauft: "+type);
        return true;
    }

    public void openAdminPanel(Player p) {
        if (p == null) return;
        if (!p.hasPermission("minecraftsurvivors.admin")) { org.bysenom.minecraftSurvivors.util.Msg.err(p, "Keine Rechte."); return; }
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Admin Panel").color(NamedTextColor.RED));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(10, createGuiItem(Material.GOLD_BLOCK, Component.text("+100 Coins").color(NamedTextColor.GOLD), java.util.List.of(Component.text("Test Coins")), "adm_coins"));
        inv.setItem(12, createGuiItem(Material.AMETHYST_SHARD, Component.text("+10 Essence").color(NamedTextColor.LIGHT_PURPLE), java.util.List.of(Component.text("Meta testen")), "adm_essence"));
        inv.setItem(14, createGuiItem(Material.ZOMBIE_HEAD, Component.text("Spawn Testmobs").color(NamedTextColor.GREEN), java.util.List.of(Component.text("5 Gegner um dich")), "adm_spawn"));
        inv.setItem(16, createGuiItem(Material.CHEST, Component.text("LevelUp öffnen").color(NamedTextColor.AQUA), java.util.List.of(Component.text("Debug LevelUp UI")), "adm_levelup"));
        inv.setItem(20, createGuiItem(Material.LIME_DYE, Component.text("Toggle Ready").color(NamedTextColor.GREEN), java.util.List.of(Component.text("Ready-Flag toggeln")), "adm_ready_toggle"));
        inv.setItem(22, createGuiItem(Material.REDSTONE_BLOCK, Component.text("Force Start").color(NamedTextColor.RED), java.util.List.of(Component.text("Countdown 3s & Start")), "adm_force_start"));
        inv.setItem(24, createGuiItem(Material.FEATHER, Component.text("Give Dash").color(NamedTextColor.AQUA), java.util.List.of(Component.text("Dash-Skill hinzufügen")), "adm_give_dash"));
        inv.setItem(26, createGuiItem(Material.ARROW, Component.text("Zurück").color(NamedTextColor.RED), java.util.List.of(Component.text("Zurück")), "back"));
        inv.setItem(18, createGuiItem(Material.EXPERIENCE_BOTTLE, Component.text("Give Skill Slot +1").color(NamedTextColor.AQUA), java.util.List.of(Component.text("Erhöhe die Anzahl der Skill-Slots um 1")), "adm_skillslot"));
        p.openInventory(inv);
    }
}
