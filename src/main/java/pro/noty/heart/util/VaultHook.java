package pro.noty.heart.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import pro.noty.heart.Hearts;

public class VaultHook {

    public static String getBalanceString(Player player) {
        Economy econ = Hearts.getEconomy();
        if (econ == null || player == null) return "";

        double balance = econ.getBalance(player);

        // Format based on balance size
        if (balance >= 1_000_000) {
            return String.format("💰 %.1fM", balance / 1_000_000);
        } else if (balance >= 10_000) {
            return String.format("💰 %.1fK", balance / 1_000);
        } else if (balance >= 1_000) {
            return String.format("💰 %.1fK", balance / 1_000);
        } else {
            return String.format("💰 %.0f", balance);
        }
    }

    public static boolean hasEconomy() {
        return Hearts.getEconomy() != null;
    }

    public static double getBalance(Player player) {
        Economy econ = Hearts.getEconomy();
        return econ != null ? econ.getBalance(player) : 0.0;
    }
}