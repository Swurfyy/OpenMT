package nl.openminetopia.modules.teleporter.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.police.chase.ChaseManager;
import nl.openminetopia.modules.teleporter.events.PlayerUseTeleporterEvent;
import nl.openminetopia.modules.teleporter.tasks.TeleporterCountdownTask;
import nl.openminetopia.modules.teleporter.utils.TeleporterCooldownManager;
import nl.openminetopia.modules.teleporter.utils.TeleporterTaskManager;
import nl.openminetopia.modules.teleporter.utils.TeleporterUtil;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.events.EventUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleporterInteractListener implements Listener {

    // Track when players last received the chase message to prevent spam
    private final Map<UUID, Long> lastChaseMessageTime = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000; // 3 seconds

    @EventHandler
    public void pressPlate(final PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.PHYSICAL)) return;

        Block block = event.getClickedBlock();
        if (!TeleporterUtil.isTeleporterBlock(block)) return;

        Location location = TeleporterUtil.blockLocation(block);
        if (location == null) return;

        Player player = event.getPlayer();
        
        // Check if player already has an active teleporter task
        TeleporterTaskManager taskManager = TeleporterTaskManager.getInstance();
        if (taskManager.hasActiveTask(player)) {
            // Don't show message if player is already on the same teleporter block
            TeleporterCountdownTask activeTask = taskManager.getActiveTask(player);
            if (activeTask != null && !activeTask.isOnSameTeleporterBlock(block)) {
                Component activeTaskMessage = Component.text("Je hebt al een teleportatie bezig!")
                        .color(NamedTextColor.RED);
                player.sendMessage(activeTaskMessage);
                return;
            }
            // If on same block, just return without message
            return;
        }
        
        // Check if player is already on cooldown
        TeleporterCooldownManager cooldownManager = TeleporterCooldownManager.getInstance();
        if (cooldownManager.isOnCooldown(player)) {
            int remainingSeconds = cooldownManager.getRemainingCooldown(player);
            Component cooldownMessage = Component.text("Je moet nog " + remainingSeconds + " seconden wachten voordat je opnieuw kunt teleporteren!")
                    .color(NamedTextColor.RED);
            player.sendMessage(cooldownMessage);
            return;
        }

        // Check if player is being chased (in "/volg")
        ChaseManager chaseManager = ChaseManager.getInstance();
        if (chaseManager.isBeingChased(player)) {
            // Only show message once every 3 seconds to prevent spam
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            Long lastMessageTime = lastChaseMessageTime.get(playerId);
            
            if (lastMessageTime == null || (currentTime - lastMessageTime) >= MESSAGE_COOLDOWN_MS) {
                player.sendMessage(ChatUtils.color("<red>Je kan de metro niet gebruiken in achtervolgingen!"));
                lastChaseMessageTime.put(playerId, currentTime);
            }
            return;
        }

        PlayerUseTeleporterEvent playerUseTeleporterEvent = new PlayerUseTeleporterEvent(player, location);
        if (EventUtils.callCancellable(playerUseTeleporterEvent)) return;

        // Get cooldown configuration
        int cooldownSeconds = OpenMinetopia.getDefaultConfiguration().getTeleporterCooldownSeconds();
        
        // Start countdown task with teleporter block location for movement tracking
        Location teleporterBlockLocation = block.getLocation();
        TeleporterCountdownTask countdownTask = new TeleporterCountdownTask(player, location, teleporterBlockLocation, cooldownSeconds);
        countdownTask.start();
    }

}