package nl.openminetopia.modules.police.handcuff.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.openminetopia.modules.police.handcuff.utils.HandcuffUtils;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

@AllArgsConstructor
@Data
public class HandcuffedPlayer {

    private final Player source;
    private final Player player;

    public void handcuff() {
        HandcuffUtils.applyHandcuffEffects(player);
        player.sendMessage(ChatUtils.color("<red>Je bent in de boeien geslagen!"));
    }

    public void release() {
        HandcuffUtils.clearHandcuffEffects(player);
        player.sendMessage(ChatUtils.color("<red>Je bent uit de boeien gehaald!"));
    }
}
