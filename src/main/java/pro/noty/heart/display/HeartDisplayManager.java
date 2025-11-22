package pro.noty.heart.display;

import net.milkbowl.vault.economy.Economy;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

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
import org.bukkit.scheduler.BukkitRunnable;
import pro.noty.heart.Hearts;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class HeartDisplayManager {

    private final Hearts plugin;
    private final Economy economy;
    private final Map<UUID, ArmorStand> displays = new HashMap<>();

    private static final long TICK_INTERVAL = 10L; // 10 ticks = 0.5s
    private static final double ACTIONBAR_RADIUS = 12.0; // blocks (we compare squared distances where needed)

    public HeartDisplayManager(Hearts plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        startUpdater();
    }

    private void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupDeadStands();
                updateAllEntities();
            }
        }.runTaskTimer(plugin, 0L, TICK_INTERVAL);
    }

    private void updateAllEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (living.isDead()) continue;
                updateEntityDisplay(living);
            }
        }
    }

    private void updateEntityDisplay(LivingEntity living) {
        UUID id = living.getUniqueId();

        // get health
        AttributeInstance maxAttr = living.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = (maxAttr != null) ? maxAttr.getValue() : 20.0;
        double health = Math.max(0.0, living.getHealth());

        String heartBar = buildHeartBar(health, maxHealth);

        // economy string (players only)
        String bal = "";
        if (living instanceof Player p && economy != null) {
            bal = ChatColor.GOLD + "  💰" + String.format("%.1f", economy.getBalance(p));
        }

        // get or create armorstand by UUID
        ArmorStand stand = displays.get(id);
        if (stand == null || stand.isDead()) {
            stand = spawnStand(living);
            displays.put(id, stand);
        }

        // move armorstand above entity
        Location target = living.getLocation().add(0.0, living.getHeight() + 0.6, 0.0);
        stand.teleport(target);

        // update text (String-based for widest compatibility)
        stand.setCustomName(ChatColor.RED + heartBar + bal);

        // Ensure custom name visible flag (some server versions reset this)
        stand.setCustomNameVisible(true);

        // Action bar fallback for Bedrock players (optional; A1 uses holograms for both but we keep fallback)
        if (plugin.getFloodgate() != null) {
            double radiusSq = ACTIONBAR_RADIUS * ACTIONBAR_RADIUS;
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                try {
                    if (!plugin.getFloodgate().isFloodgatePlayer(viewer.getUniqueId())) continue;
                    if (!viewer.getWorld().equals(living.getWorld())) continue;
                    if (viewer.getLocation().distanceSquared(living.getLocation()) > radiusSq) continue;

                    // Send sanitized text so Bedrock sees something even if holograms fail
                    viewer.spigot().sendMessage(
                            ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(ChatColor.stripColor(heartBar + bal))
                    );
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private ArmorStand spawnStand(LivingEntity ent) {
        Location loc = ent.getLocation().add(0.0, ent.getHeight() + 0.6, 0.0);

        ArmorStand as = ent.getWorld().spawn(loc, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.setGravity(false);
            s.setCustomNameVisible(true);
            // small = true can be used if you want smaller hitbox/visual
        });

        return as;
    }

    private void cleanupDeadStands() {
        Iterator<Map.Entry<UUID, ArmorStand>> it = displays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ArmorStand> e = it.next();
            UUID id = e.getKey();
            ArmorStand stand = e.getValue();

            // find entity by UUID (safe across versions)
            LivingEntity ent = findLivingEntityByUUID(id);

            if (ent == null || ent.isDead() || stand == null || stand.isDead()) {
                if (stand != null && !stand.isDead()) stand.remove();
                it.remove();
            }
        }
    }

    private LivingEntity findLivingEntityByUUID(UUID id) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e.getUniqueId().equals(id) && e instanceof LivingEntity living) {
                    return living;
                }
            }
        }
        return null;
    }

    public void cleanup() {
        for (ArmorStand as : displays.values()) {
            if (as != null && !as.isDead()) as.remove();
        }
        displays.clear();
    }

    /** Build horizontal heart bar using ❤ full (2hp), 💔 half (1hp), ♡ empty */
    private String buildHeartBar(double health, double max) {
        int total = Math.max(1, (int) Math.round(max / 2.0));
        int full = (int) (health / 2.0);
        boolean half = ((int) health) % 2 == 1;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < full; i++) sb.append("❤");
        if (half) sb.append("💔");
        int used = full + (half ? 1 : 0);
        for (int i = used; i < total; i++) sb.append("♡");
        return sb.toString();
    }
}
