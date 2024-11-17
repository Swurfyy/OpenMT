package nl.openminetopia.modules.police.walkietalkie;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class WalkieTalkieManager {

    private final List<UUID> policeChatPlayers = new ArrayList<>();
    private final Map<UUID, UUID> composingPrivateMessage = new HashMap<>();
    private final Map<UUID, Long> emergencyButtonCooldown = new HashMap<>();

    /* -------- Private messaging -------- */
    public void startComposingMessage(Player player, Player target) {
        composingPrivateMessage.put(player.getUniqueId(), target.getUniqueId());
    }

    public void cancelComposeMessage(Player player) {
        composingPrivateMessage.remove(player.getUniqueId());
    }

    public boolean isComposingMessage(Player player) {
        return composingPrivateMessage.containsKey(player.getUniqueId());
    }

    public Player getTarget(Player player) {
        UUID targetUuid = composingPrivateMessage.get(player.getUniqueId());
        return Bukkit.getServer().getPlayer(targetUuid);
    }

    /* -------- Police chat -------- */

    public void setPoliceChatEnabled(Player player, boolean enabled) {
        if (enabled) policeChatPlayers.add(player.getUniqueId());
        else policeChatPlayers.remove(player.getUniqueId());
    }

    public boolean isPoliceChatEnabled(Player player) {
        return policeChatPlayers.contains(player.getUniqueId());
    }

    /* -------- Emergency button -------- */
    public boolean hasCooldown(Player player) {
        if (emergencyButtonCooldown.containsKey(player.getUniqueId())) {
            if (emergencyButtonCooldown.get(player.getUniqueId()) < System.currentTimeMillis()) {
                return true;
            }
            emergencyButtonCooldown.remove(player.getUniqueId());
        }
        return false;
    }
}
