package nl.openminetopia.modules.misc.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class PvPItem {
    private final ItemStack item;
    private final String attackerMessage;
    private final String victimMessage;

    public String attackerMessage() {
        return attackerMessage.replace("<item>", item.getType().name());
    }

    public String victimMessage() {
        return victimMessage.replace("<item>", item.getType().name());
    }
}
