package nl.openminetopia.modules.police.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.police.models.CriminalRecordModel;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

@CommandAlias("criminalrecord|strafblad")
public class CriminalRecordCommand extends BaseCommand {

    @HelpCommand
    public void onHelp(CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("add")
    @Description("Add a criminal record to a player")
    @Syntax("<player> <description>")
    @CommandCompletion("@players")
    @CommandPermission("openminetopia.criminalrecord.add")
    public void add(Player player, OfflinePlayer target, String description) {
        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (target == null) {
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                return;
            }

            PlayerManager.getInstance().getMinetopiaPlayerAsync(target, targetMinetopiaPlayer -> {
                if (targetMinetopiaPlayer == null) return;

                targetMinetopiaPlayer.addCriminalRecord(description, player.getUniqueId(), System.currentTimeMillis());
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_criminal_record_added")
                        .replace("<description>", description));
            }, Throwable::printStackTrace);
        }, Throwable::printStackTrace);
    }

    @Subcommand("remove")
    @Description("Remove a criminal record from a player")
    @Syntax("<player> <id>")
    @CommandCompletion("@players")
    @CommandPermission("openminetopia.criminalrecord.remove")
    public void remove(Player player, OfflinePlayer target, int id) {

        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (target == null) {
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                return;
            }

            PlayerManager.getInstance().getMinetopiaPlayerAsync(target, targetMinetopiaPlayer -> {
                if (targetMinetopiaPlayer == null) {
                    ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                    return;
                }

                for (CriminalRecordModel criminalRecord : targetMinetopiaPlayer.getCriminalRecords()) {
                    if (criminalRecord.getId() == id) {
                        targetMinetopiaPlayer.removeCriminalRecord(criminalRecord);
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_criminal_record_removed")
                                .replace("<description>", criminalRecord.getDescription())
                                .replace("<id>", String.valueOf(id)));
                        return;
                    }
                }

                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_criminal_record_not_found"));
            }, Throwable::printStackTrace);
        }, Throwable::printStackTrace);
    }

    @Subcommand("info")
    @Description("View the criminal records of a player")
    @Syntax("<player>")
    @CommandCompletion("@players")
    @CommandPermission("openminetopia.criminalrecord.info")
    public void info(Player player, OfflinePlayer target) {

        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (target == null) {
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                return;
            }

            PlayerManager.getInstance().getMinetopiaPlayerAsync(target, targetMinetopiaPlayer -> {
                if (targetMinetopiaPlayer == null) {
                    ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                    return;
                }

                if (targetMinetopiaPlayer.getCriminalRecords().isEmpty()) {
                    ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_criminal_record_none")
                            .replace("<player>", (target.getName() == null ? "Onbekend" : target.getName())));
                    return;
                }

                targetMinetopiaPlayer.getCriminalRecords().forEach(criminalRecord -> {

                    OfflinePlayer officer = Bukkit.getOfflinePlayer(criminalRecord.getOfficerId());

                    ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_criminal_record_info_entry")
                            .replace("<id>", String.valueOf(criminalRecord.getId()))
                            .replace("<description>", criminalRecord.getDescription())
                            .replace("<officer>", (officer.getName() == null ? "Onbekend" : officer.getName()))
                            .replace("<date>", formatDate(criminalRecord.getDate())));
                });
            }, Throwable::printStackTrace);
        }, Throwable::printStackTrace);
    }

    private String formatDate(long date) {
        return new SimpleDateFormat(MessageConfiguration.message("police_criminal_record_date_format")).format(new Date(date));
    }
}
