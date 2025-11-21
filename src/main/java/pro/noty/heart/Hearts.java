package pro.noty.heart;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;

import net.milkbowl.vault.economy.Economy;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Hearts extends JavaPlugin {

    private static Economy economy;

    // One ArmorStand per tracked entity
    private final Map<LivingEntity, ArmorStand> heartDisplays = new HashMap<>();

    // Floodgate API instance if available (null otherwise)
    private FloodgateApi floodgate;

    // Configuration-ish values (you can make these read from config later)
    private final long TICK_INTERVAL = 10L; // update every 10 ticks (0.5s)
    private final double ACTION_BAR_RADIUS = 12.0; // radius to send actionbar to bedrock players

    @Override
    public void onEnable() {
        getLogger().info("❤️ Hearts plugin enabling...");

        // Vault setup
        setupVault();

        // Floodgate setup (safe)
        try {
            if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
                floodgate = FloodgateApi.getInstance();
                getLogger().info("🌐 Floodgate detected and hooked.");
            } else {
                floodgate = null;
                getLogger().info("🌐 Floodgate not present.");
            }
        } catch (Throwable t) {
            // If Floodgate classes are not present at runtime, avoid crashing
            floodgate = null;
            getLogger().warning("⚠ Floodgate API unavailable: " + t.getMessage());
        }

        // Start the repeating updater
        startHeartUpdater();
        getLogger().info("❤️ Hearts plugin enabled.");
    }

    @Override
    public void onDisable() {
        // cleanup armor stands
        for (ArmorStand as : heartDisplays.values()) {
            if (as != null && !as.isDead()) as.remove();
        }
        heartDisplays.clear();
        getLogger().info("❤️ Hearts plugin disabled.");
    }

    private void setupVault() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    economy = rsp.getProvider();
                    getLogger().info("💰 Vault economy provider linked.");
                    return;
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        economy = null;
        getLogger().warning("⚠ Vault not found or not linked - economy features disabled.");
    }

    public static Economy getEconomy() {
        return economy;
    }

    private void startHeartUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Clean up dead entities from our map first
                Iterator<Map.Entry<LivingEntity, ArmorStand>> iter = heartDisplays.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<LivingEntity, ArmorStand> e = iter.next();
                    LivingEntity ent = e.getKey();
                    ArmorStand stand = e.getValue();
                    if (ent == null || ent.isDead() || stand == null || stand.isDead()) {
                        if (stand != null && !stand.isDead()) stand.remove();
                        iter.remove();
                    }
                }

                // Iterate through all worlds and their entities
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (!(entity instanceof LivingEntity living)) continue;
                        if (living.isDead()) continue;

                        // Safely get max health attribute (guard null)
                        AttributeInstance maxAttr = living.getAttribute(Attribute.MAX_HEALTH);
                        double maxHealth = (maxAttr != null) ? maxAttr.getValue() : 20.0;
                        double health = living.getHealth();

                        String heartLine = buildHeartBar(health, maxHealth);

                        // Ensure we have an armorstand for this entity
                        ArmorStand stand = heartDisplays.get(living);
                        if (stand == null || stand.isDead()) {
                            // spawn the stand slightly above head
                            Location spawnLoc = living.getLocation().add(0.0, living.getHeight() + 0.6, 0.0);
                            stand = (ArmorStand) living.getWorld().spawn(spawnLoc, ArmorStand.class);
                            stand.setVisible(false);          // invisible body
                            stand.setMarker(true);            // small hitbox
                            stand.setGravity(false);          // doesn't fall
                            stand.setCustomNameVisible(true); // show custom name (floating text)
                            heartDisplays.put(living, stand);
                        }

                        // Move stand to track the entity (teleport is fine for this frequency)
                        Location above = living.getLocation().add(0.0, living.getHeight() + 0.6, 0.0);
                        stand.teleport(above);

                        // Append balance if entity is a player and economy present
                        String balancePart = "";
                        if (living instanceof Player player && economy != null) {
                            double bal = economy.getBalance(player);
                            balancePart = ChatColor.GOLD + String.format("  💰%.1f", bal);
                        }

                        // Set the custom name (colored). This will be visible as floating text.
                        stand.setCustomName(ChatColor.RED + heartLine + balancePart);

                        // ACTION BAR fallback for Bedrock players:
                        // Send the hearts to any Floodgate (Bedrock) players within radius so they can see them
                        if (floodgate != null) {
                            for (Player viewer : Bukkit.getOnlinePlayers()) {
                                try {
                                    if (floodgate.isFloodgatePlayer(viewer.getUniqueId())) {
                                        // only send action bar for viewer if the entity is reasonably close
                                        if (viewer.getWorld().equals(living.getWorld())
                                                && viewer.getLocation().distanceSquared(living.getLocation()) <= ACTION_BAR_RADIUS * ACTION_BAR_RADIUS) {
                                            viewer.sendActionBar(Component.text(ChatColor.stripColor(heartLine + balancePart)));                                        }
                                    }
                                } catch (Throwable t) {
                                    // ignore per-player errors to avoid spamming logs
                                }
                            }
                        }
                    }
                } // end worlds loop
            }
        }.runTaskTimer(this, 0L, TICK_INTERVAL);
    }

    /**
     * Build a heart bar using icons:
     *  - full heart '❤' = 2 HP
     *  - half heart '💔' = 1 HP
     *  - empty '♡' for missing hearts
     */
    private String buildHeartBar(double health, double max) {
        int totalHearts = Math.max(1, (int) Math.round(max / 2.0)); // ensure at least 1
        int fullHearts = (int) (health / 2.0);
        boolean half = ((int) health) % 2 == 1;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fullHearts; i++) sb.append("❤");
        if (half) sb.append("💔");
        int used = fullHearts + (half ? 1 : 0);
        for (int i = used; i < totalHearts; i++) sb.append("♡");
        return sb.toString();
    }
}
