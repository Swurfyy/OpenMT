package nl.openminetopia.modules.police.chase.objects;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

@Data
public class ChaseSession {

    @Getter
    private final Player agent;
    @Getter
    private final Player target;
    @Setter
    private BukkitTask autoStopTask;

    public ChaseSession(Player agent, Player target) {
        this.agent = agent;
        this.target = target;
    }

    public void stop() {
        if (autoStopTask != null && !autoStopTask.isCancelled()) {
            autoStopTask.cancel();
        }
        
        // Send stop message to target
        MinetopiaPlayer targetMinetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(target);
        if (targetMinetopiaPlayer != null) {
            ChatUtils.sendFormattedMessage(targetMinetopiaPlayer, MessageConfiguration.message("police_chase_stopped"));
        }
    }
}
