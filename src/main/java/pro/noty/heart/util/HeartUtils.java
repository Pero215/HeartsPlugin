package pro.noty.heart.util;

public class HeartUtils {

    public static String getColoredHearts(double health, double maxHealth) {
        int totalHearts = (int) Math.ceil(maxHealth / 2.0);
        StringBuilder sb = new StringBuilder();
        double remainingHealth = health;

        double healthPercent = health / maxHealth;

        // Determine base color depending on health %
        String baseColor;
        if (healthPercent > 0.75) baseColor = "§c";        // red (healthy)
        else if (healthPercent > 0.5) baseColor = "§6";    // orange
        else if (healthPercent > 0.25) baseColor = "§e";   // yellow
        else baseColor = "§8";                             // dark gray (low)

        for (int i = 0; i < totalHearts; i++) {
            if (remainingHealth >= 2) {
                sb.append(baseColor).append("❤");
                remainingHealth -= 2;
            } else if (remainingHealth == 1) {
                sb.append("§4💔"); // Dark red for half hearts
                remainingHealth -= 1;
            } else {
                sb.append("§7♡"); // Gray for empty hearts
            }

            // Add space between hearts for better readability (except last one)
            if (i < totalHearts - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public static String getSimpleHearts(double health, double maxHealth) {
        int totalHearts = (int) Math.ceil(maxHealth / 2.0);
        int fullHearts = (int) (health / 2);
        boolean halfHeart = (health % 2) >= 1;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fullHearts; i++) sb.append("❤");
        if (halfHeart) sb.append("💔");
        for (int i = fullHearts + (halfHeart ? 1 : 0); i < totalHearts; i++) sb.append("♡");

        return sb.toString();
    }

    public static int getHealthPercent(double health, double maxHealth) {
        if (maxHealth <= 0) return 0;
        return (int) ((health / maxHealth) * 100);
    }
}