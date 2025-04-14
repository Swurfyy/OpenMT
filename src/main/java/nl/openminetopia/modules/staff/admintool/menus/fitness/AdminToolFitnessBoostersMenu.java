package nl.openminetopia.modules.staff.admintool.menus.fitness;

import com.jazzkuh.inventorylib.objects.PaginatedMenu;
import com.jazzkuh.inventorylib.objects.icon.Icon;
import lombok.Getter;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.modules.fitness.models.FitnessBoosterModel;
import nl.openminetopia.modules.player.utils.PlaytimeUtil;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Getter
public class AdminToolFitnessBoostersMenu extends PaginatedMenu {

    private final Player player;
    private final OfflinePlayer offlinePlayer;
    private final MinetopiaPlayer minetopiaPlayer;
    private final BankAccountModel bankAccountModel;

    public AdminToolFitnessBoostersMenu(Player player, OfflinePlayer offlinePlayer, MinetopiaPlayer minetopiaPlayer, BankAccountModel bankAccountModel) {
        super(ChatUtils.color("<gold>Fitheid Boosters <yellow>" + offlinePlayer.getPlayerProfile().getName()), 3);
        this.player = player;
        this.offlinePlayer = offlinePlayer;
        this.minetopiaPlayer = minetopiaPlayer;
        this.bankAccountModel = bankAccountModel;

        this.registerPageSlotsBetween(0, 17);

        if (minetopiaPlayer == null) return;

        for (FitnessBoosterModel booster : minetopiaPlayer.getFitness().getBoosters()) {
            ItemBuilder icon = new ItemBuilder(Material.POTION)
                    .setName("<gold>Booster")
                    .addLoreLine(" ")
                    .addLoreLine("<gold>Boost: <yellow>" + booster.getAmount());

            String duration = PlaytimeUtil.formatPlaytime((booster.getExpiresAt() - System.currentTimeMillis()) / 1000);
            if (booster.getExpiresAt() != -1 && booster.getExpiresAt() - System.currentTimeMillis() > -1) icon.addLoreLine("<gold>Deze booster vervalt over <yellow>" + duration + "<gold>.");
            if (booster.isExpired()) icon.addLoreLine("<gold>Deze booster is <red>verlopen<gold>.");
            if (booster.getExpiresAt() == -1) icon.addLoreLine("<gold>Deze booster vervalt <yellow>nooit<gold>.");

            icon.addLoreLine(" ").addLoreLine("<gold>Klik om deze booster te verwijderen.");

            Icon boosterIcon = new Icon(icon.toItemStack(), event -> {
                minetopiaPlayer.getFitness().removeBooster(booster);
                new AdminToolFitnessBoostersMenu(player, offlinePlayer, minetopiaPlayer, bankAccountModel).open((Player) event.getWhoClicked());
            });
            this.addItem(boosterIcon);


            ItemBuilder backItemBuilder = new ItemBuilder(Material.OAK_DOOR)
                    .setName("<gray>Terug");

            Icon backIcon = new Icon(22, backItemBuilder.toItemStack(), event -> {
                new AdminToolFitnessMenu(player, offlinePlayer, minetopiaPlayer, bankAccountModel).open((Player) event.getWhoClicked());
            });
            this.addSpecialIcon(backIcon);
        }
    }

    @Override
    public Icon getPreviousPageItem() {
        return new Icon(18, new ItemBuilder(Material.ARROW)
                .setName("<gold>Vorige pagina")
                .toItemStack(), event -> event.setCancelled(true));
    }

    @Override
    public Icon getNextPageItem() {
        return new Icon(26, new ItemBuilder(Material.ARROW)
                .setName("<gold>Volgende pagina")
                .toItemStack(), event -> event.setCancelled(true));
    }

    private int millisToHours(long millis) {
        return (int) (millis / 1000 / 60 / 60);
    }

    private int millisToMinutes(long millis) {
        return (int) (millis / 1000 / 60);
    }

    private int millisToSeconds(long millis) {
        return (int) (millis / 1000);
    }
}
