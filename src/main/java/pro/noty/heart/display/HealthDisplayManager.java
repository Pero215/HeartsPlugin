package pro.noty.heart;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HeartDisplayManager {

    private final Hearts plugin;

    // One armorstand per entity
    private final Map<LivingEntity, ArmorStand> displays = new HashMap<>();

    // Radius to show actionbar to Geyser players
    private static final double ACTIONBAR_RADIUS = 12 * 12;

    public HeartDisplayManager(Hearts plugin) {
        this.plugin = plugin;
    }

    /** Update all heart displays */
    public void update() {
        cleanupDead();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {

                if (!(entity instanceof LivingEntity living)) continue;
                if (living.isDead()) continue;

                updateForEntity(living);
            }
        }
    }

    /** Update one entity's hologram */
    private void updateForEntity(LivingEntity living) {

        // health
        AttributeInstance maxAttr = living.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = (maxAttr != null ? maxAttr.getValue() : 20.0);
        double hp = living.getHealth();

        // build hearts
        String hearts = buildHeartBar(hp, maxHp);

        // get or create hologram
        ArmorStand stand = displays.get(living);
        if (stand == null || stand.isDead()) {
            stand = spawnStand(living);
            displays.put(living, stand);
        }

        // move hologram
        stand.teleport(living.getLocation().add(0, living.getHeight() + 0.6, 0));

        // show economy
        String balPart = "";
        if (living instanceof Player p && Hearts.getEconomy() != null) {
            balPart = ChatColor.GOLD + "  💰" + String.format("%.1f", Hearts.getEconomy().getBalance(p));
        }

        // set hologram text
        stand.customName(Component.text(ChatColor.RED + hearts + balPart));

        // send actionbar to bedrock players
        if (plugin.floodgate != null) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                try {
                    if (plugin.floodgate.isFloodgatePlayer(viewer.getUniqueId())) {
                        if (viewer.getWorld().equals(living.getWorld())
                                && viewer.getLocation().distanceSquared(living.getLocation()) <= ACTIONBAR_RADIUS) {
                            viewer.sendActionBar(Component.text(ChatColor.stripColor(hearts + balPart)));
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
    }


    /** Spawn armor stand */
    private ArmorStand spawnStand(LivingEntity ent) {
        Location loc = ent.getLocation().add(0, ent.getHeight() + 0.6, 0);

        ArmorStand as = ent.getWorld().spawn(loc, ArmorStand.class);
        as.setVisible(false);
        as.setGravity(false);
        as.setMarker(true);
        as.setCustomNameVisible(true);

        return as;
    }

    /** Clean up removed mobs/players */
    private void cleanupDead() {
        Iterator<Map.Entry<LivingEntity, ArmorStand>> it = displays.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<LivingEntity, ArmorStand> e = it.next();

            LivingEntity ent = e.getKey();
            ArmorStand stand = e.getValue();

            if (ent == null || ent.isDead() || stand == null || stand.isDead()) {
                if (stand != null && !stand.isDead()) stand.remove();
                it.remove();
            }
        }
    }

    /** Build hearts (❤ 💔 ♡) clean horizontal bar */
    private String buildHeartBar(double hp, double max) {
        int total = (int) Math.ceil(max / 2.0);
        int full = (int) (hp / 2.0);
        boolean half = (hp % 2.0) >= 1;

        StringBuilder sb = new StringBuilder();

        // full hearts
        for (int i = 0; i < full; i++) sb.append("❤");

        // half heart
        if (half) sb.append("💔");

        // empty hearts
        int used = full + (half ? 1 : 0);
        for (int i = used; i < total; i++) sb.append("♡");

        return sb.toString();
    }
}
