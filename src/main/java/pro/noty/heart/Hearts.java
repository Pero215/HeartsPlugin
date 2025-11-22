package pro.noty.heart;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player; // ADD THIS IMPORT
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.geysermc.floodgate.api.FloodgateApi;
import pro.noty.heart.display.HeartDisplayManager;

public class Hearts extends JavaPlugin {

    private static Economy economy;
    private static FloodgateApi floodgate;
    private HeartDisplayManager manager;

    @Override
    public void onEnable() {
        setupVault();
        setupFloodgate();

        manager = new HeartDisplayManager(this, floodgate, economy);
        manager.start();

        getLogger().info("❤️ Hearts plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.stop();
        getLogger().info("❤️ Hearts plugin disabled.");
    }

    private void setupVault() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    economy = rsp.getProvider();
                    getLogger().info("✓ Vault economy hooked: " + economy.getName());
                    return;
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to hook into Vault economy");
        }
        economy = null;
    }

    private void setupFloodgate() {
        try {
            if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
                floodgate = FloodgateApi.getInstance();
                getLogger().info("✓ Floodgate hooked successfully");
            } else {
                floodgate = null;
                getLogger().info("✗ Floodgate not found, using Java edition only features");
            }
        } catch (Throwable t) {
            floodgate = null;
            getLogger().warning("Failed to initialize Floodgate");
        }
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static FloodgateApi getFloodgate() {
        return floodgate;
    }

    public static boolean isFloodgatePlayer(Player player) {
        return floodgate != null && floodgate.isFloodgatePlayer(player.getUniqueId());
    }

}