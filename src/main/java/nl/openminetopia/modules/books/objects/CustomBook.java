package nl.openminetopia.modules.books.objects;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.WrittenBookContent;
import lombok.AllArgsConstructor;
import lombok.Data;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

@Data
@AllArgsConstructor
public class CustomBook {

    private final String identifier;
    private final boolean copy;
    private final Material menuItem;
    private final String name;
    private String itemName;
    private final String description;

    /**
     * A map of variables used in the book.
     * <p>
     * The key represents the variable name, and the value is the explanation of the variable that is sent to the player.
     */
    private final Map<String, String> variables;
    private String content;

    public ItemStack getBookItem(Map<String, String> variables, Player player, boolean copy) {
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        if (minetopiaPlayer == null) return null;

        // Vervang de variabelen in de content
        String processedContent = content;
        String processedName = itemName;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            processedContent = processedContent.replace(entry.getKey(), entry.getValue());
            processedName = processedName.replace(entry.getKey(), entry.getValue());
        }

        ItemStack book = ItemStack.of(Material.WRITTEN_BOOK);
        WrittenBookContent.Builder builder = WrittenBookContent.writtenBookContent(processedName, player.getName());

        builder.addPage(ChatUtils.format(minetopiaPlayer, processedContent));

        if (copy) builder.generation(1);
        else builder.generation(0);

        book.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT, builder.build());

        String suffix = copy ? "<reset><white>[COPY] <reset>" : "";
        String bookTitle = processedName.length() > 32 ? suffix + processedName.substring(0, 32) : suffix + processedName;
        return new ItemBuilder(book).setName(ChatUtils.format(minetopiaPlayer, bookTitle)).toItemStack();
    }
}