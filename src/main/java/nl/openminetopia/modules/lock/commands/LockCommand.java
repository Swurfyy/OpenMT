package nl.openminetopia.modules.lock.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.lock.utils.LockUtil;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.WorldGuardUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CommandAlias("lock")
public class LockCommand extends BaseCommand {

    @Default
    public void defaultCommand(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (!validateBlock(player, targetBlock)) return;
        if (LockUtil.isLocked(targetBlock)) {
            player.sendMessage(ChatUtils.color("<red>Dit blok is al <dark_red>vergrendeld<red>!"));
            return;
        }
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        player.sendMessage(ChatUtils.format(minetopiaPlayer, "<gold>Je hebt dit blok <yellow>vergrendeld<gold>."));
        LockUtil.setLocked(targetBlock, player.getUniqueId());
    }

    @Subcommand("addmember")
    @CommandCompletion("@players")
    public void addMember(Player player, OfflinePlayer target) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (!validateBlock(player, targetBlock)) return;
        if (!validateTarget(player, target)) return;

        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        player.sendMessage(ChatUtils.format(minetopiaPlayer, "<gold>Je hebt <dark_red>" + target.getName() + " <red>toegevoegd als member van dit slot."));
        LockUtil.addLockMember(targetBlock, target.getUniqueId());
    }

    @Subcommand("removemember")
    @CommandCompletion("@players")
    public void removeMember(Player player, OfflinePlayer target) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (!validateBlock(player, targetBlock)) return;
        if (!validateTarget(player, target)) return;

        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        player.sendMessage(ChatUtils.format(minetopiaPlayer, "<red>Je hebt <dark_red>" + target.getName() + " <red>verwijderd als member van dit slot."));
        LockUtil.removeLockMember(targetBlock, target.getUniqueId());
    }

    @Subcommand("addgroup")
    @CommandPermission("openminetopia.lock.group")
    public void addGroup(Player player, String group) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (!validateBlock(player, targetBlock)) return;

        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        player.sendMessage(ChatUtils.format(minetopiaPlayer, "<gold>Je hebt de groep <yellow>" + group + " <gold>toegevoegd aan dit slot."));
        LockUtil.addLockGroup(targetBlock, group);
    }

    @Subcommand("removegroup")
    @CommandPermission("openminetopia.lock.group")
    public void removeGroup(Player player, String group) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (!validateBlock(player, targetBlock)) return;

        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        player.sendMessage(ChatUtils.format(minetopiaPlayer, "<red>Je hebt de groep <dark_red>" + group + " <red>verwijderd van dit slot."));
        LockUtil.removeLockGroup(targetBlock, group);
    }

    private boolean validateBlock(Player player, Block targetBlock) {
        if (targetBlock == null || !LockUtil.isLockable(targetBlock)) {
            player.sendMessage(ChatUtils.color("<red>Je kijkt niet naar een blok!"));
            return false;
        }
        ProtectedRegion region = WorldGuardUtils.getProtectedRegion(targetBlock.getLocation(), p -> p >= 0);
        if (!player.hasPermission("openminetopia.lock")) {
            if (region == null) {
                player.sendMessage(ChatUtils.color("<red>Je staat niet op een plot!"));
                return false;
            }
            if (!region.getOwners().contains(player.getUniqueId())) {
                player.sendMessage(ChatUtils.color("<red>Je bent geen eigenaar van dit plot!"));
                return false;
            }
        }
        return true;
    }

    private boolean validateTarget(Player player, OfflinePlayer target) {
        if (target == null || !target.hasPlayedBefore()) {
            player.sendMessage(ChatUtils.color("<red>Deze speler heeft nog nooit gespeeld."));
            return false;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.color("<red>Je kan jezelf niet toevoegen als member!"));
            return false;
        }
        return true;
    }
}
