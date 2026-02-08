package nl.openminetopia.modules.reactor;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import lombok.Getter;
import nl.openminetopia.modules.reactor.objects.Reactor;
import nl.openminetopia.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ReactorManager {

    private final ReactorModule module;
    private final Map<String, Reactor> reactors = new ConcurrentHashMap<>(); // Key: region name

    public ReactorManager(ReactorModule module) {
        this.module = module;
        scanForReactors();
    }

    /**
     * Scans all regions for reactor flags and creates Reactor instances
     * Also removes reactors that no longer have the flag enabled
     */
    public void scanForReactors() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        Set<String> activeRegionNames = new HashSet<>();
        
        // Scan all worlds for reactor regions
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager == null) continue;

            for (ProtectedRegion region : manager.getRegions().values()) {
                if (WorldGuardUtils.getRegionFlag(region, ReactorModule.REACTOR_FLAG)) {
                    // This region has the reactor flag enabled
                    String regionName = region.getId();
                    activeRegionNames.add(regionName);
                    
                    if (!reactors.containsKey(regionName)) {
                        // Calculate center location for hologram
                        Location center = calculateRegionCenter(region, world);
                        Reactor reactor = new Reactor(module, regionName, region, center);
                        reactors.put(regionName, reactor);
                        module.getLogger().info("Found reactor region: " + regionName + " at " + center);
                    } else {
                        // Reactor already exists, update hologram location to new height (4 blocks)
                        Reactor existingReactor = reactors.get(regionName);
                        if (existingReactor != null) {
                            // Calculate new center location with updated height
                            Location newCenter = calculateRegionCenter(region, world);
                            // Update hologram location (moves it 2 blocks higher)
                            existingReactor.updateHologramLocation(newCenter);
                        }
                    }
                }
            }
        }
        
        // Remove reactors that no longer have the flag enabled
        Set<String> reactorsToRemove = new HashSet<>(reactors.keySet());
        reactorsToRemove.removeAll(activeRegionNames);
        for (String regionName : reactorsToRemove) {
            removeReactor(regionName);
            module.getLogger().info("Removed reactor region (flag disabled): " + regionName);
        }
    }

    /**
     * Gets or creates a reactor for the given region
     */
    public Reactor getOrCreateReactor(ProtectedRegion region) {
        String regionName = region.getId();
        Reactor reactor = reactors.get(regionName);
        
        if (reactor == null) {
            // Find the world by searching through all worlds for this region
            World world = null;
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            for (World w : Bukkit.getWorlds()) {
                RegionManager manager = container.get(BukkitAdapter.adapt(w));
                if (manager != null && manager.hasRegion(regionName)) {
                    world = w;
                    break;
                }
            }
            if (world == null) return null;
            
            Location center = calculateRegionCenter(region, world);
            reactor = new Reactor(module, regionName, region, center);
            reactors.put(regionName, reactor);
            module.getLogger().info("Created new reactor for region: " + regionName);
        }
        
        return reactor;
    }

    /**
     * Gets a reactor by region name
     */
    public Reactor getReactor(String regionName) {
        return reactors.get(regionName);
    }

    /**
     * Removes a reactor (when flag is disabled)
     */
    public void removeReactor(String regionName) {
        Reactor reactor = reactors.remove(regionName);
        if (reactor != null) {
            reactor.disable();
        }
    }

    /**
     * Calculates the center location of a region for hologram placement
     * Places hologram at the center of the region, 4 blocks above the nearest solid block
     */
    private Location calculateRegionCenter(ProtectedRegion region, World world) {
        com.sk89q.worldedit.math.BlockVector3 min = region.getMinimumPoint();
        com.sk89q.worldedit.math.BlockVector3 max = region.getMaximumPoint();
        
        // Calculate center X and Z coordinates
        // Add 0.5 to place at the center of the middle block (not on the edge)
        double centerX = (min.x() + max.x()) / 2.0 + 0.5;
        double centerZ = (min.z() + max.z()) / 2.0 + 0.5;
        
        // Calculate center Y coordinate (middle of the region height)
        double centerY = (min.y() + max.y()) / 2.0;
        
        // Find the nearest solid block below the center point
        int solidBlockY = findNearestSolidBlock(world, centerX, centerY, centerZ, min.y());
        
        // Place hologram 4 blocks above the solid block (2 blocks higher than before)
        double hologramY = solidBlockY + 4.0;
        
        return new Location(world, centerX, hologramY, centerZ);
    }
    
    /**
     * Finds the nearest solid block below the given Y coordinate
     * Searches from centerY down to minY
     */
    private int findNearestSolidBlock(World world, double x, double startY, double z, int minY) {
        int searchY = (int) Math.floor(startY);
        
        // Search downward from center to minimum Y
        for (int y = searchY; y >= minY; y--) {
            org.bukkit.Location loc = new org.bukkit.Location(world, x, y, z);
            org.bukkit.block.Block block = loc.getBlock();
            
            // Check if block is solid
            if (block.getType().isSolid()) {
                return y;
            }
        }
        
        // If no solid block found, use the minimum Y as fallback
        return minY;
    }

    /**
     * Gets all active reactors
     */
    public Collection<Reactor> getReactors() {
        return reactors.values();
    }

    /**
     * Gets the team name for a team UUID
     */
    public String getTeamName(UUID teamId) {
        return nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.getTeamName(teamId);
    }

    /**
     * Gives reward to a team
     */
    public void giveRewardToTeam(UUID teamId) {
        // Get reward amount from configuration
        double rewardAmount = module.getConfig().getRewardAmount();
        
        boolean success = nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.addCurrencyToTeam(teamId, rewardAmount);
        if (success) {
            module.getLogger().info("Gave " + rewardAmount + " currency to team " + getTeamName(teamId));
        } else {
            module.getLogger().warn("Failed to give currency reward to team " + getTeamName(teamId));
        }
    }

    /**
     * Disables all reactors
     */
    public void disable() {
        for (Reactor reactor : reactors.values()) {
            reactor.disable();
        }
        reactors.clear();
    }
}
