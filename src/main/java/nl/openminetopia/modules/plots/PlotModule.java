package nl.openminetopia.modules.plots;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.plots.commands.PlotCommand;
import nl.openminetopia.modules.plots.commands.subcommands.PlotClearCommand;
import nl.openminetopia.modules.plots.commands.subcommands.PlotCreateCommand;
import nl.openminetopia.modules.plots.commands.subcommands.PlotDeleteCommand;
import nl.openminetopia.modules.plots.commands.subcommands.PlotDescriptionCommand;
import nl.openminetopia.modules.plots.commands.subcommands.PlotInfoCommand;
import nl.openminetopia.modules.plots.commands.subcommands.PlotListCommand;
import nl.openminetopia.modules.plots.commands.subcommands.PlotMembersCommand;
import nl.openminetopia.modules.plots.commands.subcommands.PlotOwnersCommand;
import nl.openminetopia.modules.plots.commands.subcommands.PlotTeleportCommand;
import nl.openminetopia.utils.WorldGuardUtils;

public class PlotModule extends Module {

    @Override
    public void enable() {
        registerCommand(new PlotInfoCommand());

        registerCommand(new PlotCommand());
        registerCommand(new PlotMembersCommand());
        registerCommand(new PlotOwnersCommand());
        registerCommand(new PlotClearCommand());
        registerCommand(new PlotCreateCommand());
        registerCommand(new PlotDeleteCommand());
        registerCommand(new PlotDescriptionCommand());
        registerCommand(new PlotListCommand());
        registerCommand(new PlotTeleportCommand());

        OpenMinetopia.getCommandManager().getCommandCompletions().registerCompletion("plotName", context ->
                WorldGuardUtils.getProtectedRegions(priority -> priority >= 0).stream()
                        .filter(region -> region.getFlag(OpenMinetopia.PLOT_FLAG) != null)
                        .map(ProtectedRegion::getId).toList());
    }

    @Override
    public void disable() {

    }


}
