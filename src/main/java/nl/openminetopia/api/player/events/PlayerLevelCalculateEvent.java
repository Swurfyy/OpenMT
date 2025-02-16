package nl.openminetopia.api.player.events;

import lombok.Getter;
import lombok.Setter;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerLevelCalculateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    @Setter
    private boolean cancelled = false;

    private final MinetopiaPlayer player;

    @Setter
    private int calculatedLevel;

    private final int points;

    public PlayerLevelCalculateEvent(MinetopiaPlayer player, int calculatedLevel, int points) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.player = player;
        this.calculatedLevel = calculatedLevel;
        this.points = points;
    }

    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public HandlerList getHandlerList() {
        return handlers;
    }

    public boolean setPoints(MinetopiaPlayer target, int points) {
        PlayerLevelCalculateEvent event = new PlayerLevelCalculateEvent(target, this.calculatedLevel, points);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    public boolean setCalculatedLevel(MinetopiaPlayer target, int calculatedLevel) {
        PlayerLevelCalculateEvent event = new PlayerLevelCalculateEvent(target, calculatedLevel, this.points);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
