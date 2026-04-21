package nl.openminetopia.modules.police.utils;

import me.neznamy.tab.bridge.shared.BridgePlayer;
import me.neznamy.tab.bridge.shared.TABBridge;
import nl.openminetopia.OpenMinetopia;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Balaclava nametags via TAB-Bridge: {@link BridgePlayer#setInvisible(boolean)} hides the TAB nametag
 * on the proxy, same idea as making the player invisible for tab display.
 */
public class TabNametagManager {

    private static TabNametagManager instance;

    private boolean loggedBridgePath;
    private boolean warnedBridgeMissing;
    private boolean warnedRetriesExhausted;

    private final Map<UUID, Integer> bridgeRetryCounters = new ConcurrentHashMap<>();

    private TabNametagManager() {
    }

    public static TabNametagManager getInstance() {
        if (instance == null) {
            instance = new TabNametagManager();
        }
        return instance;
    }

    /**
     * @return true if the TAB-Bridge plugin is loaded and enabled (actual {@link BridgePlayer} may still be null briefly after join).
     */
    public boolean isAvailable() {
        Plugin bridge = Bukkit.getPluginManager().getPlugin("TAB-Bridge");
        return bridge != null && bridge.isEnabled();
    }

    private void warnIfNoBridge() {
        if (warnedBridgeMissing) return;
        warnedBridgeMissing = true;
        Bukkit.getLogger().warning("[OpenMinetopia] TAB-Bridge not found or disabled. Balaclava nametag hiding will not work.");
    }

    private void logBridgePathOnce() {
        if (loggedBridgePath) return;
        loggedBridgePath = true;
        Bukkit.getLogger().info("[OpenMinetopia] Balaclava nametags use TAB-Bridge (BridgePlayer#setInvisible).");
    }

    /**
     * Requires TAB-Bridge plugin enabled; does not log missing plugin (caller handles that).
     */
    @Nullable
    private BridgePlayer resolveBridgePlayer(@NotNull Player player) {
        try {
            TABBridge tabBridge = TABBridge.getInstance();
            if (tabBridge == null) {
                return null;
            }
            return tabBridge.getPlayer(player.getUniqueId());
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to get BridgePlayer for " + player.getName(), t);
            return null;
        }
    }

    private boolean trySetInvisible(@NotNull Player player, boolean invisible) {
        if (!isAvailable()) {
            warnIfNoBridge();
            bridgeRetryCounters.remove(player.getUniqueId());
            return false;
        }
        BridgePlayer bridgePlayer = resolveBridgePlayer(player);
        if (bridgePlayer == null) {
            scheduleBridgeRetry(player.getUniqueId(), invisible);
            return false;
        }
        bridgeRetryCounters.remove(player.getUniqueId());
        try {
            bridgePlayer.setInvisible(invisible);
            logBridgePathOnce();
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] BridgePlayer#setInvisible failed for " + player.getName(), e);
            return false;
        }
    }

    private void scheduleBridgeRetry(@NotNull UUID playerUuid, boolean invisible) {
        int attempts = bridgeRetryCounters.getOrDefault(playerUuid, 0);
        if (attempts >= 8) {
            if (attempts == 8) {
                bridgeRetryCounters.put(playerUuid, 9);
                if (!warnedRetriesExhausted) {
                    warnedRetriesExhausted = true;
                    Bukkit.getLogger().warning(
                            "[OpenMinetopia] TAB-Bridge had no BridgePlayer in time for " + playerUuid
                                    + " after several retries (player may have joined before TAB-Bridge registered them).");
                }
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
            trySetInvisible(online, invisible);
        }, 10L);
    }

    public void hideNameTag(@NotNull Player player) {
        trySetInvisible(player, true);
    }

    public void showNameTag(@NotNull Player player) {
        trySetInvisible(player, false);
    }

    public boolean hasHiddenNameTag(@NotNull Player player) {
        if (!isAvailable()) {
            return false;
        }
        BridgePlayer bridgePlayer = resolveBridgePlayer(player);
        if (bridgePlayer == null) {
            return false;
        }
        try {
            return bridgePlayer.isInvisible();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] BridgePlayer#isInvisible failed for " + player.getName(), e);
            return false;
        }
    }

    public void onPlayerJoin(@NotNull Player player) {
        // BalaclavaNameTagListener applies hide when needed
    }

    public void onPlayerQuit(@NotNull Player player) {
        bridgeRetryCounters.remove(player.getUniqueId());
    }
}
