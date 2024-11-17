package nl.openminetopia.modules.police.pepperspray;

import lombok.Getter;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

public class PeppersprayManager {

    @Getter
    private static final PeppersprayManager instance = new PeppersprayManager();

    public void pepperspray(MinetopiaPlayer targetMinetopiaPlayer, MinetopiaPlayer sourceMinetopiaPlayer) {
        Player target = targetMinetopiaPlayer.getBukkit().getPlayer();
        if (target == null) return;

        Player source = sourceMinetopiaPlayer.getBukkit().getPlayer();
        if (source == null) return;

        target.sendMessage(ChatUtils.format(sourceMinetopiaPlayer, "<red>Je bent gepeppersprayed!"));
        PeppersprayUtils.applyPeppersprayEffects(target);
    }
}
