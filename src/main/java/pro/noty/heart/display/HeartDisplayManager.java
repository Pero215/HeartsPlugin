package pro.noty.heart.display;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.floodgate.api.FloodgateApi;
import pro.noty.heart.Hearts;
import pro.noty.heart.util.HeartUtils;
import pro.noty.heart.util.VaultHook;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Enhanced HeartDisplayManager with Floodgate support and utility class integration
 */
public class HeartDisplayManager {

    private final Hearts plugin;
    private final FloodgateApi floodgate;
    private final Economy economy;
    private final Map<UUID, ArmorStand> displays = new HashMap<>();
    private final Set<UUID> hideHologramsFor = new HashSet<>();
    private final Set<UUID> hideActionBarsFor = new HashSet<>();
    private final Set<UUID> bedrockPlayers = new HashSet<>();

    private static final long TICK_INTERVAL = 10L; // 0.5s
    private static final double ACTIONBAR_RADIUS = 15.0;

    private BukkitRunnable updateTask;
    private int tickCounter = 0;
    private Method sendActionBarMethod;

    public HeartDisplayManager(Hearts plugin, FloodgateApi floodgate, Economy economy) {
        this.plugin = plugin;
        this.floodgate = floodgate;
        this.economy = economy;
        detectBedrockPlayers();
        setupActionBarMethod();
        emergencyCleanup(); // Clean up any existing towering on startup
    }

    /**
     * Set up action bar method reflection for cross-version compatibility
     */
    private void setupActionBarMethod() {
        try {
            // Try to find the sendActionBar method in Player class (1.12+)
            sendActionBarMethod = Player.class.getMethod("sendActionBar", String.class);
            plugin.getLogger().info("Using modern action bar method");
        } catch (NoSuchMethodException e) {
            sendActionBarMethod = null;
            plugin.getLogger().info("Using legacy action bar fallback");
        }
    }

