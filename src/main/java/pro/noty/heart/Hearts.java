package pro.noty.heart;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.milkbowl.vault.economy.Economy;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Hearts extends JavaPlugin {

    private static Economy economy;

    // Map<Entity, Map<ViewerUUID, ArmorStand>>
    private final Map<LivingEntity, Map<UUID, ArmorStand>> heartDisplays = new HashMap<>();

    private FloodgateApi floodgate;

    @Override
    public void onEnable() {
        getLogger().info("❤️ Hearts plugin enabled!");

        // Vault setup
        setupVault();

        // Floodgate setup
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            try {
                floodgate = FloodgateApi.getInstance();
                getLogger().info("🌐 Floodgate detected!");
            } catch (Exception e) {
                floodgate = null;
                getLogger().warning("⚠ Floodgate API not found.");
            }
        }

        // Start updater
        startHeartUpdater();
    }

    @Override
    public void onDisable() {
        // Remove all ArmorStands
        for (Map<UUID, ArmorStand> map : heartDisplays.values()) {
            for (ArmorStand stand : map.values()) {
                if (!stand.isDead()) stand.remove();
            }
        }
        heartDisplays.clear();
        getLogger().info("❤️ Hearts plugin disabled.");
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
            getLogger().info("💰 Vault linked!");
        } else {
            getLogger().warning("⚠ Vault not found — balance display disabled!");
        }
    }

    public static Economy getEconomy() {
        return economy;
    }

    private void startHeartUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
                    if (!(entity instanceof LivingEntity living) || living.isDead()) continue;

                    double health = living.getHealth();
                    double maxHealth = living.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    String heartLine = buildHeartBar(health, maxHealth);

                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        updateViewerArmorStand(viewer, living, heartLine);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 10L);
    }

    private void updateViewerArmorStand(Player viewer, LivingEntity entity, String heartLine) {
        Map<UUID, ArmorStand> viewers = heartDisplays.computeIfAbsent(entity, k -> new HashMap<>());
        ArmorStand stand = viewers.get(viewer.getUniqueId());

        // Detect if viewer is Bedrock
        boolean isBedrock = floodgate != null && floodgate.isFloodgatePlayer(viewer.getUniqueId());

        if (stand == null || stand.isDead()) {
            stand = viewer.getWorld().spawn(entity.getLocation().add(0, entity.getHeight() + 0.5, 0), ArmorStand.class);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setCustomNameVisible(true);
            stand.setVisible(!isBedrock); // Java sees invisible, Bedrock sees visible
            viewers.put(viewer.getUniqueId(), stand);
        }

        // Move ArmorStand above entity
        stand.teleport(entity.getLocation().add(0, entity.getHeight() + 0.5, 0));

        // Vault balance (players only)
        String balanceLine = "";
        if (entity instanceof Player player && economy != null) {
            double bal = economy.getBalance(player);
            balanceLine = ChatColor.GOLD + String.format(" 💰%.1f", bal);
        }

        // Set the display name
        stand.setCustomName(ChatColor.RED + heartLine + balanceLine);
    }

    private String buildHeartBar(double health, double max) {
        int totalHearts = (int) (max / 2);
        int fullHearts = (int) (health / 2);
        boolean halfHeart = health % 2 != 0;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < fullHearts; i++) bar.append("❤");
        if (halfHeart) bar.append("💔");
        for (int i = fullHearts + (halfHeart ? 1 : 0); i < totalHearts; i++) bar.append("♡");

        return bar.toString();
    }
}
