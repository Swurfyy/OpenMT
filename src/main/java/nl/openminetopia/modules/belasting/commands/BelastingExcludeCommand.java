package nl.openminetopia.modules.belasting.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.belasting.BelastingModule;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandAlias("belasting")
public class BelastingExcludeCommand extends BaseCommand {

    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)(m|h|d)$", Pattern.CASE_INSENSITIVE);

    @Subcommand("exclude")
    @CommandPermission("openminetopia.belasting.exclude")
    @Description("Sluit een speler tijdelijk uit van belasting.")
    @Syntax("<username> <tijd>")
    public void onExclude(CommandSender sender, String username, String timeInput) {
        BelastingModule module = OpenMinetopia.getModuleManager().get(BelastingModule.class);
        BelastingConfiguration config = module.getConfig();

        long durationMs = parseTime(timeInput);
        if (durationMs <= 0) {
            ChatUtils.sendMessage(sender, config.getMessageTimeFormatInvalid());
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(username);
        if (offlinePlayer == null) offlinePlayer = Bukkit.getOfflinePlayer(username);
        if (offlinePlayer.getUniqueId() == null || (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline())) {
            ChatUtils.sendMessage(sender, config.getMessagePlayerNotFound());
            return;
        }
        UUID targetUuid = offlinePlayer.getUniqueId();
        String targetName = offlinePlayer.getName() != null ? offlinePlayer.getName() : targetUuid.toString();
        long expiresAt = System.currentTimeMillis() + durationMs;

        module.getTaxService().addExclusion(targetUuid, expiresAt).thenRun(() -> {
            OpenMinetopia.getInstance().getServer().getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                String name = targetName;
                String duration = formatDuration(durationMs);
                String msg = config.getMessageExcludeAdded()
                        .replace("<player>", name)
                        .replace("<duration>", duration);
                ChatUtils.sendMessage(sender, msg);
            });
        });
    }

    private static long parseTime(String input) {
        if (input == null || input.isBlank()) return 0;
        Matcher m = TIME_PATTERN.matcher(input.trim());
        if (!m.matches()) return 0;
        long value = Long.parseLong(m.group(1));
        String unit = m.group(2).toLowerCase();
        return switch (unit) {
            case "m" -> value * 60 * 1000L;
            case "h" -> value * 60 * 60 * 1000L;
            case "d" -> value * 24 * 60 * 60 * 1000L;
            default -> 0;
        };
    }

    private static String formatDuration(long ms) {
        long seconds = ms / 1000;
        if (seconds < 60) return seconds + " seconden";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minuten";
        long hours = minutes / 60;
        if (hours < 24) return hours + " uur";
        long days = hours / 24;
        return days + " dagen";
    }
}