    public void start() {
        if (updateTask != null) return;

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCounter++;
                cleanupDeadStands();
                updateAllEntities();
                updateBedrockPlayerCache();
            }
        };
        updateTask.runTaskTimer(plugin, 0L, TICK_INTERVAL);

        plugin.getLogger().info("Heart display manager started with Floodgate support");
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        cleanup();
        plugin.getLogger().info("Heart display manager stopped");
    }

    private void detectBedrockPlayers() {
        bedrockPlayers.clear();
        if (floodgate != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (floodgate.isFloodgatePlayer(player.getUniqueId())) {
                    bedrockPlayers.add(player.getUniqueId());
                }
            }
        }
    }

    private void updateBedrockPlayerCache() {
        // Update cache every 600 ticks (30 seconds) to catch new Bedrock players
        if (tickCounter % 600 == 0) {
            detectBedrockPlayers();
        }
    }

    public boolean isBedrockPlayer(UUID playerId) {
        return bedrockPlayers.contains(playerId);
    }

    // Toggle methods for player preferences
    public void toggleHologramsForPlayer(UUID viewer) {
        if (hideHologramsFor.contains(viewer)) {
            hideHologramsFor.remove(viewer);
        } else {
            hideHologramsFor.add(viewer);
        }
        refreshHologramVisibility();
    }

    public void toggleActionBarsForPlayer(UUID viewer) {
        if (hideActionBarsFor.contains(viewer)) {
            hideActionBarsFor.remove(viewer);
        } else {
            hideActionBarsFor.add(viewer);
        }
    }

    public boolean areHologramsHidden(UUID viewer) {
        return hideHologramsFor.contains(viewer);
    }

    public boolean areActionBarsHidden(UUID viewer) {
        return hideActionBarsFor.contains(viewer);
    }

    private void refreshHologramVisibility() {
        for (ArmorStand stand : displays.values()) {
            if (stand != null && !stand.isDead()) {
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (hideHologramsFor.contains(viewer.getUniqueId())) {
                        viewer.hideEntity(plugin, stand);
                    } else {
                        viewer.showEntity(plugin, stand);
                    }
                }
            }
        }
    }

    private void updateAllEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (living.isDead()) continue;
                updateEntityDisplay(living);
            }
        }
    }

    private void updateEntityDisplay(LivingEntity living) {
        UUID id = living.getUniqueId();

        // Calculate health information using HeartUtils
        AttributeInstance maxAttr = living.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = (maxAttr != null) ? maxAttr.getValue() : 20.0;
        double health = Math.max(0.0, living.getHealth());
        String heartBar = HeartUtils.getColoredHearts(health, maxHealth);

        // Handle ArmorStand hologram (for Java players)
        updateHologram(living, id, heartBar);

        // Handle action bar displays
        sendActionBarDisplays(living, id, heartBar, maxHealth, health);
    }

    private void updateHologram(LivingEntity living, UUID id, String heartBar) {
        // First, check if we already have a valid stand for this entity
        ArmorStand existingStand = displays.get(id);

        // If we have an existing stand that's valid, use it
        if (existingStand != null && !existingStand.isDead() && existingStand.isValid()) {
            updateExistingStand(existingStand, living, heartBar);
            return;
        }

        // If existing stand is invalid, remove it
        if (existingStand != null) {
            if (!existingStand.isDead()) {
                existingStand.remove();
            }
            displays.remove(id);
        }

        // Clean up any other stands that might be attached to this entity (towering fix)
        removeDuplicateStands(living);

        // Create new stand
        ArmorStand newStand = spawnStand(living, heartBar);
        displays.put(id, newStand);
    }

    private void updateExistingStand(ArmorStand stand, LivingEntity living, String heartBar) {
        // Update position
        Location target = living.getLocation().add(0.0, living.getHeight() + 0.6, 0.0);

        // Only teleport if significant movement occurred
        if (!stand.getLocation().getWorld().equals(target.getWorld()) ||
                stand.getLocation().distanceSquared(target) > 0.1) {
            stand.teleport(target);
        }

        // Update name if changed
        String currentName = stand.getCustomName();
        if (currentName == null || !currentName.equals(heartBar)) {
            stand.setCustomName(heartBar);
        }

        if (!stand.isCustomNameVisible()) {
            stand.setCustomNameVisible(true);
        }
    }

    /**
     * CRITICAL FIX: Remove any duplicate stands for the same entity
     */
    private void removeDuplicateStands(LivingEntity living) {
        UUID entityId = living.getUniqueId();
        Location entityLoc = living.getLocation();
        double radius = 3.0; // Check within 3 blocks

        for (Entity nearby : living.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof ArmorStand stand) {
                // Check if this stand has the same custom name pattern (hearts)
                if (stand.getCustomName() != null && stand.getCustomName().contains("❤")) {
                    // Check if it's very close to the entity (likely a duplicate)
                    if (stand.getLocation().distanceSquared(entityLoc) < 4.0) {
                        // Remove the duplicate stand
                        if (!stand.isDead()) {
                            stand.remove();
                        }
                        // Also remove from our tracking if it was there
                        displays.values().removeIf(s -> s != null && s.equals(stand));
                    }
                }
            }
        }
    }

    private void sendActionBarDisplays(LivingEntity living, UUID entityId, String heartBar, double maxHealth, double health) {
        double radiusSq = ACTIONBAR_RADIUS * ACTIONBAR_RADIUS;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!isViewerEligible(viewer, living, radiusSq)) continue;

            String displayText = buildActionBarText(living, entityId, viewer.getUniqueId(), heartBar, health, maxHealth);

            if (!displayText.isEmpty()) {
                sendActionBar(viewer, displayText);
            }
        }
    }

    private boolean isViewerEligible(Player viewer, LivingEntity target, double radiusSq) {
        // Skip if viewer has action bars hidden
        if (hideActionBarsFor.contains(viewer.getUniqueId())) return false;

        // Check world and distance
        if (!viewer.getWorld().equals(target.getWorld())) return false;
        if (viewer.getLocation().distanceSquared(target.getLocation()) > radiusSq) return false;

        return true;
    }

    private String buildActionBarText(LivingEntity living, UUID entityId, UUID viewerId, String heartBar, double health, double maxHealth) {
        StringBuilder text = new StringBuilder();

        boolean isBedrockViewer = isBedrockPlayer(viewerId);
        boolean isTargetPlayer = living instanceof Player;
        boolean isSelf = entityId.equals(viewerId);

        // Different display logic based on viewer type and target
        if (isBedrockViewer) {
            // Bedrock players always see hearts + health info
            text.append(heartBar);

            // Add health percentage for Bedrock (more helpful for them)
            int healthPercent = (int) ((health / maxHealth) * 100);
            text.append(" ").append(ChatColor.WHITE).append(healthPercent).append("%");

        } else if (!isSelf) {
            // Java players see hearts when looking at others
            text.append(heartBar);
        }

        // Add economy info for other players viewing a player entity (not themselves)
        if (isTargetPlayer && !isSelf) {
            String balanceText = VaultHook.getBalanceString((Player) living);
            if (!balanceText.isEmpty()) {
                if (text.length() > 0) text.append(" ");
                text.append(ChatColor.GOLD).append(balanceText);
            }
        }

        // Special case: Bedrock players looking at themselves get minimal info
        if (isSelf && isBedrockViewer) {
            int healthPercent = (int) ((health / maxHealth) * 100);
            return ChatColor.RED + "❤ " + ChatColor.WHITE + healthPercent + "%";
        }

        return text.toString();
    }

    /**
     * Cross-version action bar implementation
     */
    private void sendActionBar(Player player, String message) {
        try {
            // Method 1: Use reflection to call sendActionBar if available (1.12+)
            if (sendActionBarMethod != null) {
                sendActionBarMethod.invoke(player, message);
                return;
            }

            // Method 2: Use title API as fallback for older versions
            sendActionBarViaTitle(player, message);

        } catch (Exception e) {
            // Method 3: Final fallback - send as chat message
            player.sendMessage(ChatColor.YELLOW + "[Info] " + ChatColor.RESET + message);
        }
    }

    /**
     * Use title API to simulate action bar (works in 1.8+)
     */
    private void sendActionBarViaTitle(Player player, String message) {
        try {
            // For Minecraft 1.8-1.10, use old title method
            if (tryOldTitleMethod(player, message)) {
                return;
            }

            // For Minecraft 1.11+, use new title method
            player.sendTitle("", message, 0, 40, 10); // 2 second display

        } catch (Exception e) {
            // Titles not supported, use chat
            player.sendMessage(ChatColor.YELLOW + "[" + ChatColor.stripColor(message) + "]");
        }
    }

    /**
     * Try old title method for Minecraft 1.8-1.10
     */
    private boolean tryOldTitleMethod(Player player, String message) {
        try {
            // Old title method signature (1.8-1.10)
            Method oldTitleMethod = Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            oldTitleMethod.invoke(player, "", message, 0, 40, 10);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private ArmorStand spawnStand(LivingEntity ent, String heartBar) {
        Location loc = ent.getLocation().add(0.0, ent.getHeight() + 0.6, 0.0);
        ArmorStand stand = ent.getWorld().spawn(loc, ArmorStand.class);

        // Configure the armor stand
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setCustomName(heartBar);
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.setCollidable(false);
        stand.setSilent(true);

        // Try to set additional properties if methods exist
        try {
            Method setPickupMethod = ArmorStand.class.getMethod("setCanPickupItems", boolean.class);
            setPickupMethod.invoke(stand, false);
        } catch (Exception e) {
            // Method doesn't exist, skip it
        }

        try {
            stand.setPersistent(false); // Don't save to disk
        } catch (NoSuchMethodError e) {
            // Ignore - method might not exist in older versions
        }

        try {
            stand.setAI(false);
        } catch (NoSuchMethodError e) {
            // Ignore - method might not exist in older versions
        }

        return stand;
    }

    private void cleanupDeadStands() {
        Iterator<Map.Entry<UUID, ArmorStand>> it = displays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ArmorStand> entry = it.next();
            ArmorStand stand = entry.getValue();

            // Check if the target entity still exists and is alive
            Entity targetEntity = Bukkit.getEntity(entry.getKey());
            boolean entityValid = targetEntity instanceof LivingEntity living && !living.isDead();

            // Check if stand is invalid or entity is gone
            if (stand == null || stand.isDead() || !stand.isValid() || !entityValid) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
                it.remove();
            }
        }
    }

    /**
     * EMERGENCY CLEANUP - Remove ALL heart holograms in the entire server
     */
    public void emergencyCleanup() {
        plugin.getLogger().warning("Performing emergency armor stand cleanup for heart holograms...");

        int removedCount = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof ArmorStand stand) {
                    // Remove ANY armor stand that has hearts in its name (our holograms)
                    if (stand.getCustomName() != null &&
                            (stand.getCustomName().contains("❤") ||
                                    stand.getCustomName().contains("💔") ||
                                    stand.getCustomName().contains("♡"))) {
                        if (!stand.isDead()) {
                            stand.remove();
                            removedCount++;
                        }
                    }
                }
            }
        }

        // Clear our tracking
        displays.clear();
        plugin.getLogger().warning("Emergency cleanup removed " + removedCount + " heart hologram armor stands");
    }

    public void cleanup() {
        // Remove all armor stands
        for (ArmorStand stand : displays.values()) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        displays.clear();

        // Clear caches
        bedrockPlayers.clear();
        hideHologramsFor.clear();
        hideActionBarsFor.clear();
        tickCounter = 0;
    }

    // API methods for other plugins
    public void refreshPlayer(Player player) {
        if (player != null && player.isOnline()) {
            updateEntityDisplay(player);
        }
    }

    public void refreshAllPlayers() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateEntityDisplay(player);
            }
        });
    }

    // Public getters for state
    public Set<UUID> getBedrockPlayers() {
        return Collections.unmodifiableSet(bedrockPlayers);
    }

    public int getActiveDisplays() {
        return displays.size();
    }

    public int getHiddenHologramCount() {
        return hideHologramsFor.size();
    }

    public int getHiddenActionBarCount() {
        return hideActionBarsFor.size();
    }

    /**
     * Force remove all displays for a specific entity
     */
    public void removeDisplay(UUID entityId) {
        ArmorStand stand = displays.remove(entityId);
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }
}