package nl.openminetopia.modules.police.utils;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        
        // Wait a bit to ensure TAB is fully loaded
        if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            Bukkit.getLogger().warning("[OpenMinetopia] TAB plugin not found. Balaclava nametag hiding will not work.");
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
}
