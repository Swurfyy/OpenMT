package nl.openminetopia.modules.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import com.mysql.cj.x.protobuf.MysqlxCursor;
import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.Module;
import org.bukkit.Bukkit;

import java.io.IOException;

@Getter
public class SkriptModule extends Module {


    private SkriptAddon addon;

    @Override
    public void enable() {
        if(!Bukkit.getServer().getPluginManager().isPluginEnabled("Skript")) return;

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
