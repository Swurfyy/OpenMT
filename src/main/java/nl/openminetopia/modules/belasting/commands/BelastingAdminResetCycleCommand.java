package nl.openminetopia.modules.belasting.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.belasting.BelastingModule;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.command.CommandSender;

@CommandAlias("belasting")
public class BelastingAdminResetCycleCommand extends BaseCommand {

    @Subcommand("admin reset-cycle")
    @CommandPermission("openminetopia.belasting.admin")
    @Description("Reset de laatste cyclustijd zodat de volgende run opnieuw facturen aanmaakt.")
    public void onResetCycle(CommandSender sender) {
        BelastingModule module = OpenMinetopia.getModuleManager().get(BelastingModule.class);
        BelastingConfiguration config = module.getConfig();

        module.getConfig().setLastAndNextCycleRun(0, 0);
        ChatUtils.sendMessage(sender, config.getMessageResetCycleDone());
    }
}
