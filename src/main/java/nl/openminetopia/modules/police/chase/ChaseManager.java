package nl.openminetopia.modules.police.chase;

import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.police.chase.objects.ChaseSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChaseManager {

    @Getter
    private static final ChaseManager instance = new ChaseManager();

    @Getter
    private final Map<UUID, ChaseSession> activeChases = new HashMap<>();

    public void startChase(Player agent, Player target) {
        UUID targetId = target.getUniqueId();
        
        // Stop existing chase if target is already being chased
        if (activeChases.containsKey(targetId)) {
            stopChase(targetId);
        }

        ChaseSession chaseSession = new ChaseSession(agent, target);
        activeChases.put(targetId, chaseSession);
        
        // Start the auto-stop timer (10 minutes)
        BukkitTask autoStopTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeChases.containsKey(targetId)) {
                    stopChase(targetId);
                }
            }
        }.runTaskLater(OpenMinetopia.getInstance(), 20L * 60 * 10); // 10 minutes
        
        chaseSession.setAutoStopTask(autoStopTask);
    }

    public void stopChase(UUID targetId) {
        ChaseSession chaseSession = activeChases.remove(targetId);
        if (chaseSession != null) {
            chaseSession.stop();
        }
    }

    public boolean isBeingChased(Player player) {
        return activeChases.containsKey(player.getUniqueId());
    }

    public ChaseSession getChaseSession(Player player) {
        return activeChases.get(player.getUniqueId());
    }

    public void stopChaseByAgent(Player agent) {
        activeChases.entrySet().removeIf(entry -> {
            ChaseSession session = entry.getValue();
            if (session.getAgent().equals(agent)) {
                session.stop();
                return true;
            }
            return false;
        });
    }

    public void banPlayerForLogout(Player player) {
        // Execute temporary ban command via console (24 hours)
        String banCommand = "tempban " + player.getName() + " 1d Uitloggen in een Achtervolging is NIET Toegestaan!";
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), banCommand);
    }
}
