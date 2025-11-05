package pro.noty.heart.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import pro.noty.heart.Hearts;

public class VaultHook {

    public static String getBalanceString(Player player) {
        Economy econ = Hearts.getEconomy();
        if (econ == null) return ""; // Vault not installed
        double balance = econ.getBalance(player);
        return String.format("💰 %.0f", balance);
    }
}
