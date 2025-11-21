package pro.noty.heart;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.milkbowl.vault.economy.Economy;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Hearts extends JavaPlugin implements Listener {

    private static Economy economy;

    // 1 armor stand per living entity
    private final Map<LivingEntity, ArmorStand> heartDisplays = new HashMap<>();

    private FloodgateApi floodgate;

    private final long UPDATE_INTERVAL = 10L;
    private final double ACTIONBAR_DISTANCE = 12.0;

    @Override
    public void onEnable() {
        getLogger().info("❤️ Hearts plugin enabling...");

        setupVault();
        setupFloodgate();

        // Register death event
        getServer().getPluginManager().registerEvents(this, this);

        // Start updater
        startHeartUpdater();

        getLogger().info("❤️ Hearts plugin enabled.");
    }

    @Override
    public void onDisable() {
        for (ArmorStand as : heartDisplays.values()) {
            if (as != null && !as.isDead()) as.remove();
        }
        heartDisplays.clear();
        getLogger().info("❤️ Hearts plugin disabled.");
    }

    // --------------------------
    // 🔧 VAULT
    // --------------------------
    private void setupVault() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    economy = rsp.getProvider();
                    getLogger().info("💰 Vault linked.");
                    return;
                }
            }
        } catch (Exception ignored) {}
        economy = null;
        getLogger().warning("⚠ Vault not found.");
    }

    // --------------------------
    // 🌐 FLOODGATE
    // --------------------------
    private void setupFloodgate() {
        try {
            if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
                floodgate = FloodgateApi.getInstance();
                getLogger().info("🌐 Floodgate detected.");
            }
        } catch (Exception ignored) {
            floodgate = null;
        }
    }

    // --------------------------
    // ❤️ HEART BAR UPDATER
    // --------------------------
    private void startHeartUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {

                // Cleanup
                Iterator<Map.Entry<LivingEntity, ArmorStand>> iter = heartDisplays.entrySet().iterator();
                while (iter.hasNext()) {
                    var e = iter.next();
                    LivingEntity le = e.getKey();
                    ArmorStand st = e.getValue();
                    if (le == null || le.isDead() || st == null || st.isDead()) {
                        if (st != null) st.remove();
                        iter.remove();
                    }
                }

                // Update hearts for all worlds
                for (World w : Bukkit.getWorlds()) {
                    for (Entity ent : w.getEntities()) {

                        if (!(ent instanceof LivingEntity living)) continue;
                        if (living.isDead()) continue;

                        AttributeInstance maxAttr = living.getAttribute(Attribute.MAX_HEALTH);
                        double max = (maxAttr != null ? maxAttr.getValue() : 20.0);
                        double hp = living.getHealth();

                        String heartBar = buildHeartBar(hp, max);

                        ArmorStand stand = heartDisplays.get(living);
                        if (stand == null || stand.isDead()) {
                            stand = w.spawn(living.getLocation().add(0, living.getHeight() + 0.6, 0), ArmorStand.class);
                            stand.setVisible(false);
                            stand.setMarker(true);
                            stand.setGravity(false);
                            stand.setCustomNameVisible(true);
                            heartDisplays.put(living, stand);
                        }

                        // Update location
                        stand.teleport(living.getLocation().add(0, living.getHeight() + 0.6, 0));

                        // Add balance (only players)
                        String bal = "";
                        if (living instanceof Player player && economy != null) {
                            bal = ChatColor.GOLD + "  💰" + String.format("%.1f", economy.getBalance(player));
                        }

                        stand.setCustomName(ChatColor.RED + heartBar + bal);

                        // Send action bar to bedrock players only
                        if (floodgate != null) {
                            for (Player viewer : Bukkit.getOnlinePlayers()) {
                                if (viewer.getWorld() != living.getWorld()) continue;
                                if (viewer.getLocation().distanceSquared(living.getLocation()) > ACTIONBAR_DISTANCE * ACTIONBAR_DISTANCE)
                                    continue;

                                try {
                                    if (floodgate.isFloodgatePlayer(viewer.getUniqueId())) {
                                        sendActionBar(viewer, ChatColor.RED + heartBar + bal);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, UPDATE_INTERVAL);
    }

    // --------------------------
    // 📢 ACTION BAR
    // --------------------------
    private void sendActionBar(Player p, String msg) {
        try {
            p.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg)
            );
        } catch (Exception e) {
            p.sendMessage(msg);
        }
    }

    // --------------------------
    // ❤️ HEART BAR BUILDER
    // --------------------------
    private String buildHeartBar(double hp, double max) {
        int total = (int) Math.round(max / 2.0);
        int full = (int) (hp / 2);
        boolean half = ((int) hp) % 2 == 1;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < full; i++) sb.append("❤");
        if (half) sb.append("💔");

        int used = full + (half ? 1 : 0);
        for (int i = used; i < total; i++) sb.append("♡");

        return sb.toString();
    }

    // --------------------------
    // 💀 CUSTOM DEATH MESSAGE
    // --------------------------
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        String victim = player.getName();
        double hp = player.getHealth();
        double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();

        String heartInfo = ChatColor.RED + " 💔 " + String.format("%.1f/%.1f", hp, maxHp);

        Entity killer = player.getKiller();

        // Killed by mob or player
        if (killer != null) {
            String killerName = killer.getType().name().replace("_", " ").toLowerCase();
            killerName = killerName.substring(0, 1).toUpperCase() + killerName.substring(1);

            event.setDeathMessage(
                    ChatColor.RED + victim +
                            ChatColor.GRAY + " was slain by " +
                            ChatColor.YELLOW + killerName +
                            heartInfo
            );
            return;
        }

        // Non-entity reason (fall, lava, void…)
        if (player.getLastDamageCause() != null) {
            DamageCause cause = player.getLastDamageCause().getCause();

            String causeName = cause.name().replace("_", " ").toLowerCase();
            causeName = causeName.substring(0, 1).toUpperCase() + causeName.substring(1);

            event.setDeathMessage(
                    ChatColor.RED + victim +
                            ChatColor.GRAY + " died due to " +
                            ChatColor.YELLOW + causeName +
                            heartInfo
            );
            return;
        }

        // Fallback
        event.setDeathMessage(ChatColor.RED + victim + " died!" + heartInfo);
    }
}
