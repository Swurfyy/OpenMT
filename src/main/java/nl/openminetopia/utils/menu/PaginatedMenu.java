package nl.openminetopia.utils.menu;

import dev.triumphteam.gui.builder.item.PaperItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PaginatedMenu {

    public final PaginatedGui gui;

    public PaginatedMenu(String title, int rows, int pageSize) {
        this.gui = Gui.paginated()
                .title(ChatUtils.color(title))
                .rows(rows)
                .pageSize(pageSize)
                .create();
    }

    public GuiItem nextPageItem() {
        return PaperItemBuilder.from(Material.ARROW).name(ChatUtils.color("<gold>Volgende pagina")).asGuiItem(
                event -> gui.next()
        );
    }

    public GuiItem previousPageItem() {
        return PaperItemBuilder.from(Material.ARROW).name(ChatUtils.color("<gold>Vorige pagina")).asGuiItem(
                event -> gui.previous()
        );
    }

    public PaginatedMenu(String title, int rows) {
        this.gui = Gui.paginated()
                .title(ChatUtils.color(title))
                .rows(rows)
                .create();
    }

    public void open(Player player) {
        gui.open(player);
    }
}
