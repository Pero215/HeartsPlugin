package pro.noty.heart;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import pro.noty.heart.display.HealthDisplayManager;

public class Hearts extends JavaPlugin {

    private static Hearts instance;
    private static Economy economy = null;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("❤️ Hearts plugin enabled with colors + economy!");

        if (!setupEconomy()) {
            getLogger().warning("⚠ Vault not found! Economy features will be disabled.");
        }

        Bukkit.getPluginManager().registerEvents(new HealthDisplayManager(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("💔 Hearts plugin disabled!");
    }

    public static Hearts getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return economy;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }
}
