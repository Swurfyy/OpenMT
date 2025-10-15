package nl.openminetopia.modules.misc.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.places.MTPlaceManager;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.misc.objects.PvPItem;
import nl.openminetopia.modules.misc.utils.MiscUtils;
import nl.openminetopia.modules.police.handcuff.HandcuffManager;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ProjectileHitListener implements Listener {

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        // Only handle snowballs
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        
        // Check if the hit entity is a player
        if (!(event.getHitEntity() instanceof Player target)) return;
        
        // Check if the shooter is a player
        if (!(snowball.getShooter() instanceof Player attacker)) return;
        
        // Check if PvP is enabled
        if (!OpenMinetopia.getDefaultConfiguration().isPvpEnabled()) return;
        
        // Check if the location is valid for PvP
        if (MTPlaceManager.getInstance().getPlace(target.getLocation()) == null) return;
        
        // Check if attacker is handcuffed
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(attacker);
        if (HandcuffManager.getInstance().isHandcuffed(attacker) && !OpenMinetopia.getDefaultConfiguration().isHandcuffCanPvP()) {
            attacker.sendMessage(ChatUtils.format(minetopiaPlayer, MessageConfiguration.message("police_handcuff_cant_pvp")
                    .replace("<victim>", target.getName())
                    .replace("<attacker>", attacker.getName())
            ));
            return;
        }
        
        // Get the PvP item config for snowballs
        PvPItem pvpItem = MiscUtils.getPvPItem(new ItemStack(Material.SNOWBALL));
        if (pvpItem == null) return;
        
        // Send messages to attacker and target
        sendAttackedMessages(attacker, target, pvpItem, minetopiaPlayer);
        
        // Apply slowness effect
        applySlownessEffect(target, pvpItem);
    }
    
    private void sendAttackedMessages(Player attacker, Player target, PvPItem pvpItem, MinetopiaPlayer minetopiaPlayer) {
        MinetopiaPlayer targetMinetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(target);

        attacker.sendMessage(ChatUtils.format(minetopiaPlayer, pvpItem.attackerMessage()
                .replace("<attacker>", attacker.getName()).replace("<victim>", target.getName())));
        target.sendMessage(ChatUtils.format(targetMinetopiaPlayer, pvpItem.victimMessage()
                .replace("<attacker>", attacker.getName()).replace("<victim>", target.getName())));
    }

    private void applySlownessEffect(Player target, PvPItem pvpItem) {
        if (pvpItem.slownessConfig() == null || !pvpItem.slownessConfig().enabled()) {
            return;
        }

        int duration = pvpItem.slownessConfig().duration();
        int amplifier = pvpItem.slownessConfig().amplifier();
        
        PotionEffect slownessEffect = new PotionEffect(
            PotionEffectType.SLOWNESS, 
            duration, 
            amplifier, 
            false, 
            true, 
            true
        );
        
        target.addPotionEffect(slownessEffect);
    }
}

