package nl.openminetopia.modules.reactor.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.RequiredArgsConstructor;
import nl.openminetopia.modules.reactor.ReactorModule;
import nl.openminetopia.modules.reactor.objects.Reactor;
import nl.openminetopia.modules.reactor.utils.BetterTeamsUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

@RequiredArgsConstructor
public class ReactorRegionListener implements Listener {

    private final ReactorModule module;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Check if player joins in a reactor region
        Player player = event.getPlayer();
        ProtectedRegion reactorRegion = getReactorRegion(player.getLocation());
        
        if (reactorRegion != null) {
            Reactor reactor = module.getReactorManager().getOrCreateReactor(reactorRegion);
            if (reactor != null) {
                UUID teamId = BetterTeamsUtils.getPlayerTeamId(player);
                reactor.addPlayer(player, teamId);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if player moved to a different block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        
        // Check if player is in a reactor region
        ProtectedRegion reactorRegion = getReactorRegion(event.getTo());
        
        if (reactorRegion != null) {
            // Player entered or is in a reactor region
            Reactor reactor = module.getReactorManager().getOrCreateReactor(reactorRegion);
            if (reactor != null) {
                // Get player's team
                UUID teamId = BetterTeamsUtils.getPlayerTeamId(player);
                reactor.addPlayer(player, teamId);
            }
        } else {
            // Check if player left a reactor region
            ProtectedRegion previousRegion = getReactorRegion(event.getFrom());
            if (previousRegion != null) {
                Reactor reactor = module.getReactorManager().getReactor(previousRegion.getId());
                if (reactor != null) {
                    reactor.removePlayer(player.getUniqueId());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Remove player from all reactors
        for (Reactor reactor : module.getReactorManager().getReactors()) {
            reactor.removePlayer(player.getUniqueId());
        }
    }

    /**
     * Gets the reactor region at the given location, if any
     */
    private ProtectedRegion getReactorRegion(org.bukkit.Location location) {
        // WorldGuard is accessed via WorldGuard.getInstance() in newer versions
        com.sk89q.worldguard.protection.managers.RegionManager manager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        
        if (manager == null) return null;

        ApplicableRegionSet regions = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        
        for (ProtectedRegion region : regions) {
            if (nl.openminetopia.utils.WorldGuardUtils.getRegionFlag(region, ReactorModule.REACTOR_FLAG)) {
                return region;
            }
        }
        
        return null;
    }
}
