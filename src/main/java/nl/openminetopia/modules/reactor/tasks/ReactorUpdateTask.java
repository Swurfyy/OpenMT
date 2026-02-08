package nl.openminetopia.modules.reactor.tasks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.RequiredArgsConstructor;
import nl.openminetopia.modules.reactor.ReactorModule;
import nl.openminetopia.modules.reactor.objects.Reactor;
import nl.openminetopia.modules.reactor.utils.BetterTeamsUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class ReactorUpdateTask extends BukkitRunnable {

    private final ReactorModule module;

    @Override
    public void run() {
        // Update all reactors
        for (Reactor reactor : module.getReactorManager().getReactors()) {
            // Verify players are still in region (handles teleports, etc.)
            verifyPlayersInRegion(reactor);
            
            // Update timer
            reactor.updateTimer();
            
            // Update hologram
            if (reactor.getHologram() != null) {
                reactor.getHologram().updateContent();
            }
        }
    }

    /**
     * Verifies which players are actually in the reactor region
     * This handles cases where players teleport or disconnect
     */
    private void verifyPlayersInRegion(Reactor reactor) {
        Set<UUID> playersToRemove = new HashSet<>();
        
        // Check all tracked players
        for (UUID playerId : reactor.getPlayersInRegion().keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                // Player is offline
                playersToRemove.add(playerId);
                continue;
            }
            
            // Check if player is still in the region
            ProtectedRegion region = reactor.getRegion();
            com.sk89q.worldguard.protection.managers.RegionManager manager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(player.getWorld()));
            
            if (manager == null) {
                playersToRemove.add(playerId);
                continue;
            }
            
            ApplicableRegionSet regions = manager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
            boolean inRegion = regions.getRegions().stream()
                    .anyMatch(r -> r.getId().equals(reactor.getRegionName()) && 
                            nl.openminetopia.utils.WorldGuardUtils.getRegionFlag(r, ReactorModule.REACTOR_FLAG));
            
            if (!inRegion) {
                playersToRemove.add(playerId);
            }
        }
        
        // Remove players that are no longer in the region
        for (UUID playerId : playersToRemove) {
            reactor.removePlayer(playerId);
        }
        
        // Add any new players in the region
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (reactor.getPlayersInRegion().containsKey(player.getUniqueId())) {
                continue; // Already tracked
            }
            
            ProtectedRegion region = reactor.getRegion();
            com.sk89q.worldguard.protection.managers.RegionManager manager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(player.getWorld()));
            
            if (manager == null) continue;
            
            ApplicableRegionSet regions = manager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
            boolean inRegion = regions.getRegions().stream()
                    .anyMatch(r -> r.getId().equals(reactor.getRegionName()) && 
                            nl.openminetopia.utils.WorldGuardUtils.getRegionFlag(r, ReactorModule.REACTOR_FLAG));
            
            if (inRegion) {
                UUID teamId = BetterTeamsUtils.getPlayerTeamId(player);
                reactor.addPlayer(player, teamId);
            }
        }
    }
}
