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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disable() {

    }
}
