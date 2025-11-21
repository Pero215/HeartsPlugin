package pro.noty.heart.display;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import pro.noty.heart.Hearts;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HeartDisplayManager {

    private final Hearts plugin;

    private final Map<LivingEntity, ArmorStand> displays = new HashMap<>();

    private static final double ACTIONBAR_RADIUS = 12 * 12;

    public HeartDisplayManager(Hearts plugin) {
        this.plugin = plugin;
    }

    /** Update all displays */
    public void update() {
        cleanupDead();

        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {

                if (!(e instanceof LivingEntity living)) continue;
                if (living.isDead()) continue;

                updateForEntity(living);
            }
        }
    }

    /** Single entity update */
    private void updateForEntity(LivingEntity living) {

        AttributeInstance maxA = living.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = (maxA != null ? maxA.getValue() : 20.0);
        double hp = living.getHealth();

        String heartBar = buildHeartBar(hp, maxHp);

        ArmorStand stand = displays.get(living);
        if (stand == null || stand.isDead()) {
            stand = spawnStand(living);
            displays.put(living, stand);
        }

        stand.teleport(living.getLocation().add(0, living.getHeight() + 0.6, 0));

        // Economy
        String eco = "";
        if (living instanceof Player p && Hearts.getEconomy() != null) {
            eco = ChatColor.GOLD + "  💰" + String.format("%.1f", Hearts.getEconomy().getBalance(p));
        }

        // Set name using Adventure Component
        stand.customName(Component.text(ChatColor.RED + heartBar + eco));

        // Floodgate actionbar
        if (plugin.getFloodgateAPI() != null) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                try {
                    if (plugin.getFloodgateAPI().isFloodgatePlayer(viewer.getUniqueId())) {
                        if (viewer.getWorld() == living.getWorld()
                                && viewer.getLocation().distanceSquared(living.getLocation()) <= ACTIONBAR_RADIUS) {
                            viewer.sendActionBar(Component.text(ChatColor.stripColor(heartBar + eco)));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    /** Spawn hologram ArmorStand */
    private ArmorStand spawnStand(LivingEntity ent) {
        Location l = ent.getLocation().add(0, ent.getHeight() + 0.6, 0);

        ArmorStand as = ent.getWorld().spawn(l, ArmorStand.class);
        as.setMarker(true);
        as.setInvisible(true);
        as.setGravity(false);
        as.setCustomNameVisible(true);

        return as;
    }

    /** Cleanup */
    private void cleanupDead() {
        Iterator<Map.Entry<LivingEntity, ArmorStand>> it = displays.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<LivingEntity, ArmorStand> e = it.next();

            LivingEntity ent = e.getKey();
            ArmorStand st = e.getValue();

            if (ent == null || ent.isDead() || st == null || st.isDead()) {
                if (st != null && !st.isDead()) st.remove();
                it.remove();
            }
        }
    }

    /** Build ❤ 💔 ♡ bar */
    private String buildHeartBar(double hp, double max) {
        int total = (int) Math.ceil(max / 2.0);
        int full = (int) (hp / 2.0);
        boolean half = (hp % 2.0) >= 1;

        StringBuilder sb = new StringBuilder();

        // full hearts
        for (int i = 0; i < full; i++) sb.append("❤");

        // half
        if (half) sb.append("💔");

        // empty
        int used = full + (half ? 1 : 0);
        for (int i = used; i < total; i++) sb.append("♡");

        return sb.toString();
    }
}
