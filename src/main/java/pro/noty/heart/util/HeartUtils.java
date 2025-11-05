package pro.noty.heart.util;

public class HeartUtils {

    public static String getColoredHearts(double health, double maxHealth) {
        int totalHearts = (int) Math.ceil(maxHealth / 2.0);
        StringBuilder sb = new StringBuilder();
        double remainingHealth = health;

        double healthPercent = health / maxHealth;

        // Determine base color depending on health %
        String color;
        if (healthPercent > 0.75) color = "§c";        // red (healthy)
        else if (healthPercent > 0.5) color = "§6";    // orange
        else if (healthPercent > 0.25) color = "§e";   // yellow
        else color = "§8";                             // dark gray (low)

        for (int i = 0; i < totalHearts; i++) {
            if (remainingHealth >= 2) {
                sb.append(color).append("❤");
                remainingHealth -= 2;
            } else if (remainingHealth == 1) {
                sb.append("§4💔");
                remainingHealth -= 1;
            } else {
                sb.append("§7♡");
            }
        }
        return sb.toString();
    }
}
