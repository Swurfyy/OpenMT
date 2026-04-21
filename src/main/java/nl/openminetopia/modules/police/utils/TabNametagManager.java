package nl.openminetopia.modules.police.utils;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import nl.openminetopia.OpenMinetopia;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * TAB-based nametag manager that replaces the old scoreboard-based implementation.
 * Uses TAB API to hide/show nametags, behaving exactly like /tab nametag hide <player>.
 */
public class TabNametagManager {

    private static TabNametagManager instance;
    private TabAPI tabApi;
    private NameTagManager nameTagManager;
    private boolean initializationAttempted = false;
    private boolean bridgeUnavailableLogged = false;
    private final Map<UUID, Integer> bridgeRetryCounters = new ConcurrentHashMap<>();

    private TabNametagManager() {
        // Lazy initialization - don't initialize here, wait until first use
    }

    public static TabNametagManager getInstance() {
        if (instance == null) {
            instance = new TabNametagManager();
        }
        return instance;
    }

    /**
     * Initializes TAB API if not already attempted.
     * This is called lazily on first use to ensure TAB is fully loaded.
     */
    private void ensureInitialized() {
        if (initializationAttempted) {
            return;
        }
        initializationAttempted = true;

        // If TAB is not installed on this backend but TAB-Bridge is, API fallback is unavailable
        // but bridge-based handling can still work.
        boolean tabEnabled = Bukkit.getPluginManager().isPluginEnabled("TAB");
        boolean bridgeEnabled = Bukkit.getPluginManager().isPluginEnabled("TAB-Bridge");
        if (!tabEnabled) {
            if (!bridgeEnabled) {
                Bukkit.getLogger().warning("[OpenMinetopia] Neither TAB nor TAB-Bridge plugin found. Balaclava nametag hiding will not work.");
            } else {
                Bukkit.getLogger().info("[OpenMinetopia] TAB not found on backend; using TAB-Bridge pathway for balaclava nametag handling.");
            }
            return;
        }
        
        try {
            tabApi = TabAPI.getInstance();
            if (tabApi != null) {
                nameTagManager = tabApi.getNameTagManager();
                if (nameTagManager == null) {
                    Bukkit.getLogger().warning("[OpenMinetopia] TAB plugin is installed but nametag feature is disabled. Balaclava nametag hiding will not work.");
                } else {
                    Bukkit.getLogger().info("[OpenMinetopia] TAB API initialized successfully. Balaclava nametag hiding is enabled.");
                }
            } else {
                Bukkit.getLogger().warning("[OpenMinetopia] TAB API instance is null. Balaclava nametag hiding will not work.");
            }
        } catch (NoClassDefFoundError e) {
            Bukkit.getLogger().warning("[OpenMinetopia] TAB API classes not available. Is TAB plugin installed? Balaclava nametag hiding will not work.");
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to initialize TAB API", e);
        }
    }

    /**
     * Checks if TAB API is available and nametag feature is enabled.
     * @return true if TAB is available and ready to use
     */
    public boolean isAvailable() {
        ensureInitialized();
        return tabApi != null && nameTagManager != null;
    }

    /**
     * Gets the TabPlayer for a Bukkit Player.
     * @param player The Bukkit player
     * @return TabPlayer or null if not available
     */
    @Nullable
    private TabPlayer getTabPlayer(@NotNull Player player) {
        if (tabApi == null) return null;
        try {
            return tabApi.getPlayer(player.getUniqueId());
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to get TabPlayer for " + player.getName(), e);
            return null;
        }
    }

    /**
     * Hides the nametag for a player using TAB API.
     * This behaves exactly like /tab nametag hide <player>.
     * @param player The player whose nametag should be hidden
     */
    public void hideNameTag(@NotNull Player player) {
        if (setBridgeInvisibility(player, true)) {
            return;
        }

        if (!isAvailable()) {
            return;
        }

        TabPlayer tabPlayer = getTabPlayer(player);
        if (tabPlayer == null) {
            return;
        }

        try {
            nameTagManager.hideNameTag(tabPlayer);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to hide nametag for " + player.getName(), e);
        }
    }

    /**
     * Shows/restores the nametag for a player using TAB API.
     * This restores the nametag to its default state.
     * @param player The player whose nametag should be shown
     */
    public void showNameTag(@NotNull Player player) {
        if (setBridgeInvisibility(player, false)) {
            return;
        }

        if (!isAvailable()) {
            return;
        }

        TabPlayer tabPlayer = getTabPlayer(player);
        if (tabPlayer == null) {
            return;
        }

        try {
            nameTagManager.showNameTag(tabPlayer);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to show nametag for " + player.getName(), e);
        }
    }

