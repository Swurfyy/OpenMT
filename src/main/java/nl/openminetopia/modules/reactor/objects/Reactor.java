package nl.openminetopia.modules.reactor.objects;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.Setter;
import nl.openminetopia.modules.reactor.ReactorModule;
import nl.openminetopia.modules.reactor.holograms.ReactorHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class Reactor {

    private final ReactorModule module;
    private final String regionName;
    private final ProtectedRegion region;
    private Location centerLocation; // Not final so we can update hologram location

    private ReactorHologram hologram;
    
    // Timer state (0-100, represents percentage)
    private int timerProgress = 0;
    
    // Team currently claiming (UUID of team)
    private UUID claimingTeam = null;
    
    // Players currently in the reactor region (UUID -> Team UUID)
    private final Map<UUID, UUID> playersInRegion = new ConcurrentHashMap<>();
    
    // Placeholder UUID for players without a team (ConcurrentHashMap doesn't allow null values)
    private static final UUID NO_TEAM_PLACEHOLDER = new UUID(0, 0);
    
    // Exhaustion state
    private boolean exhausted = false;
    private long exhaustionEndTime = 0; // Timestamp when exhaustion ends

    public Reactor(ReactorModule module, String regionName, ProtectedRegion region, Location centerLocation) {
        this.module = module;
        this.regionName = regionName;
        this.region = region;
        this.centerLocation = centerLocation;
        
        // Create hologram
        this.hologram = new ReactorHologram(this, centerLocation);
    }

    /**
     * Adds a player to the reactor region
     */
    public void addPlayer(Player player, UUID teamId) {
        if (player == null) {
            return; // Player is null, skip
        }
        
        UUID playerId = player.getUniqueId();
        if (playerId == null) {
            return; // Player UUID is null, skip
        }
        
        // ConcurrentHashMap doesn't allow null values, so we use a placeholder UUID for players without team
        UUID teamIdToStore = (teamId != null) ? teamId : NO_TEAM_PLACEHOLDER;
        
        playersInRegion.put(playerId, teamIdToStore);
        updateClaimingState();
    }

    /**
     * Removes a player from the reactor region
     */
    public void removePlayer(UUID playerId) {
        playersInRegion.remove(playerId);
        updateClaimingState();
    }

    /**
     * Updates the claiming state based on current players
     */
    private void updateClaimingState() {
        // Get config values
        int requiredMembersPerTeam = module.getConfig().getRequiredMembersPerTeam();
        int requiredTeams = module.getConfig().getRequiredTeams();
        
        // FIRST: Check if there are enough teams worldwide (all worlds) with enough online members
        int onlineTeamsWithMinMembers = nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.getOnlineTeamsWithMinMembers(requiredMembersPerTeam);
        
        // If not enough teams worldwide, reset claiming
        if (onlineTeamsWithMinMembers < requiredTeams) {
            if (claimingTeam != null) {
                claimingTeam = null;
            }
            return;
        }

        // SECOND: Now check which team is in the region and can claim
        // Count players per team IN THE REGION
        Map<UUID, Integer> teamCounts = new HashMap<>();
        for (UUID teamId : playersInRegion.values()) {
            if (teamId != null && !teamId.equals(NO_TEAM_PLACEHOLDER)) {
                teamCounts.put(teamId, teamCounts.getOrDefault(teamId, 0) + 1);
            }
        }

        // Determine which team is claiming
        Set<UUID> activeTeams = new HashSet<>(playersInRegion.values());
        activeTeams.remove(null);
        activeTeams.remove(NO_TEAM_PLACEHOLDER);

        if (activeTeams.size() == 0) {
            // No teams present in region
            claimingTeam = null;
        } else if (activeTeams.size() == 1) {
            // Only one team present - they can claim (if they have enough members in region)
            UUID singleTeam = activeTeams.iterator().next();
            if (teamCounts.getOrDefault(singleTeam, 0) >= requiredMembersPerTeam) {
                claimingTeam = singleTeam;
            } else {
                claimingTeam = null;
            }
        } else {
            // Multiple teams present - timer should decrease
            claimingTeam = null;
        }
    }

    /**
     * Updates the timer based on current state
     * Called every second
     */
    public void updateTimer() {
        if (exhausted) {
            // Check if exhaustion period has ended
            if (System.currentTimeMillis() >= exhaustionEndTime) {
                exhausted = false;
                exhaustionEndTime = 0;
                timerProgress = 0;
            }
            return;
        }

        // Get config values
        int maxTimer = module.getConfig().getMaxTimer();
        int incrementPerSecond = module.getConfig().getIncrementPerSecond();
        int decrementPerSecond = module.getConfig().getDecrementPerSecond();
        int requiredMembersPerTeam = module.getConfig().getRequiredMembersPerTeam();
        int requiredTeams = module.getConfig().getRequiredTeams();
        
        // FIRST: Check if there are enough teams worldwide (all worlds) with enough online members
        int onlineTeamsWithMinMembers = nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.getOnlineTeamsWithMinMembers(requiredMembersPerTeam);
        
        // If not enough teams worldwide, timer decreases
        if (onlineTeamsWithMinMembers < requiredTeams) {
            if (timerProgress > 0) {
                timerProgress = Math.max(0, timerProgress - decrementPerSecond);
            }
            claimingTeam = null;
            return;
        }

        // SECOND: Now check timer logic based on teams in region
        // Count active teams IN THE REGION
        Set<UUID> activeTeams = new HashSet<>(playersInRegion.values());
        activeTeams.remove(null);
        activeTeams.remove(NO_TEAM_PLACEHOLDER);
        
        if (activeTeams.size() == 0) {
            // No teams present in region - timer decreases
            if (timerProgress > 0) {
                timerProgress = Math.max(0, timerProgress - decrementPerSecond);
            }
        } else if (activeTeams.size() == 1) {
            // Only one team present in region
            UUID singleTeam = activeTeams.iterator().next();
            
            // Check if this team has enough members IN THE REGION
            long teamMemberCount = playersInRegion.values().stream()
                    .filter(teamId -> teamId != null && !teamId.equals(NO_TEAM_PLACEHOLDER) && teamId.equals(singleTeam))
                    .count();

            if (teamMemberCount >= requiredMembersPerTeam) {
                // Team can claim - timer increases
                if (claimingTeam == null || claimingTeam.equals(singleTeam)) {
                    claimingTeam = singleTeam;
                    if (timerProgress < maxTimer) {
                        timerProgress = Math.min(maxTimer, timerProgress + incrementPerSecond);
                        // Check if timer just completed
                        if (timerProgress >= maxTimer) {
                            onTimerComplete();
                        }
                    }
                } else {
                    // Different team, timer decreases
                    if (timerProgress > 0) {
                        timerProgress = Math.max(0, timerProgress - decrementPerSecond);
                    }
                }
            } else {
                // Not enough members in region - timer decreases
                if (timerProgress > 0) {
                    timerProgress = Math.max(0, timerProgress - decrementPerSecond);
                }
                if (claimingTeam != null && claimingTeam.equals(singleTeam)) {
                    claimingTeam = null;
                }
            }
        } else {
            // Multiple teams present in region - timer decreases
            if (timerProgress > 0) {
                timerProgress = Math.max(0, timerProgress - decrementPerSecond);
            }
            claimingTeam = null;
        }
    }

    /**
     * Called when timer reaches 100%
     */
    private void onTimerComplete() {
        UUID completedTeam = claimingTeam;
        if (completedTeam == null) return;

        // Give reward to team
        module.getReactorManager().giveRewardToTeam(completedTeam);

        // Set exhaustion
        exhausted = true;
        long exhaustionDurationMs = module.getConfig().getExhaustionDurationMs();
        exhaustionEndTime = System.currentTimeMillis() + exhaustionDurationMs;
        timerProgress = 0;
        claimingTeam = null;

        module.getLogger().info("Reactor " + regionName + " completed! Team " + completedTeam + " received reward.");
    }

    /**
     * Gets the team name for display
     */
    public String getClaimingTeamName() {
        if (claimingTeam == null) return null;
        return module.getReactorManager().getTeamName(claimingTeam);
    }

    /**
     * Gets online member counts per team for display
     * Returns a list of "x/2" strings for up to 3 teams
     * Shows ALL teams with online members (not just those in the region)
     */
    public List<String> getTeamMemberCounts() {
        // Get all teams with online members (worldwide)
        java.util.Collection<UUID> allTeamsWithMembers = nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.getAllTeamsWithOnlineMembers();
        
        // Also include teams from players currently in the region (to prioritize them)
        Set<UUID> teamsInRegion = new HashSet<>();
        for (UUID teamId : playersInRegion.values()) {
            if (teamId != null && !teamId.equals(NO_TEAM_PLACEHOLDER)) {
                teamsInRegion.add(teamId);
            }
        }
        
        // Get online member count for each team (worldwide)
        Map<UUID, Integer> teamCounts = new HashMap<>();
        
        // First, add teams that are in the region (prioritize them)
        for (UUID teamId : teamsInRegion) {
            int onlineCount = nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.getOnlineMemberCount(teamId);
            if (onlineCount > 0) {
                teamCounts.put(teamId, onlineCount);
            }
        }
        
        // Then add other teams with online members
        for (UUID teamId : allTeamsWithMembers) {
            if (!teamCounts.containsKey(teamId)) {
                int onlineCount = nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.getOnlineMemberCount(teamId);
                if (onlineCount > 0) {
                    teamCounts.put(teamId, onlineCount);
                }
            }
        }

        List<String> counts = new ArrayList<>();
        int displayed = 0;
        
        // Sort by count (descending) to show teams with most members first
        List<Map.Entry<UUID, Integer>> sortedEntries = teamCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();
        
        int requiredTeams = module.getConfig().getRequiredTeams();
        int requiredMembersPerTeam = module.getConfig().getRequiredMembersPerTeam();
        
        for (Map.Entry<UUID, Integer> entry : sortedEntries) {
            if (displayed >= requiredTeams) break;
            counts.add(entry.getValue() + "/" + requiredMembersPerTeam);
            displayed++;
        }

        // Fill remaining slots
        while (counts.size() < requiredTeams) {
            counts.add("0/" + requiredMembersPerTeam);
        }

        return counts;
    }

    /**
     * Updates the hologram location to a new position
     * Recreates the hologram at the new location
     */
    public void updateHologramLocation(Location newLocation) {
        if (newLocation != null) {
            // Delete old hologram
            if (hologram != null) {
                hologram.delete();
            }
            
            // Update center location
            this.centerLocation = newLocation;
            
            // Recreate hologram at new location
            this.hologram = new ReactorHologram(this, newLocation);
        }
    }

    /**
     * Disables the reactor
     */
    public void disable() {
        if (hologram != null) {
            hologram.delete();
            hologram = null;
        }
        playersInRegion.clear();
    }
}
