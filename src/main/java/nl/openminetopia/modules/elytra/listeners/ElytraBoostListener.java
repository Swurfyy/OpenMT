package nl.openminetopia.modules.elytra.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.elytra.ElytraModule;
import nl.openminetopia.modules.elytra.configuration.ElytraConfiguration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ElytraBoostListener implements Listener {

    private final Map<UUID, Long> lastGlideToggle = new HashMap<>();
    private final Map<UUID, Vector> lastVelocity = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();
    private final Map<UUID, Integer> groundSpamCount = new HashMap<>();
    private final Map<UUID, Location> lastGroundLocation = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        ElytraModule elytraModule = OpenMinetopia.getModuleManager().get(ElytraModule.class);
        if (elytraModule == null) return;

        ElytraConfiguration config = elytraModule.getConfiguration();
        if (!config.isEnabled()) return;

        if (elytraModule.isPlayerBypassed(player.getUniqueId())) return;

        UUID playerId = player.getUniqueId();
        Location currentLoc = player.getLocation();
        long currentTime = System.currentTimeMillis();

        // Track ground contact for boost detection
        if (player.getLocation().getBlock().getRelative(0, -1, 0).getType().isSolid()) {
            lastGroundLocation.put(playerId, currentLoc.clone());
            groundSpamCount.put(playerId, 0);
        }

        // Only apply restrictions when gliding
        if (player.isGliding()) {
            Vector currentVelocity = player.getVelocity();
            
            // Fix: Prevent glitched flying near ground
            if (player.getLocation().getBlock().getRelative(0, -1, 0).getType().isSolid() && 
                currentVelocity.getY() < 0) {
                // Force player to stop gliding when hitting ground
                player.setGliding(false);
                return;
            }

            // Fix: Better speed limiting
            double totalSpeed = currentVelocity.length();
            double maxSpeed = Math.max(config.getMaxVerticalSpeed(), config.getMaxHorizontalSpeed());
            
            if (totalSpeed > maxSpeed) {
                Vector newVelocity = currentVelocity.clone();
                double scale = maxSpeed / totalSpeed;
                newVelocity.multiply(scale);
                player.setVelocity(newVelocity);
            }

            // Additional check for excessive vertical speed
            if (Math.abs(currentVelocity.getY()) > config.getMaxVerticalSpeed()) {
                Vector newVelocity = currentVelocity.clone();
                newVelocity.setY(Math.signum(newVelocity.getY()) * config.getMaxVerticalSpeed());
                player.setVelocity(newVelocity);
            }

            lastVelocity.put(playerId, currentVelocity.clone());
        }
        
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

        // Prevent rapid toggling of elytra (spam deploying) - only block if trying to boost
        if (event.isGliding()) {
            // Only prevent if player is trying to boost (recent ground contact + rapid toggle)
            Long lastToggle = lastGlideToggle.get(playerId);
            boolean recentGroundContact = lastGroundLocation.containsKey(playerId) && 
                (currentTime - lastMoveTime.getOrDefault(playerId, 0L)) < 500; // 0.5 seconds
            
            if (lastToggle != null && currentTime - lastToggle < 500 && recentGroundContact) {
                // This looks like boosting - prevent it
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        ElytraModule elytraModule = OpenMinetopia.getModuleManager().get(ElytraModule.class);
        if (elytraModule == null) return;

        ElytraConfiguration config = elytraModule.getConfiguration();
        if (!config.isEnabled()) return;

        if (elytraModule.isPlayerBypassed(player.getUniqueId())) return;

        // Fix: Force stop gliding when sneaking near ground
        if (player.isGliding() && event.isSneaking()) {
            Location loc = player.getLocation();
            if (loc.getBlock().getRelative(0, -1, 0).getType().isSolid() || 
                loc.getBlock().getRelative(0, -2, 0).getType().isSolid()) {
                player.setGliding(false);
            }
        }
    }
}
