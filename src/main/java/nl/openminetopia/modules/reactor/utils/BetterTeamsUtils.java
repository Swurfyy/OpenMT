package nl.openminetopia.modules.reactor.utils;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Utility class for BetterTeams integration
 * Uses the official BetterTeams API
 */
public class BetterTeamsUtils {

    private static boolean initialized = false;

    static {
        initialize();
    }

    /**
     * Re-initializes BetterTeams connection (useful if plugin loads after this class)
     */
    public static void reinitialize() {
        initialized = false;
        initialize();
    }

    /**
     * Checks if BetterTeams is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    private static void initialize() {
        // Check if BetterTeams is enabled
        if (!Bukkit.getPluginManager().isPluginEnabled("BetterTeams")) {
            initialized = false;
            return;
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("BetterTeams");
        if (plugin == null) {
            Bukkit.getLogger().warning("[ReactorModule] BetterTeams plugin not found!");
            initialized = false;
            return;
        }

        if (!plugin.isEnabled()) {
            initialized = false;
            return;
        }

        initialized = true;
        Bukkit.getLogger().info("[ReactorModule] BetterTeams initialized successfully!");
    }

    /**
     * Gets the team UUID for a player
     */
    public static UUID getPlayerTeamId(Player player) {
        if (!initialized || player == null) {
            return null;
        }

        try {
            Team team = Team.getTeam(player);
            if (team != null) {
                return team.getID();
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ReactorModule] Failed to get team for player " + player.getName() + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Gets the team name for a team UUID
     */
    public static String getTeamName(UUID teamId) {
        if (!initialized || teamId == null) {
            return null;
        }

        try {
            Team team = Team.getTeam(teamId);
            if (team != null) {
                return team.getName();
            }
        } catch (Exception e) {
            if (Bukkit.getPluginManager().isPluginEnabled("BetterTeams")) {
                Bukkit.getLogger().warning("[ReactorModule] Failed to get team name for " + teamId + ": " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Gets the number of online members in a team (worldwide)
     * Counts all online team members across all worlds
     */
    public static int getOnlineMemberCount(UUID teamId) {
        if (!initialized || teamId == null) {
            return 0;
        }

        try {
            Team team = Team.getTeam(teamId);
            if (team == null) {
                return 0;
            }

            com.booksaw.betterTeams.team.MemberSetComponent memberSet = team.getMembers();
            if (memberSet == null) {
                return 0;
            }

            // Get online team players - this already filters for online players
            List<TeamPlayer> onlineTeamPlayers = memberSet.getOnlineTeamPlayers();
            return onlineTeamPlayers != null ? onlineTeamPlayers.size() : 0;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ReactorModule] Failed to get online member count for team " + teamId + ": " + e.getMessage());
        }

        return 0;
    }

    /**
     * Gets all teams with at least one online member
     * Returns a collection of team UUIDs
     */
    public static Collection<UUID> getAllTeamsWithOnlineMembers() {
        Set<UUID> teams = new HashSet<>();

        if (!initialized) {
            return teams;
        }

        try {
            com.booksaw.betterTeams.team.TeamManager teamManager = Team.getTeamManager();
            if (teamManager == null) {
                return teams;
            }
            Map<UUID, Team> allTeamsMap = teamManager.getLoadedTeamListClone();
            if (allTeamsMap == null) {
                return teams;
            }

            for (Team team : allTeamsMap.values()) {
                if (team == null) continue;

                try {
                    UUID teamId = team.getID();
                    int onlineCount = getOnlineMemberCount(teamId);
                    if (onlineCount > 0) {
                        teams.add(teamId);
                    }
                } catch (Exception e) {
                    // Silently skip teams with errors
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ReactorModule] Failed to get all teams: " + e.getMessage());
        }

        return teams;
    }

    /**
     * Gets the number of teams that have at least the specified minimum number of online members
     * Checks all teams across all worlds
     * 
     * @param minMembersPerTeam Minimum number of online members required per team
     * @return Number of teams that meet the requirement
     */
    public static int getOnlineTeamsWithMinMembers(int minMembersPerTeam) {
        if (!initialized) {
            return 0;
        }

        int onlineTeamsWithMinMembers = 0;

        try {
            com.booksaw.betterTeams.team.TeamManager teamManager = Team.getTeamManager();
            if (teamManager == null) {
                return 0;
            }
            Map<UUID, Team> allTeamsMap = teamManager.getLoadedTeamListClone();
            if (allTeamsMap == null) {
                return 0;
            }

            for (Team team : allTeamsMap.values()) {
                if (team == null) continue;

                try {
                    com.booksaw.betterTeams.team.MemberSetComponent memberSet = team.getMembers();
                    if (memberSet == null) continue;

                    // Get online team players - this already filters for online players
                    List<TeamPlayer> onlineTeamPlayers = memberSet.getOnlineTeamPlayers();
                    int online = onlineTeamPlayers != null ? onlineTeamPlayers.size() : 0;

                    if (online >= minMembersPerTeam) {
                        onlineTeamsWithMinMembers++;
                    }
                } catch (Exception e) {
                    // Silently skip teams with errors
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ReactorModule] Failed to get teams with min members: " + e.getMessage());
        }

        return onlineTeamsWithMinMembers;
    }

    /**
     * Adds currency to a team
     */
    public static boolean addCurrencyToTeam(UUID teamId, double amount) {
        if (!initialized || teamId == null) {
            return false;
        }

        try {
            Team team = Team.getTeam(teamId);
            if (team == null) {
                return false;
            }

            // Try to add balance to the team
            try {
                // BetterTeams 4.15.2 uses getMoney() and setMoney()
                double currentBalance = team.getMoney();
                team.setMoney(currentBalance + amount);
                return true;
            } catch (Exception e) {
                // Balance system might not be available or method name differs
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
