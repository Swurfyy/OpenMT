package nl.openminetopia.modules.currencies.configuration;

import nl.openminetopia.modules.currencies.CurrencyModule;
import nl.openminetopia.modules.currencies.objects.RegisteredCurrency;
import nl.openminetopia.utils.ConfigurateConfig;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CurrencyConfiguration extends ConfigurateConfig {

    public CurrencyConfiguration(CurrencyModule module, File file) {
        super(file, "currencies.yml", "default/currencies.yml", false);

        ConfigurationNode node = rootNode.node("currencies");

        node.childrenMap().forEach((currencyObj, currencyNode) -> {
            String currencyId = (String) currencyObj;

            String displayName = currencyNode.node("display-name").getString();
            String command = currencyNode.node("command").getString();

            List<String> aliases = new ArrayList<>();
            try {
                aliases.addAll(currencyNode.node("aliases").getList(String.class));
            } catch (SerializationException e) {
                module.getLogger().warn("Couldn't register currency " + currencyId + ": " + e.getMessage());
            }

            RegisteredCurrency registeredCurrency = new RegisteredCurrency(
                    currencyId,
                    displayName,
                    command,
                    aliases
            );

            ConfigurationNode automaticNode = currencyNode.node("automatic");

            if (!automaticNode.isNull()) {
                registeredCurrency.setAutomatic(true);
                registeredCurrency.setAmount(automaticNode.node("amount").getInt());
                registeredCurrency.setInterval(automaticNode.node("interval").getInt());
            }

            module.getCurrencies().add(registeredCurrency);
            module.getLogger().info("Currency " + registeredCurrency.getId() + " registered. (auto: " + registeredCurrency.isAutomatic() + ")");
        });
    }
}
