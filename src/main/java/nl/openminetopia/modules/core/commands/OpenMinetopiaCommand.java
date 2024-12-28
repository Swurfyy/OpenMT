package nl.openminetopia.modules.core.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.RootCommand;
import co.aikar.commands.annotation.*;
import lombok.SneakyThrows;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.configuration.*;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.List;

@CommandAlias("openminetopia|sdb|minetopia|omt")
public class OpenMinetopiaCommand extends BaseCommand {

    @Subcommand("reload")
    @SneakyThrows
    @CommandPermission("openminetopia.reload")
    public void reload(Player player) {
        File dataFolder = OpenMinetopia.getInstance().getDataFolder();

        OpenMinetopia.setDefaultConfiguration(new DefaultConfiguration(dataFolder));
        OpenMinetopia.getDefaultConfiguration().saveConfiguration();

        OpenMinetopia.setMessageConfiguration(new MessageConfiguration(dataFolder));
        OpenMinetopia.getMessageConfiguration().saveConfiguration();

        OpenMinetopia.setLevelcheckConfiguration(new LevelCheckConfiguration(dataFolder));
        OpenMinetopia.getLevelcheckConfiguration().saveConfiguration();

        OpenMinetopia.setColorsConfiguration(new ColorsConfiguration(dataFolder));
        OpenMinetopia.getColorsConfiguration().saveConfiguration();

        OpenMinetopia.setFitnessConfiguration(new FitnessConfiguration(dataFolder));
        OpenMinetopia.getFitnessConfiguration().saveConfiguration();

        OpenMinetopia.setBankingConfiguration(new BankingConfiguration(dataFolder));
        OpenMinetopia.getBankingConfiguration().saveConfiguration();

        player.sendMessage(ChatUtils.color("<gold>De configuratiebestanden zijn succesvol herladen!"));
    }

    @Default
    public void onCommand(Player player) {
        player.sendMessage(ChatUtils.color(" "));
        player.sendMessage(ChatUtils.color("<gold>Deze server maakt gebruik van <yellow>OpenMinetopia <gold>versie <yellow>" + OpenMinetopia.getInstance().getDescription().getVersion()));
        player.sendMessage(ChatUtils.color("<gold>Auteurs: <yellow>" + OpenMinetopia.getInstance().getDescription().getAuthors().toString().replace("[", "").replace("]", "")));
        player.sendMessage(ChatUtils.color(" "));
    }

    @Subcommand("help")
    @CommandPermission("openminetopia.help")
    @Description("Laat alle commando's zien die beschikbaar zijn in OpenMinetopia")
    public void help(CommandSender sender, @Optional Integer page) {
        List<RootCommand> rootCommands = OpenMinetopia.getCommandManager().getRegisteredRootCommands().stream().toList();

        // paginated help
        int pageSize = 10;
        int pages = (int) Math.ceil(rootCommands.size() / (double) pageSize);
        int currentPage = page == null ? 1 : page;

        ChatUtils.sendMessage(sender, " ");
        ChatUtils.sendMessage(sender, "<gold>OpenMinetopia commando's" + " <gray>(<yellow>" + currentPage + "<gray>/<yellow>" + pages +  "<gray>)<gold>:");

        if (currentPage < 1 || currentPage > pages) {
            ChatUtils.sendMessage(sender, "<red>Deze pagina bestaat niet.");
            return;
        }

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, rootCommands.size());

        for (int i = start; i < end; i++) {
            RootCommand command = rootCommands.get(i);
            ChatUtils.sendMessage(sender, "<gray>/<yellow><click:suggest_command:'/" + command.getCommandName() + "'>" + command.getCommandName() + " <gray>- " + (command.getDescription().isEmpty() ? "Geen beschrijving" : command.getDescription()) + "</click>");
        }

        ChatUtils.sendMessage(sender, " ");
        ChatUtils.sendMessage(sender, "<gold>Gebruik <yellow>/openminetopia help <pagina> <gold>om naar een andere pagina te gaan.");
    }
}
