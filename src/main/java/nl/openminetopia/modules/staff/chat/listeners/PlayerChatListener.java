package nl.openminetopia.modules.staff.chat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.police.PoliceModule;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class PlayerChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player source = event.getPlayer();

        PoliceModule policeModule = OpenMinetopia.getModuleManager().getModule(PoliceModule.class);
        if (policeModule.getWalkieTalkieManager().isComposingMessage(source)
        || policeModule.getWalkieTalkieManager().isPoliceChatEnabled(source)) return;

        PlayerManager.getInstance().getMinetopiaPlayerAsync(source, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (!source.hasPermission("openminetopia.staffchat") || !minetopiaPlayer.isStaffchatEnabled()) return;

            event.setCancelled(true);

            List<Player> recipients = new ArrayList<>();

            Bukkit.getServer().getOnlinePlayers().forEach(target -> {
                if (target.hasPermission("openminetopia.staffchat")) recipients.add(target);
            });

            String formattedMessage = MessageConfiguration.message("staff_chat_format")
                    .replace("<player>", source.getName())
                    .replace("<world_name>", source.getWorld().getName())
                    .replace("<message>", ChatUtils.stripMiniMessage(event.message()));

            // Iterate over recipients
            recipients.forEach(player -> {
                // Send the formatted message to the player
                ChatUtils.sendFormattedMessage(minetopiaPlayer, formattedMessage);
            });
            Bukkit.getConsoleSender().sendMessage(ChatUtils.format(minetopiaPlayer, formattedMessage));
        }, Throwable::printStackTrace);
    }
}
