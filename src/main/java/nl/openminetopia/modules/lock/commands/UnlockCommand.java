package nl.openminetopia.modules.lock.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.openminetopia.modules.lock.utils.LockUtil;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.WorldGuardUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CommandAlias("unlock")
public class UnlockCommand extends BaseCommand {

    @Default
    public void defaultCommand(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !LockUtil.isLockable(targetBlock)) {
            player.sendMessage(ChatUtils.color("<red>Je kijkt niet naar een blok!"));
            return;
        }

        ProtectedRegion region = WorldGuardUtils.getProtectedRegion(targetBlock.getLocation(), p -> p >= 0);
        if (region == null) {
            player.sendMessage(ChatUtils.color("<red>Je staat niet op een plot!"));
            return;
        }
        if (!region.getOwners().contains(player.getUniqueId()) && !player.hasPermission("openminetopia.lock")) {
            player.sendMessage(ChatUtils.color("<red>Je bent niet de eigenaar van dit plot!"));
            return;
        }

        LockUtil.removeLock(targetBlock);
    }
}
