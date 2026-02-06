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
public class BelastingAdminSimulateCommand extends BaseCommand {

    @Subcommand("admin simulate")
    @CommandPermission("openminetopia.belasting.admin")
    @Description("Voer direct een belastingcyclus uit (voor testen).")
    public void onSimulate(CommandSender sender) {
        BelastingModule module = OpenMinetopia.getModuleManager().get(BelastingModule.class);
        BelastingConfiguration config = module.getConfig();

        module.getTaxService().runCycleForced().thenAccept(count -> {
            OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                if (count == null || count == 0) {
                    ChatUtils.sendMessage(sender, config.getMessageSimulateNoPlots());
                } else {
                    String msg = config.getMessageSimulateDone().replace("<count>", String.valueOf(count));
                    ChatUtils.sendMessage(sender, msg);
                }
            });
        }).exceptionally(ex -> {
            OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () ->
                    ChatUtils.sendMessage(sender, "<red>Simulatie mislukt: " + ex.getMessage()));
            return null;
        });
    }
}
