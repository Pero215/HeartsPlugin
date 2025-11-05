package pro.noty;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.milkbowl.vault.economy.Economy;

public class Hearts extends JavaPlugin implements Listener {

    private Economy economy;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // Setup Vault economy (optional)
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            getServer().getLogger().info("Vault found. Hooking into economy...");
            if (getServer().getServicesManager().getRegistration(Economy.class) != null) {
                economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
            }
        }

        // Update hearts every second
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllEntityHearts();
            }
        }.runTaskTimer(this, 0L, 20L);

        getLogger().info("Hearts plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Hearts plugin disabled!");
    }

    private void updateAllEntityHearts() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Entity entity : viewer.getWorld().getEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;

                double health = Math.round(living.getHealth() * 10.0) / 10.0;
                double maxHealth = Math.round(living.getMaxHealth() * 10.0) / 10.0;

                // Build heart symbol based on percentage
                String heartSymbol = getHeartSymbol(health, maxHealth);
                String nameTag;

                if (living instanceof Player player) {
                    String skull = "💀"; // skull for players
                    nameTag = ChatColor.RED + skull + " " + heartSymbol + " " + health + "/" + maxHealth;

                    // Add Vault balance if available
                    if (economy != null) {
                        double bal = economy.getBalance(player);
                        nameTag += ChatColor.GRAY + " | $" + Math.round(bal);
                    }
                } else {
                    // For mobs/animals: only heart and health
                    nameTag = ChatColor.RED + heartSymbol + " " + health + "/" + maxHealth;
                }

                living.setCustomNameVisible(true);
                living.setCustomName(nameTag);
            }
        }
    }

    private String getHeartSymbol(double health, double maxHealth) {
        double ratio = health / maxHealth;
        if (ratio >= 0.75) return "❤";
        else if (ratio >= 0.5) return "♥";
        else if (ratio >= 0.25) return "♡";
        else return "💔";
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateAllEntityHearts();
                }
            }.runTaskLater(this, 5L);
        }
    }

    @EventHandler
    public void onEntityHeal(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateAllEntityHearts();
                }
            }.runTaskLater(this, 5L);
        }
    }
}
