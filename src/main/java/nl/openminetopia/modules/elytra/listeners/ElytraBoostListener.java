package nl.openminetopia.modules.elytra.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.elytra.ElytraModule;
import nl.openminetopia.modules.elytra.configuration.ElytraConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ElytraBoostListener implements Listener {

    private final Map<UUID, Long> lastGlideToggle = new HashMap<>();
    private final Map<UUID, Long> lastGroundTouch = new HashMap<>();
    private final Map<UUID, Vector> lastVelocity = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();
    private final Map<UUID, Integer> groundSpamCount = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isGliding()) return;

        ElytraModule elytraModule = OpenMinetopia.getModuleManager().get(ElytraModule.class);
        if (elytraModule == null) return;

        ElytraConfiguration config = elytraModule.getConfiguration();
        if (!config.isEnabled()) return;

        if (elytraModule.isPlayerBypassed(player.getUniqueId())) return;

        UUID playerId = player.getUniqueId();
        Vector currentVelocity = player.getVelocity();

        long currentTime = System.currentTimeMillis();

        // Detect ground spam (constant jumping to deploy elytra)
        if (player.getLocation().getBlock().getRelative(0, -1, 0).getType().isSolid()) {
            lastGroundTouch.put(playerId, currentTime);
            if (config.isPreventGroundSpam()) {
                int spamCount = groundSpamCount.getOrDefault(playerId, 0);
                if (spamCount > 3) { // Allow some jumping but prevent spam
                    event.setCancelled(true);
                    return;
                }
                groundSpamCount.put(playerId, spamCount + 1);
            }
        } else {
            // Reset spam count when not on ground
            groundSpamCount.put(playerId, 0);
        }

        // Detect excessive vertical speed
        if (config.isPreventConstantFlying()) {
            double verticalSpeed = Math.abs(currentVelocity.getY());
            if (verticalSpeed > config.getMaxVerticalSpeed()) {
                // Only cancel if the player is gaining altitude without proper reason
                if (currentVelocity.getY() > 0 && !player.getLocation().getBlock().getRelative(0, -1, 0).getType().isSolid()) {
                    Vector newVelocity = currentVelocity.clone();
                    newVelocity.setY(Math.min(newVelocity.getY(), config.getMaxVerticalSpeed()));
                    player.setVelocity(newVelocity);
                }
            }
        }

        // Detect excessive horizontal speed
        double horizontalSpeed = Math.sqrt(currentVelocity.getX() * currentVelocity.getX() + 
                                         currentVelocity.getZ() * currentVelocity.getZ());
        if (horizontalSpeed > config.getMaxHorizontalSpeed()) {
            Vector newVelocity = currentVelocity.clone();
            double scale = config.getMaxHorizontalSpeed() / horizontalSpeed;
            newVelocity.setX(newVelocity.getX() * scale);
            newVelocity.setZ(newVelocity.getZ() * scale);
            player.setVelocity(newVelocity);
        }

        lastVelocity.put(playerId, currentVelocity.clone());
        lastMoveTime.put(playerId, currentTime);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ElytraModule elytraModule = OpenMinetopia.getModuleManager().get(ElytraModule.class);
        if (elytraModule == null) return;

        ElytraConfiguration config = elytraModule.getConfiguration();
        if (!config.isEnabled()) return;

        if (elytraModule.isPlayerBypassed(player.getUniqueId())) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Prevent rapid toggling of elytra (spam deploying)
        if (event.isGliding()) {
            Long lastToggle = lastGlideToggle.get(playerId);
            if (lastToggle != null && currentTime - lastToggle < 1000) { // 1 second cooldown
                event.setCancelled(true);
                return;
            }
            lastGlideToggle.put(playerId, currentTime);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (event.getItem().getType() != Material.FIREWORK_ROCKET) return;

        Player player = event.getPlayer();
        if (!player.isGliding()) return;

        ElytraModule elytraModule = OpenMinetopia.getModuleManager().get(ElytraModule.class);
        if (elytraModule == null) return;

        ElytraConfiguration config = elytraModule.getConfiguration();
        if (!config.isEnabled()) return;

        if (elytraModule.isPlayerBypassed(player.getUniqueId())) return;

        // Prevent firework boosting
        if (config.isPreventFireworkBoost()) {
            event.setCancelled(true);
            player.sendMessage("§cFirework boosting is disabled!");
        }
    }
}
