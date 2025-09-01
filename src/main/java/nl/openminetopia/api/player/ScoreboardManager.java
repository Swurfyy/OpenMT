package nl.openminetopia.api.player;

import lombok.Getter;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.scoreboard.ScoreboardModule;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ScoreboardManager {

    private static ScoreboardManager instance;

    public static ScoreboardManager getInstance() {
        if (instance == null) {
            instance = new ScoreboardManager();
        }
        return instance;
    }

    public final HashMap<UUID, Sidebar> scoreboards = new HashMap<>();

    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastTouchMs = new HashMap<>();
    private static final long MIN_UPDATE_INTERVAL_MS = 250;
    private static final int BATCH_PER_TICK = 128;

    public void initTicker() {
        Bukkit.getScheduler().runTaskTimer(OpenMinetopia.getInstance(), this::flushDirty, 2L, 10L);
    }

    /** Markeer een speler voor update (met simpele rate-limit) */
    public void markDirty(UUID id) {
        long now = System.currentTimeMillis();
        Long last = lastTouchMs.get(id);
        if (last == null || (now - last) >= MIN_UPDATE_INTERVAL_MS) {
            dirty.add(id);
            lastTouchMs.put(id, now);
        }
    }

    /** (Optioneel) iedereen periodiek verversen, bv. elke 3s */
    private long lastHeartbeat = 0;
    private void heartbeatAll() {
        long now = System.currentTimeMillis();
        if (now - lastHeartbeat >= 3000) {
            lastHeartbeat = now;
            dirty.addAll(scoreboards.keySet());
        }
    }

    private void flushDirty() {
        heartbeatAll();

        int budget = BATCH_PER_TICK;
        Iterator<UUID> it = dirty.iterator();
        while (it.hasNext() && budget-- > 0) {
            UUID id = it.next();
            it.remove();

            Sidebar sidebar = scoreboards.get(id);
            if (sidebar == null) continue;

            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                removeScoreboard(id);
                continue;
            }

            MinetopiaPlayer mtp = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
            if (mtp == null) continue;

            updateBoard(mtp);
        }
    }

    public void updateBoard(MinetopiaPlayer minetopiaPlayer) {
        Sidebar sidebar = getScoreboard(minetopiaPlayer.getUuid());
        if (sidebar == null) return;
        if (!minetopiaPlayer.isScoreboardVisible()) return;

        Player player = minetopiaPlayer.getBukkit().getPlayer();
        if (player == null) return;

        if (!minetopiaPlayer.isInPlace()) {
            if (sidebar.players().contains(player)) removeScoreboard(player);
            return;
        }
        if (!sidebar.players().contains(player)) addScoreboard(player);

        List<String> lines = OpenMinetopia.getDefaultConfiguration().getScoreboardLines();
        int size = Math.min(lines.size(), 16);
        for (int i = 0; i < size; i++) {
            String line = lines.get(i);
            if (i == 0) {
                sidebar.title(ChatUtils.format(minetopiaPlayer, line));
            } else {
                sidebar.line(i - 1, ChatUtils.format(minetopiaPlayer, line));
            }
        }
    }

    public void addScoreboard(Player player) {
        if (!OpenMinetopia.getDefaultConfiguration().isScoreboardEnabled()) return;
        if (scoreboards.containsKey(player.getUniqueId())) return;

        Sidebar sidebar = OpenMinetopia.getModuleManager()
                .get(ScoreboardModule.class)
                .getScoreboardLibrary()
                .createSidebar();

        sidebar.addPlayer(player);
        scoreboards.put(player.getUniqueId(), sidebar);

        markDirty(player.getUniqueId());
    }

    public void removeScoreboard(Player player) {
        removeScoreboard(player.getUniqueId());
    }

    public void removeScoreboard(UUID uuid) {
        Sidebar sidebar = scoreboards.remove(uuid);
        if (sidebar != null) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) sidebar.removePlayer(p);
            sidebar.close();
        }
        dirty.remove(uuid);
        lastTouchMs.remove(uuid);
    }

    public Sidebar getScoreboard(UUID uuid) {
        return scoreboards.get(uuid);
    }
}
