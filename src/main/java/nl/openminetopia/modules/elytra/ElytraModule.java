package nl.openminetopia.modules.elytra;

import com.jazzkuh.modulemanager.spigot.SpigotModuleManager;
import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.data.DataModule;
import nl.openminetopia.modules.elytra.commands.ElytraBypassCommand;
import nl.openminetopia.modules.elytra.configuration.ElytraConfiguration;
import nl.openminetopia.modules.elytra.listeners.ElytraBoostListener;
import nl.openminetopia.utils.modules.ExtendedSpigotModule;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class ElytraModule extends ExtendedSpigotModule {

    private ElytraConfiguration configuration;
    private final Set<UUID> bypassedPlayers = new HashSet<>();

    public ElytraModule(SpigotModuleManager<@NotNull OpenMinetopia> moduleManager, DataModule dataModule) {
        super(moduleManager);
    }

    @Override
    public void onEnable() {
        configuration = new ElytraConfiguration(OpenMinetopia.getInstance().getDataFolder());
        configuration.saveConfiguration();

        registerComponent(new ElytraBoostListener());
        registerComponent(new ElytraBypassCommand());
    }

    public boolean isPlayerBypassed(UUID playerId) {
        return bypassedPlayers.contains(playerId);
    }

    public void addBypass(UUID playerId) {
        bypassedPlayers.add(playerId);
    }

    public void removeBypass(UUID playerId) {
        bypassedPlayers.remove(playerId);
    }
}
