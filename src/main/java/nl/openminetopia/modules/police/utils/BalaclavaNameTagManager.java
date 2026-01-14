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
        UUID uuid = player.getUniqueId();
        if (playersWithHiddenNameTags.add(uuid)) {
            updateVisibility(player);
        }
    }

    public void showNameTag(Player player) {
        UUID uuid = player.getUniqueId();
        if (playersWithHiddenNameTags.remove(uuid)) {
            updateVisibility(player);
        }
    }

    public boolean hasHiddenNameTag(Player player) {
        return playersWithHiddenNameTags.contains(player.getUniqueId());
    }

    public void updateVisibility(Player player) {
        boolean hide = playersWithHiddenNameTags.contains(player.getUniqueId());
        String playerName = player.getName();

        // Update for all online viewers
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();
            Team team = scoreboard.getTeam("balaclava_" + playerName);

            if (hide) {
                if (team == null) {
                    team = scoreboard.registerNewTeam("balaclava_" + playerName);
                    team.setNameTagVisibility(NameTagVisibility.NEVER);
                    team.addEntry(playerName);
                } else {
                    team.setNameTagVisibility(NameTagVisibility.NEVER);
                    if (!team.hasEntry(playerName)) {
                        team.addEntry(playerName);
                    }
                }
            } else {
                if (team != null && team.hasEntry(playerName)) {
                    team.removeEntry(playerName);
                    // Clean up empty team
                    if (team.getEntries().isEmpty()) {
                        team.unregister();
                    }
                }
            }
        }
    }

    public void onPlayerJoin(Player player) {
        // Update visibility for the new player for all existing players with hidden nametags
        for (UUID uuid : playersWithHiddenNameTags) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target.isOnline()) {
                updateVisibilityForViewer(target, player);
            }
        }
    }

    public void onPlayerQuit(Player player) {
        // Clean up team references when player leaves
        UUID uuid = player.getUniqueId();
        playersWithHiddenNameTags.remove(uuid);
        
        String playerName = player.getName();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();
            Team team = scoreboard.getTeam("balaclava_" + playerName);
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
        Scoreboard scoreboard = viewer.getScoreboard();
        Team team = scoreboard.getTeam("balaclava_" + targetName);

        if (hide) {
            if (team == null) {
                team = scoreboard.registerNewTeam("balaclava_" + targetName);
                team.setNameTagVisibility(NameTagVisibility.NEVER);
            }
            if (!team.hasEntry(targetName)) {
                team.addEntry(targetName);
            }
        } else {
            if (team != null && team.hasEntry(targetName)) {
                team.removeEntry(targetName);
                if (team.getEntries().isEmpty()) {
                    team.unregister();
                }
            }
        }
    }

    public Set<UUID> getPlayersWithHiddenNameTags() {
        return new HashSet<>(playersWithHiddenNameTags);
    }
}