    /**
     * Checks if a player's nametag is currently hidden.
     * @param player The player to check
     * @return true if nametag is hidden, false otherwise
     */
    public boolean hasHiddenNameTag(@NotNull Player player) {
        Boolean bridgeInvisible = getBridgeInvisibility(player);
        if (Boolean.TRUE.equals(bridgeInvisible)) return true;

        if (!isAvailable()) {
            return false;
        }

        TabPlayer tabPlayer = getTabPlayer(player);
        if (tabPlayer == null) {
            return false;
        }

        try {
            return nameTagManager.hasHiddenNameTag(tabPlayer);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to check nametag status for " + player.getName(), e);
            return false;
        }
    }

    /**
     * Handles player join - ensures nametag state is correct for new viewers.
     * @param player The player who joined
     */
    public void onPlayerJoin(@NotNull Player player) {
        // TAB handles this automatically, but we can ensure state is correct
        // by checking if player should have hidden nametag
        if (!isAvailable()) {
            return;
        }

        // If player is wearing balaclava, ensure nametag is hidden
        // This is handled by PlayerArmorChangeListener, but we can double-check here
        TabPlayer tabPlayer = getTabPlayer(player);
        if (tabPlayer == null) {
            return;
        }

        // TAB automatically syncs nametag state for new players
        // No manual intervention needed
    }

    /**
     * Handles player quit - cleanup is handled automatically by TAB.
     * @param player The player who quit
     */
    public void onPlayerQuit(@NotNull Player player) {
        // TAB handles cleanup automatically
        // No manual cleanup needed
    }

    @Nullable
    private Object getBridgePlayer(@NotNull Player player) {
        Plugin bridgePlugin = Bukkit.getPluginManager().getPlugin("TAB-Bridge");
        if (bridgePlugin == null || !bridgePlugin.isEnabled()) {
            if (!bridgeUnavailableLogged) {
                Bukkit.getLogger().info("[OpenMinetopia] TAB-Bridge not available, using TAB API fallback for nametag handling.");
                bridgeUnavailableLogged = true;
            }
            return null;
        }
        try {
            Class<?> tabBridgeClass = Class.forName(
                    "me.neznamy.tab.bridge.shared.TABBridge",
                    true,
                    bridgePlugin.getClass().getClassLoader()
            );
            Method getInstance = tabBridgeClass.getMethod("getInstance");
            Object bridge = getInstance.invoke(null);
            if (bridge == null) return null;
            Method getPlayer = tabBridgeClass.getMethod("getPlayer", java.util.UUID.class);
            Object bridgePlayer = getPlayer.invoke(bridge, player.getUniqueId());
            return bridgePlayer;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to fetch BridgePlayer for " + player.getName(), e);
            return null;
        }
    }

    private boolean setBridgeInvisibility(@NotNull Player player, boolean invisible) {
        Object bridgePlayer = getBridgePlayer(player);
        if (bridgePlayer == null) {
            scheduleBridgeRetry(player.getUniqueId(), invisible);
            return false;
        }
        bridgeRetryCounters.remove(player.getUniqueId());
        try {
            Method setInvisible = bridgePlayer.getClass().getMethod("setInvisible", boolean.class);
            setInvisible.invoke(bridgePlayer, invisible);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to set BridgePlayer invisibility for " + player.getName(), e);
            return false;
        }
    }

    private void scheduleBridgeRetry(@NotNull UUID playerUuid, boolean invisible) {
        int attempts = bridgeRetryCounters.getOrDefault(playerUuid, 0);
        if (attempts >= 8) {
            if (attempts == 8) {
                bridgeRetryCounters.put(playerUuid, 9);
                Bukkit.getLogger().warning("[OpenMinetopia] TAB-Bridge did not expose BridgePlayer in time for " + playerUuid + ". Falling back to TAB API.");
            }
            return;
        }
        bridgeRetryCounters.put(playerUuid, attempts + 1);
        Bukkit.getScheduler().runTaskLater(OpenMinetopia.getInstance(), () -> {
            Player online = Bukkit.getPlayer(playerUuid);
            if (online == null || !online.isOnline()) {
                bridgeRetryCounters.remove(playerUuid);
                return;
            }
            setBridgeInvisibility(online, invisible);
        }, 10L);
    }

    @Nullable
    private Boolean getBridgeInvisibility(@NotNull Player player) {
        Object bridgePlayer = getBridgePlayer(player);
        if (bridgePlayer == null) return null;
        try {
            Method isInvisible = bridgePlayer.getClass().getMethod("isInvisible");
            Object value = isInvisible.invoke(bridgePlayer);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to read BridgePlayer invisibility for " + player.getName(), e);
            return null;
        }
    }
}
