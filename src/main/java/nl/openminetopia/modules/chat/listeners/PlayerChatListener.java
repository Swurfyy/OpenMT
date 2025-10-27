package nl.openminetopia.modules.chat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.DefaultConfiguration;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.chat.utils.SpyUtils;
import nl.openminetopia.modules.police.PoliceModule;
import nl.openminetopia.modules.police.utils.BalaclavaUtils;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class PlayerChatListener implements Listener {

    @EventHandler
    public void playerChat(AsyncChatEvent event) {
        if (event.isCancelled()) return;

        Player source = event.getPlayer();
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(source);

        if (minetopiaPlayer == null) return;

        DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();

        String originalMessage = ChatUtils.rawMiniMessage(event.message());
        SpyUtils.chatSpy(source, originalMessage, new ArrayList<>());

        if (!configuration.isChatEnabled()) return;
        if (!minetopiaPlayer.isInPlace()) return;
        if (minetopiaPlayer.isStaffchatEnabled()) return;

        PoliceModule policeModule = OpenMinetopia.getModuleManager().get(PoliceModule.class);
        if (policeModule.getWalkieTalkieManager().isPoliceChatEnabled(source)) return;

        event.setCancelled(true);

        List<Player> recipients = new ArrayList<>();

        if (configuration.isChatRadiusEnabled()) {
            Bukkit.getServer().getOnlinePlayers().forEach(target -> {
                if (target.getWorld().equals(source.getWorld())
                        && source.getLocation().distance(target.getLocation()) <= configuration.getChatRadiusRange())
                    recipients.add(target);
            });

            recipients.remove(source);
            if (recipients.isEmpty() && configuration.isNotifyWhenNobodyInRange()) {
                event.getPlayer().sendMessage(MessageConfiguration.component("chat_no_players_in_range"));
                return;
            }
            recipients.add(source);
        } else {
            recipients.addAll(Bukkit.getOnlinePlayers());
        }

        // Format the message
        String formattedMessage = configuration.getChatFormat();

        // Check if the player is wearing a balaclava and replace placeholders with default values
        if (BalaclavaUtils.isBalaclavaItem(source.getInventory().getHelmet())) {
            formattedMessage = formattedMessage
                    .replace("<level>", configuration.getDefaultLevel() + "")
                    .replace("<prefix>", configuration.getDefaultPrefix())
                    .replace("<name_color>", configuration.getDefaultNameColor())
                    .replace("<level_color>", configuration.getDefaultLevelColor())
                    .replace("<prefix_color>", configuration.getDefaultPrefixColor())
                    .replace("<chat_color>", configuration.getDefaultChatColor());
        }

        // First, replace the message placeholder with the actual message
        Component baseComponent = ChatUtils.format(minetopiaPlayer, formattedMessage);
        Component formattedComponent = baseComponent.replaceText(
                builder -> builder.matchLiteral("<message>").replacement(event.message())
        );
        Bukkit.getConsoleSender().sendMessage(formattedComponent); // Log the message without potential scrambled name

        for (Player target : recipients) {
            Component finalMessage = formattedComponent;
            if (originalMessage.contains(target.getName())) {
                // Only highlight the player name within the message part, not in the display name
                // We do this by applying the replacement only on the message component
                final Component originalMessageComponent = event.message();
                
                // Check if the target's name appears in the message
                final boolean hasNameInMessage = ChatUtils.stripMiniMessage(originalMessageComponent).contains(target.getName());
                
                // Highlight the target's name in the message if present
                final Component messageComponent;
                if (hasNameInMessage) {
                    messageComponent = originalMessageComponent.replaceText(builder -> {
                        builder.match(target.getName())
                               .replacement(component -> ChatUtils.color("<green>" + target.getName() + minetopiaPlayer.getActiveChatColor().color()));
                    });
                } else {
                    messageComponent = originalMessageComponent;
                }
                
                // Rebuild the full message with the highlighted name
                finalMessage = baseComponent.replaceText(
                        builder -> builder.matchLiteral("<message>").replacement(messageComponent)
                );
                target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }

            target.sendMessage(finalMessage);
        }
    }
}