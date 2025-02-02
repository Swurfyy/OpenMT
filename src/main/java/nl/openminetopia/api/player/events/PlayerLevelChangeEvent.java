package nl.openminetopia.api.player.events;

import lombok.Getter;
import lombok.Setter;
import nl.openminetopia.api.enums.LevelChangeReason;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerLevelChangeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    @Setter
    private boolean cancelled = false;

    private final MinetopiaPlayer player;

    private final LevelChangeReason reason;

    private final int oldLevel;

    private final int newLevel;

    public PlayerLevelChangeEvent(MinetopiaPlayer player, LevelChangeReason reason, int oldLevel, int newLevel) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.player = player;
        this.reason = reason;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public HandlerList getHandlerList() {
        return handlers;
    }

    public boolean setNewLevel(MinetopiaPlayer target, int newLevel, LevelChangeReason changeReason) {
        PlayerLevelChangeEvent event = new PlayerLevelChangeEvent(target, changeReason, target.getLevel(), newLevel);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
