package pro.noty.heart.display;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pro.noty.heart.Hearts;
import pro.noty.heart.util.HeartUtils;
import pro.noty.heart.util.VaultHook;

public class HealthDisplayManager implements Listener {

    private final Hearts plugin;

    public HealthDisplayManager(Hearts plugin) {
        this.plugin = plugin;
        startAutoUpdate();
    }

    private void startAutoUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : Bukkit.getWorlds().get(0).getEntities()) {
                    if (e instanceof LivingEntity living) {
                        updateEntityName(living);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // every 2 seconds
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof LivingEntity living) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateEntityName(living), 5L);
        }
    }

    @EventHandler
    public void onHeal(EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof LivingEntity living) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateEntityName(living), 5L);
        }
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent e) {
        if (e.getEntity() instanceof LivingEntity living) {
            updateEntityName(living);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        updateEntityName(e.getPlayer());
    }

    private void updateEntityName(LivingEntity entity) {
        double health = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();

        String heartBar = HeartUtils.getColoredHearts(health, maxHealth);

        String name;
        if (entity instanceof Player player) {
            String balance = VaultHook.getBalanceString(player);
            name = "§7💀 " + player.getName() + "\n" + heartBar + (balance.isEmpty() ? "" : "\n§6" + balance);
        } else {
            String mobName = entity.getType().name().toLowerCase().replace("_", " ");
            mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1);
            name = "§7💀 " + mobName + "\n" + heartBar;
        }

        entity.setCustomNameVisible(true);
        entity.setCustomName(name);
    }
}
