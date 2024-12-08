package nl.openminetopia.modules.portal;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.portal.commands.LinkCommand;

public class PortalModule extends Module {

    @Override
    public void enable() {
        if (OpenMinetopia.getDefaultConfiguration().isPortalEnabled()) {
            registerCommand(new LinkCommand());
        }
    }

    @Override
    public void disable() {

    }

    public String getPortalUrl() {
        return OpenMinetopia.getDefaultConfiguration().getPortalUrl();
    }

    public String getPortalApiUrl() {
        return "https://" + getPortalUrl() + "/api";
    }
}
