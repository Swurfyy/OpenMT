package nl.openminetopia.modules.color.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.color.enums.OwnableColorType;
import nl.openminetopia.modules.color.objects.ChatColor;
import nl.openminetopia.modules.color.objects.LevelColor;
import nl.openminetopia.modules.color.objects.NameColor;
import nl.openminetopia.modules.color.objects.PrefixColor;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("color")
public class ColorAddCommand extends BaseCommand {

    @Subcommand("add")
    @Syntax("<player> <type> <color>")
    @CommandCompletion("@players @colorTypes @colorIds")
    @CommandPermission("openminetopia.color.add")
    @Description("Add a color to a player.")
    public void prefix(Player player, OfflinePlayer offlinePlayer, OwnableColorType type, String draftColor) {

        if (offlinePlayer == null) {
            ChatUtils.sendMessage(player, MessageConfiguration.message("player_not_found"));
            return;
        }

        final String colorId = draftColor.toLowerCase();
        if (!OpenMinetopia.getColorsConfiguration().exists(colorId)) {
            ChatUtils.sendMessage(player, MessageConfiguration.message("color_not_found"));
            return;
        }

        PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete((minetopiaPlayer, throwable) -> {
            if (minetopiaPlayer == null) return;
            PlayerManager.getInstance().getMinetopiaPlayer(offlinePlayer).whenComplete((targetMinetopiaPlayer, throwable1) -> {
                if (targetMinetopiaPlayer == null) {
                    ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                    return;
                }

                switch (type) {
                    case PREFIX:
                        if (targetMinetopiaPlayer.getColors().stream().anyMatch(prefixColor -> prefixColor.getColorId().equalsIgnoreCase(colorId) && prefixColor.getType() == type)) {
                            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_prefix_exists"));
                            return;
                        }

                        PrefixColor prefixColor = new PrefixColor(colorId, -1L);
                        targetMinetopiaPlayer.addColor(prefixColor);
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_prefix_added")
                                .replace("<color>", prefixColor.getColorId()));
                        break;

                    case CHAT:
                        if (targetMinetopiaPlayer.getColors().stream().anyMatch(chatColor -> chatColor.getColorId().equalsIgnoreCase(colorId) && chatColor.getType() == type)) {
                            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_chat_exists"));
                            return;
                        }

                        ChatColor chatColor = new ChatColor(colorId, -1L);
                        targetMinetopiaPlayer.addColor(chatColor);
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_chat_added")
                                .replace("<color>", chatColor.getColorId()));
                        break;
                    case NAME:
                        if (targetMinetopiaPlayer.getColors().stream().anyMatch(nameColor -> nameColor.getColorId().equalsIgnoreCase(colorId) && nameColor.getType() == type)) {
                            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_name_exists"));
                            return;
                        }

                        NameColor nameColor = new NameColor(colorId, -1L);
                        targetMinetopiaPlayer.addColor(nameColor);
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_name_added")
                                .replace("<color>", nameColor.getColorId()));
                        break;
                    case LEVEL:
                        if (targetMinetopiaPlayer.getColors().stream().anyMatch(levelColor -> levelColor.getColorId().equalsIgnoreCase(colorId) && levelColor.getType() == type)) {
                            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_level_exists"));
                            return;
                        }

                        LevelColor levelColor = new LevelColor(colorId, -1L);
                        targetMinetopiaPlayer.addColor(levelColor);
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_level_added")
                                .replace("<color>", levelColor.getColorId()));

                        break;
                }
            });
        });
    }
}
