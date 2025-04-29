package nl.openminetopia.modules.lock;

import com.jazzkuh.modulemanager.spigot.SpigotModule;
import com.jazzkuh.modulemanager.spigot.SpigotModuleManager;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.lock.commands.LockCommand;
import nl.openminetopia.modules.lock.commands.UnlockCommand;
import nl.openminetopia.modules.lock.listeners.LockInteractListener;
import org.jetbrains.annotations.NotNull;

public class LockModule extends SpigotModule<@NotNull OpenMinetopia> {
    public LockModule(SpigotModuleManager<@NotNull OpenMinetopia> moduleManager) {
        super(moduleManager);
    }

    @Override
    public void onEnable() {
        registerComponent(new LockCommand());
        registerComponent(new UnlockCommand());
        registerComponent(new LockInteractListener());
    }
}
