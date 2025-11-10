package org.bysenom.minecraftSurvivors.manager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bysenom.minecraftSurvivors.MinecraftSurvivors;

/**
 * Collects per-round statistics: damage by source (ability/glyph/physical), kills, coins and lootchests picked.
 * Exposes a snapshot after round end and writes an admin JSON report to disk.
 */
public class RoundStatsManager {

    private final MinecraftSurvivors plugin;

    // aggregated counters
    private final Map<String, Double> damageBySource = new ConcurrentHashMap<>(); // key -> total damage
    private final Map<UUID, Double> damageByPlayer = new ConcurrentHashMap<>(); // player -> total damage
    private final Map<UUID, Integer> killsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> coinsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lootchestsByPlayer = new ConcurrentHashMap<>();

    private long roundStartMs = 0L;
    private long roundEndMs = 0L;

    private RoundSnapshot lastSnapshot = null;

    public RoundStatsManager(MinecraftSurvivors plugin) {
        this.plugin = plugin;
    }

    public synchronized void startRound() {
        damageBySource.clear(); damageByPlayer.clear(); killsByPlayer.clear(); coinsByPlayer.clear(); lootchestsByPlayer.clear();
        roundStartMs = System.currentTimeMillis(); roundEndMs = 0L; lastSnapshot = null;
    }

    public void recordDamage(UUID player, String sourceKey, double amount) {
        if (player == null || amount <= 0.0) return;
        if (sourceKey == null) sourceKey = "unknown";
        damageBySource.merge(sourceKey, amount, Double::sum);
        damageByPlayer.merge(player, amount, Double::sum);
    }

    public void recordKill(UUID player, String entityType) {
        if (player == null) return;
        // record kill count
        killsByPlayer.merge(player, 1, Integer::sum);
        // small debug log to reference entityType (helps future analytics; log level FINEST so normally silenced)
        try { plugin.getLogger().finest("recordKill entityType: " + java.util.Objects.toString(entityType, "")); } catch (Throwable ignored) {}
    }

    public void recordCoins(UUID player, int coins) {
        if (player == null || coins == 0) return;
        coinsByPlayer.merge(player, coins, Integer::sum);
    }

    public void recordLootChestPicked(UUID player) {
        if (player == null) return;
        lootchestsByPlayer.merge(player, 1, Integer::sum);
    }

    /**
     * Ensure that a source key is present in the damage breakdown even if it produced no damage.
     * Useful for non-damaging abilities or lingering effects that should appear in the report.
     */
    public void recordSourceObserved(String sourceKey) {
        if (sourceKey == null || sourceKey.isEmpty()) return;
        damageBySource.putIfAbsent(sourceKey, 0.0);
    }

    public synchronized RoundSnapshot finishRoundAndSnapshot() {
        roundEndMs = System.currentTimeMillis();
        RoundSnapshot snap = new RoundSnapshot();
        snap.startMs = roundStartMs; snap.endMs = roundEndMs;
        snap.damageBySource = new HashMap<>(damageBySource);
        snap.damageByPlayer = new HashMap<>(damageByPlayer);
        snap.killsByPlayer = new HashMap<>(killsByPlayer);
        snap.coinsByPlayer = new HashMap<>(coinsByPlayer);
        snap.lootchestsByPlayer = new HashMap<>(lootchestsByPlayer);
        // populate player name map for offline-friendly reports
        Map<java.util.UUID, String> names = new HashMap<>();
        for (java.util.UUID id : snap.damageByPlayer.keySet()) {
            String name = null;
            try { var online = org.bukkit.Bukkit.getPlayer(id); if (online != null) name = online.getName(); } catch (Throwable ignored) {}
            if (name == null) {
                try { var off = org.bukkit.Bukkit.getOfflinePlayer(id); if (off != null) name = off.getName(); } catch (Throwable ignored) {}
            }
            if (name == null) name = "<unknown>";
            names.put(id, name);
        }
        snap.playerNames = names;
        // round length in seconds
        snap.roundLengthSec = (snap.endMs - snap.startMs) / 1000.0;
        // collect player levels (class level) for players present in the round
        Map<java.util.UUID, Integer> pLevels = new HashMap<>();
        for (java.util.UUID id : snap.damageByPlayer.keySet()) {
            try {
                org.bysenom.minecraftSurvivors.model.SurvivorPlayer sp = plugin.getPlayerManager().get(id);
                if (sp != null) pLevels.put(id, Math.max(0, sp.getClassLevel())); else pLevels.put(id, 0);
            } catch (Throwable ignored) { pLevels.put(id, 0); }
        }
        snap.playerLevels = pLevels;
        lastSnapshot = snap;
        // write admin file (auto-generated, will be cleared on server start)
        try { writeJsonReportAuto(snap); } catch (Throwable ignored) {}
        try { writeCsvReportAuto(snap); } catch (Throwable ignored) {}
        return snap;
    }

