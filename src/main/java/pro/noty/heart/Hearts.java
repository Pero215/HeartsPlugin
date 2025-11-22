package pro.noty.heart;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import pro.noty.heart.display.HeartDisplayManager;

public class Hearts extends JavaPlugin {

    private static Economy economy;
    private FloodgateApi floodgate;
    private HeartDisplayManager displayManager;

    @Override
    public void onEnable() {
        getLogger().info("❤️ Hearts plugin enabling...");

        // Vault setup
        try {
            var reg = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (reg != null) {
                economy = reg.getProvider();
                getLogger().info("💰 Vault economy provider linked.");
            } else {
                economy = null;
                getLogger().warning("⚠ Vault or economy provider not found. Economy features disabled.");
            }
        } catch (Throwable t) {
            economy = null;
            getLogger().warning("⚠ Vault check failed: " + t.getMessage());
        }

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
            floodgate = null;
            getLogger().warning("⚠ Floodgate API unavailable: " + t.getMessage());
        }

        // Start the single display manager (Option A1)
        displayManager = new HeartDisplayManager(this, economy);
        getLogger().info("❤️ HeartDisplayManager started.");

        getLogger().info("❤️ Hearts plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (displayManager != null) displayManager.cleanup();
        getLogger().info("❤️ Hearts plugin disabled.");
    }

    // Accessors for other classes
    public static Economy getEconomy() {
        return economy;
    }

    public FloodgateApi getFloodgate() {
        return floodgate;
    }

    public HeartDisplayManager getDisplayManager() {
        return displayManager;
    }
}
