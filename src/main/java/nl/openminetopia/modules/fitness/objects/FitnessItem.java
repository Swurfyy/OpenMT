package nl.openminetopia.modules.fitness.objects;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.inventory.ItemStack;

@Getter
@Accessors(fluent = true)
public class FitnessItem {
    private final String identifier;
    private final ItemStack itemStack;
    private final int fitnessAmount;
    private final int fitnessDuration;

    public FitnessItem(String identifier, ItemStack itemStack, int fitnessAmount, int fitnessDuration) {
        this.identifier = identifier;
        this.itemStack = itemStack;
        this.fitnessAmount = fitnessAmount;
        this.fitnessDuration = fitnessDuration;
    }
}
