package nl.openminetopia.utils.input;

import io.papermc.paper.event.player.AsyncChatEvent;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputHandler implements Listener {
    private final Map<UUID, Consumer<String>> inputHandlers = new HashMap<>();

    public void waitForInput(Player player, Consumer<String> responseHandler) {
        inputHandlers.put(player.getUniqueId(), responseHandler);
        Bukkit.getScheduler().runTaskLater(OpenMinetopia.getInstance(), () -> {
            if (inputHandlers.containsKey(player.getUniqueId())) {
                inputHandlers.remove(player.getUniqueId());
                ChatUtils.sendMessage(player, "<red>Actie afgebroken, geen invoer ontvangen.");
            }
        }, 20L * 30); // 30 seconds timeout
    }

    @EventHandler
    public void playerChat(final AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (inputHandlers.containsKey(playerId)) {
            event.setCancelled(true);
            Consumer<String> handler = inputHandlers.remove(playerId);
            handler.accept(ChatUtils.rawMiniMessage(event.message()));
        }
    }
}