    public RoundSnapshot getLastSnapshot() { return lastSnapshot; }

    // Auto vs Export: auto reports use prefix 'roundstats_auto_' and are cleared at server start.
    public java.nio.file.Path writeCsvReportAuto(RoundSnapshot snap) {
        return writeReportFile(snap, "roundstats_auto_", ".csv");
    }

    public java.nio.file.Path writeCsvReportExport(RoundSnapshot snap) {
        return writeReportFile(snap, "roundstats_export_", ".csv");
    }

    // keep explicit json/html auto/export entry points for clarity
    public java.nio.file.Path writeHtmlReportAuto(RoundSnapshot snap) { return writeReportFile(snap, "roundstats_auto_", ".html"); }
    public java.nio.file.Path writeHtmlReportExport(RoundSnapshot snap) { return writeReportFile(snap, "roundstats_export_", ".html"); }
    public java.nio.file.Path writeJsonReportAuto(RoundSnapshot snap) { return writeReportFile(snap, "roundstats_auto_", ".json"); }
    public java.nio.file.Path writeJsonReportExport(RoundSnapshot snap) { return writeReportFile(snap, "roundstats_export_", ".json"); }

    // Generalized writer for different prefixes/extensions
    private java.nio.file.Path writeReportFile(RoundSnapshot snap, String prefix, String ext) {
        try {
            if (snap == null) return null;
            // Exports go into 'exports/YYYY-MM-DD' subfolder; auto reports stay in data folder root
            File baseDir = plugin.getDataFolder();
            boolean isExport = prefix != null && prefix.startsWith("roundstats_export_");
            File dir;
            if (isExport) {
                // create per-day folder for exports for better organization
                java.time.Instant instant = Instant.ofEpochMilli(snap.endMs);
                java.time.LocalDate ld = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                String day = ld.toString(); // YYYY-MM-DD
                dir = new File(new File(baseDir, "exports"), day);
            } else {
                dir = baseDir;
            }
            if (!dir.exists() && !dir.mkdirs()) { plugin.getLogger().warning("Failed to create data folder: " + dir.getAbsolutePath()); }
            String ts = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(snap.endMs));
            String safeTs = ts.replaceAll("[:\\\\/\\s]", "_");
            File out = new File(dir, prefix + safeTs + ext);
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(out), StandardCharsets.UTF_8))) {
                if (ext.equalsIgnoreCase(".json")) {
                    pw.write(snap.toJson());
                } else if (ext.equalsIgnoreCase(".csv")) {
                    pw.println("section,key,value");
                    for (var en : snap.damageBySource.entrySet()) pw.println("damageSource," + escapeCsv(en.getKey()) + "," + String.format(java.util.Locale.ROOT, "%.3f", en.getValue()));
                    for (var en : snap.damageByPlayer.entrySet()) pw.println("damagePlayer," + en.getKey().toString() + "," + String.format(java.util.Locale.ROOT, "%.3f", en.getValue()));
                    // write player name mapping (uuid -> name) for easier parsing
                    for (var pn : snap.playerNames.entrySet()) pw.println("playerName," + pn.getKey().toString() + "," + escapeCsv(pn.getValue()));
                    // write player levels
                    for (var pl : snap.playerLevels.entrySet()) pw.println("playerLevel," + pl.getKey().toString() + "," + pl.getValue());
                    for (var en : snap.killsByPlayer.entrySet()) pw.println("kills," + en.getKey().toString() + "," + en.getValue());
                    for (var en : snap.coinsByPlayer.entrySet()) pw.println("coins," + en.getKey().toString() + "," + en.getValue());
                    for (var en : snap.lootchestsByPlayer.entrySet()) pw.println("lootchests," + en.getKey().toString() + "," + en.getValue());
                } else {
                    // HTML: add simple CSS and an inline SVG bar chart for top sources (re-use isExport declared above)
                    pw.println("<html><head><meta charset='utf-8'><title>RoundStats " + safeTs + "</title>");
                    pw.println("<style>body{font-family:Arial,Helvetica,sans-serif;color:#222} table{border-collapse:collapse} th,td{padding:6px 8px;border:1px solid #ccc} .title{color:#2b6dad} .chart{margin:10px 0 20px}</style>");
                    pw.println("</head><body>");
                    pw.println("<h1 class=\"title\">Round Statistics</h1>");
                    pw.println("<p><strong>Type:</strong> " + (isExport ? "Export" : "Auto-generated") + " &nbsp;|&nbsp; <strong>Duration:</strong> " + ((snap.endMs - snap.startMs)/1000.0) + "s</p>");
                    // prepare top sources for chart
                    java.util.List<java.util.Map.Entry<String, Double>> top = new java.util.ArrayList<>(snap.damageBySource.entrySet());
                    top.sort((a,b)-> Double.compare(b.getValue(), a.getValue()));
                    int chartN = Math.min(8, top.size());
                    double total = snap.damageBySource.values().stream().mapToDouble(Double::doubleValue).sum();
                    if (chartN > 0) {
                        // simple inline SVG bar chart
                        int w = 600, h = 160; int margin = 40; int barH = Math.max(12, (h - margin*2)/chartN - 6);
                        pw.println("<div class=\"chart\"><svg width=\""+w+"\" height=\""+h+"\" xmlns=\"http://www.w3.org/2000/svg\">\n");
                        double max = top.get(0).getValue();
                        for (int i=0;i<chartN;i++) {
                            var en = top.get(i);
                            double value = en.getValue();
                            int y = margin + i * (barH + 6);
                            int barW = (int) Math.round((value / (max>0?max:1.0)) * (w - margin*2));
                            String label = escapeHtml(en.getKey());
                            pw.println("<rect x=\""+margin+"\" y=\""+y+"\" width=\""+barW+"\" height=\""+barH+"\" fill=\"#2b6dad\"/>\n");
                            pw.println("<text x=\""+(margin+barW+6)+"\" y=\""+(y+barH*0.75)+"\" font-size=\"12\">"+label+"</text>\n");
                            pw.println("<text x=\""+(margin+barW-4)+"\" y=\""+(y+barH*0.75)+"\" font-size=\"11\" fill=\"#fff\" text-anchor=\"end\">"+String.format(java.util.Locale.ROOT, "%.1f", value)+"</text>\n");
                        }
                        pw.println("</svg></div>");
                    }
                    // Damage by Source table
                    pw.println("<h2>Damage by Source</h2>");
                    pw.println("<table><tr><th>Source</th><th>Damage</th><th>% of total</th></tr>");
                    for (var en : top) {
                        try {
                            double dmg = en.getValue();
                            double pct = total > 0.0 ? (dmg / total * 100.0) : 0.0;
                            pw.println("<tr><td>" + escapeHtml(en.getKey()) + "</td><td>" + String.format(java.util.Locale.ROOT, "%.2f", dmg) + "</td><td>" + String.format(java.util.Locale.ROOT, "%.1f", pct) + "%</td></tr>");
                        } catch (Throwable ignored) {}
                    }
                    pw.println("</table>");
                    pw.println("<br/>");
                    // Per-player table
                    pw.println("<h2>Per-player Damage / Kills / Coins</h2>");
                    pw.println("<table><tr><th>Player UUID</th><th>Name</th><th>Level</th><th>Damage</th><th>Kills</th><th>Coins</th><th>Lootchests</th></tr>");
                    snap.damageByPlayer.entrySet().stream().sorted((a,b)-> Double.compare(b.getValue(), a.getValue())).forEach(en -> {
                        try {
                            String name = snap.playerNames != null ? snap.playerNames.getOrDefault(en.getKey(), "<unknown>") : "<unknown>";
                            int kills = snap.killsByPlayer.getOrDefault(en.getKey(), 0);
                            int coins = snap.coinsByPlayer.getOrDefault(en.getKey(), 0);
                            int loot = snap.lootchestsByPlayer.getOrDefault(en.getKey(), 0);
                            int lvl = snap.playerLevels != null ? snap.playerLevels.getOrDefault(en.getKey(), 0) : 0;
                            pw.println("<tr><td>" + en.getKey().toString() + "</td><td>" + escapeHtml(name) + "</td><td>" + lvl + "</td><td>" + String.format(java.util.Locale.ROOT, "%.2f", en.getValue()) + "</td><td>" + kills + "</td><td>" + coins + "</td><td>" + loot + "</td></tr>");
                        } catch (Throwable ignored) {}
                    });
                    pw.println("</table>");
                    pw.println("</body></html>");
                }
            }
            plugin.getLogger().info("RoundStats written to " + out.getAbsolutePath());
            return out.toPath();
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to write round stats report: " + t.getMessage());
            return null;
        }
    }

    public java.nio.file.Path getLastReportPathJsonExport() {
        try {
            File base = plugin.getDataFolder(); File dir = new File(base, "exports"); if (!dir.exists() && !dir.mkdirs()) { plugin.getLogger().warning("Failed to create exports folder: " + dir.getAbsolutePath()); }
            String ts = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(roundEndMs));
            String safeTs = ts.replaceAll("[:\\\\/\\s]", "_");
            return new File(dir, "roundstats_export_" + safeTs + ".json").toPath();
        } catch (Throwable ignored) { return null; }
    }

    /**
     * Deletes previous auto-generated round reports (prefix 'roundstats_auto_').
     * Exported reports (prefix 'roundstats_export_') are preserved.
     */
    public void clearAutoReports() {
        try {
            File dir = plugin.getDataFolder();
            if (!dir.exists() || !dir.isDirectory()) return;
            java.io.File[] files = dir.listFiles((d, name) -> name != null && name.startsWith("roundstats_auto_"));
            if (files == null) return;
            int deleted = 0;
            for (File f : files) {
                try { if (f.delete()) deleted++; } catch (Throwable ignored) {}
            }
            if (deleted > 0) plugin.getLogger().info("Cleared " + deleted + " auto roundstats files on startup.");
        } catch (Throwable t) { plugin.getLogger().warning("Failed to clear auto reports: " + t.getMessage()); }
    }

    // Simple HTML escaping for minimal safety in generated reports
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    // Simple snapshot DTO
    public static class RoundSnapshot {
        public long startMs;
        public long endMs;
        public Map<String, Double> damageBySource = Collections.emptyMap();
        public Map<UUID, Double> damageByPlayer = Collections.emptyMap();
        public Map<UUID, Integer> killsByPlayer = Collections.emptyMap();
        public Map<UUID, Integer> coinsByPlayer = Collections.emptyMap();
        public Map<UUID, Integer> lootchestsByPlayer = Collections.emptyMap();
        public Map<UUID, String> playerNames = Collections.emptyMap(); // new field for player name mapping
        public double roundLengthSec = 0.0;
        public Map<UUID, Integer> playerLevels = Collections.emptyMap();

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            sb.append("\"startMs\":").append(startMs).append(',');
            sb.append("\"endMs\":").append(endMs).append(',');
            sb.append("\"roundLengthSec\":").append(String.format(Locale.ROOT, "%.3f", roundLengthSec)).append(',');
            // damageBySource
            sb.append("\"damageBySource\":{");
            int i = 0;
            for (var en : damageBySource.entrySet()) {
                if (i++ > 0) sb.append(',');
                sb.append('"').append(escape(en.getKey())).append('"').append(':').append(String.format(Locale.ROOT, "%.3f", en.getValue()));
            }
            sb.append('}').append(',');
            // playerLevels
            sb.append('"').append("playerLevels").append('"').append(':').append('{');
            i = 0;
            for (var en : playerLevels.entrySet()) { if (i++>0) sb.append(','); sb.append('"').append(en.getKey().toString()).append('"').append(':').append(en.getValue()); }
            sb.append('}').append(',');
            // player name mapping
            sb.append('"').append("playerNames").append('"').append(':').append('{');
            i = 0;
            for (var en : playerNames.entrySet()) { if (i++>0) sb.append(','); sb.append('"').append(en.getKey().toString()).append('"').append(':').append('"').append(escape(en.getValue())).append('"'); }
            sb.append('}').append(',');
            // damageByPlayer
            sb.append('"').append("damageByPlayer").append('"').append(':').append('{');
            i = 0;
            for (var en : damageByPlayer.entrySet()) { if (i++>0) sb.append(','); sb.append('"').append(en.getKey().toString()).append('"').append(':').append(String.format(Locale.ROOT, "%.3f", en.getValue())); }
            sb.append('}').append(',');
            // killsByPlayer
            sb.append('"').append("killsByPlayer").append('"').append(':').append('{');
            i = 0;
            for (var en : killsByPlayer.entrySet()) { if (i++>0) sb.append(','); sb.append('"').append(en.getKey().toString()).append('"').append(':').append(en.getValue()); }
            sb.append('}').append(',');
            // coinsByPlayer
            sb.append('"').append("coinsByPlayer").append('"').append(':').append('{');
            i = 0;
            for (var en : coinsByPlayer.entrySet()) { if (i++>0) sb.append(','); sb.append('"').append(en.getKey().toString()).append('"').append(':').append(en.getValue()); }
            sb.append('}').append(',');
            // lootchestsByPlayer
            sb.append('"').append("lootchestsByPlayer").append('"').append(':').append('{');
            i = 0;
            for (var en : lootchestsByPlayer.entrySet()) { if (i++>0) sb.append(','); sb.append('"').append(en.getKey().toString()).append('"').append(':').append(en.getValue()); }
            sb.append('}');
            sb.append('}');
            return sb.toString();
        }

        private static String escape(String s) { if (s == null) return ""; return s.replace("\\","\\\\").replace("\"","\\\""); }
    }

    private static String escapeCsv(String s) { if (s == null) return ""; String out = s.replace("\"", "\"\""); if (out.contains(",") || out.contains("\n")) return "\""+out+"\""; return out; }

    /**
     * Create an aggregated summary report from all persisted export JSONs in exports/.
     * Writes JSON + HTML summary into exports/ as roundstats_export_summary_<ts>.*
     */
    public java.nio.file.Path writeAggregateReportExport() {
        try {
            File base = plugin.getDataFolder(); File exportsRoot = new File(base, "exports");
            if (!exportsRoot.exists() && !exportsRoot.mkdirs()) { plugin.getLogger().warning("Failed to create exports folder: " + exportsRoot.getAbsolutePath()); }
            // search recursively for exported JSON files under exportsRoot
            List<File> found = new ArrayList<>();
            try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(exportsRoot.toPath())) {
                walk.filter(p -> java.nio.file.Files.isRegularFile(p) && p.getFileName().toString().startsWith("roundstats_export_") && p.getFileName().toString().endsWith(".json"))
                        .forEach(p -> found.add(p.toFile()));
            } catch (Throwable ignored) {}
            if (found.isEmpty()) return null;
            Map<String, Double> totalDamageBySource = new HashMap<>();
            long totalKills = 0, totalCoins = 0, totalLoot = 0;
            int rounds = 0;
            Pattern mapPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*([0-9.+\\-eE]+)");
            for (File f : found) {
                try {
                    String s = Files.readString(f.toPath());
                    // extract damageBySource block
                    Matcher m = Pattern.compile("\"damageBySource\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL).matcher(s);
                    if (m.find()) {
                        String inner = m.group(1);
                        Matcher mm = mapPattern.matcher(inner);
                        while (mm.find()) {
                            String key = mm.group(1);
                            double val = Double.parseDouble(mm.group(2));
                            totalDamageBySource.merge(key, val, Double::sum);
                        }
                    }
                    // kills
                    Matcher mk = Pattern.compile("\"killsByPlayer\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL).matcher(s);
                    if (mk.find()) {
                        String inner = mk.group(1);
                        Matcher mm = mapPattern.matcher(inner);
                        while (mm.find()) { totalKills += Long.parseLong(mm.group(2)); }
                    }
                    // coins
                    Matcher mc = Pattern.compile("\"coinsByPlayer\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL).matcher(s);
                    if (mc.find()) {
                        String inner = mc.group(1);
                        Matcher mm = mapPattern.matcher(inner);
                        while (mm.find()) { totalCoins += Long.parseLong(mm.group(2)); }
                    }
                    // lootchests
                    Matcher ml = Pattern.compile("\"lootchestsByPlayer\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL).matcher(s);
                    if (ml.find()) {
                        String inner = ml.group(1);
                        Matcher mm = mapPattern.matcher(inner);
                        while (mm.find()) { totalLoot += Long.parseLong(mm.group(2)); }
                    }
                    rounds++;
                } catch (Throwable t) { plugin.getLogger().warning("Failed to parse export JSON " + f.getAbsolutePath() + ": " + t.getMessage()); }
            }
            if (rounds == 0) return null;
            // compute averages
            Map<String, Double> avgDamageBySource = new HashMap<>();
            double totalDamageSum = 0.0;
            for (var en : totalDamageBySource.entrySet()) { avgDamageBySource.put(en.getKey(), en.getValue() / rounds); totalDamageSum += en.getValue(); }
            String ts = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replaceAll("[:\\\\/\\s]","_");
            File outJson = new File(exportsRoot, "roundstats_export_summary_" + ts + ".json");
            Map<String,Object> summary = new LinkedHashMap<>();
            summary.put("rounds", rounds);
            summary.put("totalDamageBySource", totalDamageBySource);
            summary.put("avgDamageBySource", avgDamageBySource);
            summary.put("totalDamage", totalDamageSum);
            summary.put("totalKills", totalKills);
            summary.put("totalCoins", totalCoins);
            summary.put("totalLootchests", totalLoot);
            // write JSON
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(outJson), StandardCharsets.UTF_8))) {
                pw.println(toJsonSummary(summary));
            }
            // write simple HTML
            File outHtml = new File(exportsRoot, "roundstats_export_summary_" + ts + ".html");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(outHtml), StandardCharsets.UTF_8))) {
                pw.println("<html><head><meta charset='utf-8'><title>RoundStats Summary " + ts + "</title>");
                pw.println("<style>body{font-family:Arial,Helvetica,sans-serif}table{border-collapse:collapse}th,td{padding:6px;border:1px solid #ccc}</style>");
                pw.println("</head><body>");
                pw.println("<h1>RoundStats Summary</h1>");
                pw.println("<p>Rounds analyzed: " + rounds + "</p>");
                pw.println("<h2>Average Damage by Source (per round)</h2>");
                pw.println("<table><tr><th>Source</th><th>Avg Damage</th></tr>");
                avgDamageBySource.entrySet().stream().sorted((a,b)-> Double.compare(b.getValue(), a.getValue())).forEach(en -> pw.println("<tr><td>" + escapeHtml(en.getKey()) + "</td><td>" + String.format(java.util.Locale.ROOT, "%.2f", en.getValue()) + "</td></tr>"));
                pw.println("</table>");
                pw.println("<h2>Totals</h2>");
                pw.println("<ul><li>Total Damage (all rounds): " + String.format(java.util.Locale.ROOT, "%.2f", totalDamageSum) + "</li>");
                pw.println("<li>Total Kills: " + totalKills + "</li>");
                pw.println("<li>Total Coins: " + totalCoins + "</li>");
                pw.println("<li>Total Lootchests: " + totalLoot + "</li></ul>");
                pw.println("</body></html>");
            }
            plugin.getLogger().info("RoundStats aggregate written to " + outHtml.getAbsolutePath());
            return outHtml.toPath();
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to write aggregate report: " + t.getMessage());
            return null;
        }
    }

    private static String toJsonSummary(Map<String,Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        int i = 0;
        for (var en : map.entrySet()) {
            if (i++>0) sb.append(',');
            sb.append('"').append(en.getKey()).append('"').append(':');
             Object v = en.getValue();
             if (v instanceof Number) sb.append(v);
             else if (v instanceof Map) {
                 sb.append('{'); int j=0; for (var e2 : ((Map<?,?>)v).entrySet()) { if (j++>0) sb.append(','); sb.append('"').append(e2.getKey().toString()).append('"').append(':').append(e2.getValue().toString()); } sb.append('}');
             } else sb.append('"').append(String.valueOf(v)).append('"');
         }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Find the most recent exported JSON report under exports/ (searches recursively) and return its Path.
     */
    public java.nio.file.Path getLastReportPathJsonExportRecursive() {
        try {
            File base = plugin.getDataFolder(); File exportsRoot = new File(base, "exports");
            if (!exportsRoot.exists() || !exportsRoot.isDirectory()) return null;
            try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(exportsRoot.toPath())) {
                java.util.Optional<java.nio.file.Path> latest = walk
                        .filter(p -> java.nio.file.Files.isRegularFile(p) && p.getFileName().toString().startsWith("roundstats_export_") && p.getFileName().toString().endsWith(".json"))
                        .max(java.util.Comparator.comparingLong(p -> p.toFile().lastModified()));
                return latest.map(java.nio.file.Path::toAbsolutePath).orElse(null);
            }
        } catch (Throwable ignored) { return null; }
    }
}
