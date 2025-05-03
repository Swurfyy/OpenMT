package nl.openminetopia.modules.labymod.configuration;

import lombok.Getter;
import nl.openminetopia.utils.ConfigurateConfig;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;

@Getter
public class LabymodConfiguration extends ConfigurateConfig {

	private final boolean enabled;

	private final boolean EconomyDisplayEnabled;
	private final String EconomyIconUrl;

	public LabymodConfiguration(File file) {
		super(file, "labymod.yml", "default-labymod.yml", true);

		this.enabled = rootNode.node("enabled").getBoolean();

		ConfigurationNode economyNode = rootNode.node("economy-display");

		this.EconomyDisplayEnabled = economyNode.node( "enabled").getBoolean();
		this.EconomyIconUrl = economyNode.node("icon-url").getString();

	}


}
