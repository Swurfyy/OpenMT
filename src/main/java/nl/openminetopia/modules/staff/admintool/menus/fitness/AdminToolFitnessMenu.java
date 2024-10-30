package nl.openminetopia.modules.staff.admintool.menus.fitness;

import com.jazzkuh.inventorylib.objects.Menu;
import com.jazzkuh.inventorylib.objects.icon.Icon;
import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.fitness.FitnessStatisticType;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.FitnessConfiguration;
import nl.openminetopia.modules.fitness.models.FitnessStatisticModel;
import nl.openminetopia.modules.staff.admintool.menus.AdminToolInfoMenu;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

@Getter
public class AdminToolFitnessMenu extends Menu {

    private final Player player;
    private final OfflinePlayer offlinePlayer;

    public AdminToolFitnessMenu(Player player, OfflinePlayer offlinePlayer) {
        super(ChatUtils.color("<gold>Fitheid <yellow>" + offlinePlayer.getPlayerProfile().getName()), 3);
        this.player = player;
        this.offlinePlayer = offlinePlayer;

        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(offlinePlayer);
        if (minetopiaPlayer == null) return;

        FitnessConfiguration configuration = OpenMinetopia.getFitnessConfiguration();

        // Add statistics for each fitness type
        addFitnessStatisticItem(minetopiaPlayer, FitnessStatisticType.DRINKING, Material.POTION, 9,
                "<gold>Drinken", "<dark_purple>Spelers krijgen <light_purple>" + configuration.getDrinkingPointsPerWaterBottle() +
                        " <dark_purple>punt voor het drinken van water.");

        addFitnessStatisticItem(minetopiaPlayer, FitnessStatisticType.HEALTH, Material.APPLE, 10,
                "<gold>Fatsoenlijk eten", "<dark_purple>Spelers krijgen <light_purple>" + configuration.getPointsAbove9Hearts() +
                        " <dark_purple>punt als hun voedselniveau hoger is dan 9.");

        addFitnessStatisticItem(minetopiaPlayer, FitnessStatisticType.EATING, Material.GOLDEN_APPLE, 11,
                "<gold>Eten", "<dark_purple>Luxe eten: <light_purple>" + configuration.getLuxuryFood());

        addFitnessStatisticItem(minetopiaPlayer, FitnessStatisticType.CLIMBING, Material.LADDER, 12,
                "<gold>Klimmen", "<dark_purple>Spelers krijgen <light_purple>1 punt per " + (configuration.getCmPerClimbingLevel() / 1000) + " kilometer klimmen.");

        addFitnessStatisticItem(minetopiaPlayer, FitnessStatisticType.FLYING, Material.ELYTRA, 13,
                "<gold>Vliegen", "<dark_purple>Spelers krijgen <light_purple>1 punt per " + (configuration.getCmPerFlyingLevel() / 1000) + " kilometer vliegen.");

        addFitnessStatisticItem(minetopiaPlayer, FitnessStatisticType.WALKING, Material.LEATHER_BOOTS, 14,
                "<gold>Lopen", "<dark_purple>Spelers krijgen <light_purple>1 punt per " + (configuration.getCmPerWalkingLevel() / 1000) + " kilometer lopen.");

        addFitnessStatisticItem(minetopiaPlayer, FitnessStatisticType.SWIMMING, Material.OAK_BOAT, 15,
                "<gold>Zwemmen", "<dark_purple>Spelers krijgen <light_purple>1 punt per " + (configuration.getCmPerWalkingLevel() / 1000) + " kilometer zwemmen.");

        addFitnessStatisticItem(minetopiaPlayer, FitnessStatisticType.SPRINTING, Material.DIAMOND_BOOTS, 16,
                "<gold>Rennen", "<dark_purple>Spelers krijgen <light_purple>1 punt per " + (configuration.getCmPerSprintingLevel() / 1000) + " kilometer rennen.");

        addTotalStatisticIcon(minetopiaPlayer);

        // Back button
        ItemBuilder backItemBuilder = new ItemBuilder(Material.OAK_DOOR).setName("<gray>Terug");
        Icon backIcon = new Icon(22, backItemBuilder.toItemStack(), event -> {
            new AdminToolInfoMenu(player, offlinePlayer).open((Player) event.getWhoClicked());
        });
        this.addItem(backIcon);
    }

    private void addFitnessStatisticItem(MinetopiaPlayer minetopiaPlayer, FitnessStatisticType type, Material material,
                                         int slot, String name, String description) {
        FitnessStatisticModel statistic = minetopiaPlayer.getFitness().getStatistic(type);
        double kilometers = getKilometersFromStatistic(type);

        ItemBuilder itemBuilder = new ItemBuilder(material)
                .setName(name + " " + statistic.getFitnessGained() + "/" + statistic.getMaximum())
                .addLoreLine(" ")
                .addLoreLine("<gold>Precieze score: <yellow>" + statistic.getPoints())
                .addLoreLine("<gold>Kilometers: <yellow>" + kilometers)
                .addLoreLine(" ")
                .addLoreLine(description)
                .addLoreLine(" ");

        Icon icon = new Icon(slot, itemBuilder.toItemStack(), event -> event.setCancelled(true));
        this.addItem(icon);
    }

    private void addTotalStatisticIcon(MinetopiaPlayer minetopiaPlayer) {

        ItemBuilder totalItemBuilder = new ItemBuilder(Material.PAPER)
                .setName("<gold>Totaal: <yellow>" + minetopiaPlayer.getFitness().getTotalFitness() + "<gold>/<yellow>"
                        + OpenMinetopia.getFitnessConfiguration().getMaxFitnessLevel())
                .addLoreLine(" ")
                .addLoreLine("<gold>Klik om de <yellow>fitness boosters <gold>te bekijken.");

        Icon totalIcon = new Icon(17, totalItemBuilder.toItemStack(), event -> {
            event.setCancelled(true);
            new AdminToolFitnessBoostersMenu(player, offlinePlayer).open((Player) event.getWhoClicked());
        });
        this.addItem(totalIcon);
    }

    private double getKilometersFromStatistic(FitnessStatisticType type) {
        Statistic stat;
        switch (type) {
            case WALKING -> stat = Statistic.WALK_ONE_CM;
            case CLIMBING -> stat = Statistic.CLIMB_ONE_CM;
            case SPRINTING -> stat = Statistic.SPRINT_ONE_CM;
            case SWIMMING -> stat = Statistic.SWIM_ONE_CM;
            case FLYING -> stat = Statistic.AVIATE_ONE_CM;
            default -> {
                return 0.0;
            }
        }
        return offlinePlayer.getStatistic(stat) / 100000.0;
    }
}
