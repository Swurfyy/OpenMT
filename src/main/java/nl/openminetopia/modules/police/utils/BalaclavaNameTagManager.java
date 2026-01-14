package nl.openminetopia.modules.police.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BalaclavaNameTagManager {

    private static final String TEAM_PREFIX = "bal_"; // kort houden!
    private static BalaclavaNameTagManager instance;

    public static BalaclavaNameTagManager getInstance() {
        if (instance == null) {
            instance = new BalaclavaNameTagManager();
        }
        return instance;
    }

    private final Set<UUID> playersWithHiddenNameTags = new HashSet<>();

    private BalaclavaNameTagManager() {
    }

    public void hideNameTag(Player player) {
        if (playersWithHiddenNameTags.add(player.getUniqueId())) {
            updateVisibility(player);
        }
    }

    public void showNameTag(Player player) {
        if (playersWithHiddenNameTags.remove(player.getUniqueId())) {
            updateVisibility(player);
        }
    }

    public boolean hasHiddenNameTag(Player player) {
        return playersWithHiddenNameTags.contains(player.getUniqueId());
    }

    public void updateVisibility(Player player) {
        boolean hide = playersWithHiddenNameTags.contains(player.getUniqueId());
        String playerName = player.getName();
        String teamName = getTeamName(player);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();
            Team team = scoreboard.getTeam(teamName);

            if (hide) {
                if (team == null) {
                    team = scoreboard.registerNewTeam(teamName);
                }
                team.setNameTagVisibility(NameTagVisibility.NEVER);

                if (!team.hasEntry(playerName)) {
                    team.addEntry(playerName);
                }
            } else {
                if (team != null) {
                    team.removeEntry(playerName);
                    if (team.getEntries().isEmpty()) {
                        team.unregister();
                    }
                }
            }
        }
    }

    public void onPlayerJoin(Player player) {
        for (UUID uuid : playersWithHiddenNameTags) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target.isOnline()) {
                updateVisibilityForViewer(target, player);
            }
        }
    }

    public void onPlayerQuit(Player player) {
        playersWithHiddenNameTags.remove(player.getUniqueId());

        String teamName = getTeamName(player);
        String playerName = player.getName();

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.removeEntry(playerName);
                if (team.getEntries().isEmpty()) {
                    team.unregister();
                }
            }
        }
    }

    private void updateVisibilityForViewer(Player target, Player viewer) {
        boolean hide = playersWithHiddenNameTags.contains(target.getUniqueId());
        String targetName = target.getName();
        String teamName = getTeamName(target);

        Scoreboard scoreboard = viewer.getScoreboard();
        Team team = scoreboard.getTeam(teamName);

        if (hide) {
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                team.setNameTagVisibility(NameTagVisibility.NEVER);
            }
            if (!team.hasEntry(targetName)) {
                team.addEntry(targetName);
            }
        } else {
            if (team != null) {
                team.removeEntry(targetName);
                if (team.getEntries().isEmpty()) {
                    team.unregister();
                }
            }
        }
    }

    private String getTeamName(Player player) {
        // ✔ Altijd <= 16 chars → GEEN errors meer
        return TEAM_PREFIX + player.getUniqueId().toString().substring(0, 8);
    }

    public Set<UUID> getPlayersWithHiddenNameTags() {
        return new HashSet<>(playersWithHiddenNameTags);
    }
}
