package nl.openminetopia.modules.police.utils;

import nl.openminetopia.OpenMinetopia;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Balaclava nametags via TAB-Bridge {@code BridgePlayer#setInvisible(boolean)}.
 * <p>
 * TAB-Bridge's {@code DataBridge} re-syncs every second from the real Bukkit player:
 * {@code setInvisible(player.checkInvisibility())} where {@code checkInvisibility()} is only the
 * invisibility <em>potion</em>. So a one-shot {@code setInvisible(true)} is overwritten almost
 * immediately. While balaclava hiding is active we therefore re-assert {@code true} on a short timer.
 */
public class TabNametagManager {

    private static final String TAB_BRIDGE = "TAB-Bridge";
    private static final String CLASS_TAB_BRIDGE = "me.neznamy.tab.bridge.shared.TABBridge";

    /** Players for whom we keep forcing bridge invisibility until balaclava is removed. */
    private final Set<UUID> forcedBalaclavaHide = ConcurrentHashMap.newKeySet();

    private static TabNametagManager instance;

    private boolean loggedBridgePath;
    private boolean warnedBridgeMissing;
    private boolean warnedRetriesExhausted;

    private final Map<UUID, Integer> bridgeRetryCounters = new ConcurrentHashMap<>();

    private @Nullable BukkitTask maintenanceTask;

    private TabNametagManager() {
    }

    public static TabNametagManager getInstance() {
        if (instance == null) {
            instance = new TabNametagManager();
        }
        return instance;
    }

    /**
     * @return true if the TAB-Bridge plugin is loaded and enabled (BridgePlayer may still be null briefly after join).
     */
    public boolean isAvailable() {
        Plugin bridge = Bukkit.getPluginManager().getPlugin(TAB_BRIDGE);
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
        Bukkit.getLogger().info("[OpenMinetopia] Balaclava nametags use TAB-Bridge (BridgePlayer#setInvisible, re-asserted against TAB-Bridge 1s sync).");
    }

    @Nullable
    private Object resolveBridgePlayer(@NotNull Player player) {
        Plugin bridgePlugin = Bukkit.getPluginManager().getPlugin(TAB_BRIDGE);
        if (bridgePlugin == null || !bridgePlugin.isEnabled()) {
            return null;
        }
        try {
            Class<?> tabBridgeClass = Class.forName(CLASS_TAB_BRIDGE, true, bridgePlugin.getClass().getClassLoader());
            Method getInstance = tabBridgeClass.getMethod("getInstance");
            Object tabBridge = getInstance.invoke(null);
            if (tabBridge == null) {
                return null;
            }
            Method getPlayer = tabBridgeClass.getMethod("getPlayer", UUID.class);
            return getPlayer.invoke(tabBridge, player.getUniqueId());
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] Failed to get BridgePlayer for " + player.getName(), t);
            return null;
        }
    }

    private boolean bridgeSetInvisible(@Nullable Object bridgePlayer, boolean invisible) {
        if (bridgePlayer == null) {
            return false;
        }
        try {
            Method setInvisible = bridgePlayer.getClass().getMethod("setInvisible", boolean.class);
            setInvisible.invoke(bridgePlayer, invisible);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[OpenMinetopia] BridgePlayer#setInvisible failed", e);
            return false;
        }
    }

    private boolean trySetInvisible(@NotNull Player player, boolean invisible) {
        if (!isAvailable()) {
            warnIfNoBridge();
            bridgeRetryCounters.remove(player.getUniqueId());
            return false;
        }
        Object bridgePlayer = resolveBridgePlayer(player);
        if (bridgePlayer == null) {
            scheduleBridgeRetry(player.getUniqueId(), invisible);
            return false;
        }
        bridgeRetryCounters.remove(player.getUniqueId());
        if (bridgeSetInvisible(bridgePlayer, invisible)) {
            logBridgePathOnce();
            return true;
        }
        return false;
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

    /**
     * Align bridge invisibility with the real invisibility potion (TAB-Bridge's own periodic sync does this too).
     */
    private void syncBridgeInvisibilityToGame(@NotNull Player player) {
        if (!isAvailable()) {
            return;
        }
        Object bridgePlayer = resolveBridgePlayer(player);
        if (bridgePlayer == null) {
            return;
        }
        boolean gameInvisible = player.hasPotionEffect(PotionEffectType.INVISIBILITY);
        bridgeSetInvisible(bridgePlayer, gameInvisible);
    }

    private void ensureMaintenance() {
        if (maintenanceTask != null && !maintenanceTask.isCancelled()) {
            return;
        }
        maintenanceTask = Bukkit.getScheduler().runTaskTimer(OpenMinetopia.getInstance(), () -> {
            if (forcedBalaclavaHide.isEmpty()) {
                if (maintenanceTask != null) {
                    maintenanceTask.cancel();
                }
                maintenanceTask = null;
                return;
            }
            for (UUID id : new HashSet<>(forcedBalaclavaHide)) {
                Player online = Bukkit.getPlayer(id);
                if (online == null || !online.isOnline()) {
                    forcedBalaclavaHide.remove(id);
                    continue;
                }
                if (!BalaclavaUtils.isWearingBalaclava(online)) {
                    forcedBalaclavaHide.remove(id);
                    syncBridgeInvisibilityToGame(online);
                    continue;
                }
                if (!isAvailable()) {
                    continue;
                }
                Object bridgePlayer = resolveBridgePlayer(online);
                if (bridgeSetInvisible(bridgePlayer, true)) {
                    logBridgePathOnce();
                }
            }
        }, 4L, 4L);
    }

    public void hideNameTag(@NotNull Player player) {
        forcedBalaclavaHide.add(player.getUniqueId());
        ensureMaintenance();
        trySetInvisible(player, true);
    }

    public void showNameTag(@NotNull Player player) {
        forcedBalaclavaHide.remove(player.getUniqueId());
        if (forcedBalaclavaHide.isEmpty() && maintenanceTask != null) {
            maintenanceTask.cancel();
            maintenanceTask = null;
        }
        syncBridgeInvisibilityToGame(player);
    }

    public boolean hasHiddenNameTag(@NotNull Player player) {
        return forcedBalaclavaHide.contains(player.getUniqueId());
    }

    public void onPlayerJoin(@NotNull Player player) {
        // BalaclavaNameTagListener applies hide when needed
    }

    public void onPlayerQuit(@NotNull Player player) {
        bridgeRetryCounters.remove(player.getUniqueId());
        forcedBalaclavaHide.remove(player.getUniqueId());
        if (forcedBalaclavaHide.isEmpty() && maintenanceTask != null) {
            maintenanceTask.cancel();
            maintenanceTask = null;
        }
    }
}
