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

    private static final long TICK_INTERVAL = 10L;
    private static final double ACTION_BAR_RADIUS = 12.0;

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
                updateEntities();
            }
        }.runTaskTimer(plugin, 0L, TICK_INTERVAL);
    }

    private void cleanupDeadStands() {
        Iterator<Map.Entry<UUID, ArmorStand>> it = displays.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, ArmorStand> entry = it.next();
            UUID id = entry.getKey();
            ArmorStand stand = entry.getValue();

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

    private void updateEntities() {
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

        AttributeInstance maxAttr = living.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = (maxAttr != null) ? maxAttr.getValue() : 20.0;
        double health = Math.max(0, living.getHealth());

        String heartBar = buildHeartBar(health, maxHealth);

        // Add balance if this is a player
        String bal = "";
        if (living instanceof Player p && economy != null) {
            bal = ChatColor.GOLD + "  💰" + String.format("%.1f", economy.getBalance(p));
        }

        // CREATE / GET STAND
        ArmorStand stand = displays.get(id);
        if (stand == null || stand.isDead()) {
            stand = spawnStand(living);
            displays.put(id, stand);
        }

        // Teleport stand to correct location
        Location loc = living.getLocation().add(0, living.getHeight() + 0.6, 0);
        stand.teleport(loc);

        // Update text
        stand.setCustomName(ChatColor.RED + heartBar + bal);

        // Bedrock action bar fallback (Floodgate)
        if (plugin.getFloodgate() != null) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                try {
                    if (plugin.getFloodgate().isFloodgatePlayer(viewer.getUniqueId())) {
                        if (viewer.getWorld() == living.getWorld()
                                && viewer.getLocation().distanceSquared(living.getLocation()) <= ACTION_BAR_RADIUS * ACTION_BAR_RADIUS) {

                            viewer.spigot().sendMessage(
                                    ChatMessageType.ACTION_BAR,
                                    TextComponent.fromLegacyText(ChatColor.stripColor(heartBar + bal))
                            );
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private ArmorStand spawnStand(LivingEntity ent) {
        Location loc = ent.getLocation().add(0, ent.getHeight() + 0.6, 0);

        ArmorStand stand = ent.getWorld().spawn(loc, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.setGravity(false);
            s.setCustomNameVisible(true);
        });

        return stand;
    }

    /**
     * ❤ full
     * 💔 half
     * ♡ empty
     */
    private String buildHeartBar(double health, double max) {
        int total = (int) Math.round(max / 2.0);
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
