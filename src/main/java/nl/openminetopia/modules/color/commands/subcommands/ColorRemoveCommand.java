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

import java.util.Optional;

@CommandAlias("color")
public class ColorRemoveCommand extends BaseCommand {

    @Subcommand("remove")
    @Syntax("<player> <type> <color>")
    @CommandCompletion("@players @colorTypes @playerColors")
    @CommandPermission("openminetopia.color.remove")
    @Description("Remove a color from a player.")
    public void prefix(Player player, OfflinePlayer offlinePlayer, OwnableColorType type, String draftColor) {
        if (offlinePlayer == null) {
            ChatUtils.sendMessage(player, MessageConfiguration.message("player_not_found"));
            return;
        }

        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            PlayerManager.getInstance().getMinetopiaPlayerAsync(offlinePlayer, targetMinetopiaPlayer -> {
                if (targetMinetopiaPlayer == null) {
                    ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                    return;
                }

                final String colorId = draftColor.toLowerCase();
                if (!OpenMinetopia.getColorsConfiguration().exists(colorId)) {
                    ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_not_found"));
                    return;
                }

                switch (type) {
                    case PREFIX:
                        Optional<PrefixColor> prefixColor = targetMinetopiaPlayer.getColors().stream()
                                .filter(c -> c.getColorId().equals(colorId) && c.getType().equals(type))
                                .map(c -> (PrefixColor) c)
                                .findAny();
                        if (prefixColor.isEmpty()) {
                            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_prefix_not_found"));
                            return;
                        }

                        targetMinetopiaPlayer.removeColor(prefixColor.get());
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_prefix_removed")
                                .replace("<color>", prefixColor.get().getColorId()));
                        break;

                    case CHAT:
                        Optional<ChatColor> chatColor = targetMinetopiaPlayer.getColors().stream()
                                .filter(c -> c.getColorId().equals(colorId) && c.getType().equals(type))
                                .map(c -> (ChatColor) c)
                                .findAny();
                        if (chatColor.isEmpty()) {
                            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_chat_not_found"));
                            return;
                        }

                        targetMinetopiaPlayer.removeColor(chatColor.get());
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_chat_removed")
                                .replace("<color>", chatColor.get().getColorId()));
                        break;

                    case NAME:
                        Optional<NameColor> nameColor = targetMinetopiaPlayer.getColors().stream()
                                .filter(c -> c.getColorId().equals(colorId) && c.getType().equals(type))
                                .map(c -> (NameColor) c)
                                .findAny();
                        if (nameColor.isEmpty()) {
                            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_name_not_found"));
                            return;
                        }

                        targetMinetopiaPlayer.removeColor(nameColor.get());
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_name_removed")
                                .replace("<color>", nameColor.get().getColorId()));
                        break;

                    case LEVEL:
                        Optional<LevelColor> levelColor = targetMinetopiaPlayer.getColors().stream()
                                .filter(c -> c.getColorId().equals(colorId) && c.getType().equals(type))
                                .map(c -> (LevelColor) c)
                                .findAny();
                        if (levelColor.isEmpty()) {
                            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_level_not_found"));
                            return;
                        }

                        targetMinetopiaPlayer.removeColor(levelColor.get());
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("color_level_removed")
                                .replace("<color>", levelColor.get().getColorId()));
                        break;
                }
            }, Throwable::printStackTrace);
        }, Throwable::printStackTrace);
    }
}
