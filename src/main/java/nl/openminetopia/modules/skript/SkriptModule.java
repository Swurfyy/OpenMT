package nl.openminetopia.modules.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.Module;

import java.io.IOException;

@Getter
public class SkriptModule extends Module {


    private SkriptAddon addon;

    @Override
    public void enable() {
        addon = Skript.registerAddon(OpenMinetopia.getInstance());
        try {
            addon.loadClasses("nl.openminetopia.modules.skript", "expressions");
            addon.loadClasses("nl.openminetopia.modules.skript", "effects");
        } catch (IOException e) {
            OpenMinetopia.getInstance().getLogger().severe("Failed to load classes for Skript module");
        }
    }

    @Override
    public void disable() {

    }
}
