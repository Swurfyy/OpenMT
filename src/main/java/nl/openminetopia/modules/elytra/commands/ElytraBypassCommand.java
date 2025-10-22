package nl.openminetopia.modules.elytra.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.elytra.ElytraModule;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

@CommandAlias("elytrabypass|elytrab")
public class ElytraBypassCommand extends BaseCommand {

    @Default
    @CommandPermission("openminetopia.elytra.bypass")
    public void onCommand(Player player) {
        ElytraModule elytraModule = OpenMinetopia.getModuleManager().get(ElytraModule.class);
        if (elytraModule == null) {
            player.sendMessage(ChatUtils.color("§cElytra module is not enabled!"));
            return;
        }

        if (elytraModule.isPlayerBypassed(player.getUniqueId())) {
            elytraModule.removeBypass(player.getUniqueId());
            player.sendMessage(ChatUtils.color("§aElytra anti-boost is now §cENABLED §afor you."));
        } else {
            elytraModule.addBypass(player.getUniqueId());
            player.sendMessage(ChatUtils.color("§aElytra anti-boost is now §cDISABLED §afor you."));
        }
    }

    @Subcommand("toggle")
    @CommandPermission("openminetopia.elytra.bypass")
    public void toggle(Player player) {
        onCommand(player);
    }

    @Subcommand("status")
    @CommandPermission("openminetopia.elytra.bypass")
    public void status(Player player) {
        ElytraModule elytraModule = OpenMinetopia.getModuleManager().get(ElytraModule.class);
        if (elytraModule == null) {
            player.sendMessage(ChatUtils.color("§cElytra module is not enabled!"));
            return;
        }

        boolean bypassed = elytraModule.isPlayerBypassed(player.getUniqueId());
        String status = bypassed ? "§cDISABLED" : "§aENABLED";
        player.sendMessage(ChatUtils.color("§7Elytra anti-boost is currently " + status + " §7for you."));
    }
}
