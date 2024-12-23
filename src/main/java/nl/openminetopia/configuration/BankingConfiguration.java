package nl.openminetopia.configuration;

import lombok.Getter;
import lombok.SneakyThrows;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.utils.ConfigurateConfig;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BankingConfiguration extends ConfigurateConfig {

    private final String economyFormat;
    private final List<Material> atmMaterials;

    private final String typeSelectionTitle;
    private final String accountSelectionTitle;
    private final String accountContentsTitle;

    @SneakyThrows
    public BankingConfiguration(File file) {
        super(file, "banking.yml", "");

        this.economyFormat = rootNode.node("banking", "economy-format").getString("#,##0.00");
        this.atmMaterials = new ArrayList<>();
        rootNode.node("banking", "atm-materials").getList(String.class, List.of(
                "RED_SANDSTONE_STAIRS"
        )).forEach(materialString -> {
            Material material = Material.matchMaterial(materialString);
            if (material == null) {
                OpenMinetopia.getInstance().getLogger().warning("Invalid material in atm-materials: " + materialString);
                return;
            }
            this.atmMaterials.add(material);
        });

        this.typeSelectionTitle = rootNode.node("banking", "inventories", "select-type-title").getString("<gray>Select het rekeningtype:");
        this.accountSelectionTitle = rootNode.node("banking", "inventories", "select-account-title").getString("<type_color><type_name>");
        this.accountContentsTitle = rootNode.node("banking", "inventories", "account-contents-title").getString("<type_color><account_name> <reset>| <gold><account_balance>");
    }
}
