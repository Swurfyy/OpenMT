package nl.openminetopia.api.player.objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.places.MTPlaceManager;
import nl.openminetopia.api.places.objects.MTPlace;
import nl.openminetopia.configuration.DefaultConfiguration;
import nl.openminetopia.modules.color.ColorModule;
import nl.openminetopia.modules.color.enums.OwnableColorType;
import nl.openminetopia.modules.color.objects.*;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.places.PlacesModule;
import nl.openminetopia.modules.places.models.WorldModel;
import nl.openminetopia.modules.player.PlayerModule;
import nl.openminetopia.modules.player.models.PlayerModel;
import nl.openminetopia.modules.police.PoliceModule;
import nl.openminetopia.modules.police.models.CriminalRecordModel;
import nl.openminetopia.modules.prefix.PrefixModule;
import nl.openminetopia.modules.prefix.objects.Prefix;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class MinetopiaPlayer {

    private final UUID uuid;
    private final PlayerModel playerModel;

    private @Setter boolean scoreboardVisible;
    private @Setter boolean actionbarVisible;

    @Getter
    private long playtime;
    private transient long startTime;


    private boolean staffchatEnabled;
    private boolean commandSpyEnabled;
    private boolean chatSpyEnabled;

    private List<Prefix> prefixes;
    private Prefix activePrefix;

    private List<OwnableColor> colors;
    private PrefixColor activePrefixColor;
    private NameColor activeNameColor;
    private ChatColor activeChatColor;
    private LevelColor activeLevelColor;

    private final @Getter(AccessLevel.PRIVATE) PlayerModule playerModule = OpenMinetopia.getModuleManager().get(PlayerModule.class);
    private final @Getter(AccessLevel.PRIVATE) PrefixModule prefixModule = OpenMinetopia.getModuleManager().get(PrefixModule.class);
    private final @Getter(AccessLevel.PRIVATE) ColorModule colorModule = OpenMinetopia.getModuleManager().get(ColorModule.class);
    private final @Getter(AccessLevel.PRIVATE) PlacesModule placesModule = OpenMinetopia.getModuleManager().get(PlacesModule.class);
    private final @Getter(AccessLevel.PRIVATE) PoliceModule policeModule = OpenMinetopia.getModuleManager().get(PoliceModule.class);

    public MinetopiaPlayer(UUID uuid, PlayerModel playerModel) {
        this.uuid = uuid;
        this.playerModel = playerModel;
    }

    public CompletableFuture<Void> load() {
        CompletableFuture<Void> loadFuture = new CompletableFuture<>();

        DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();

        if (this.getBukkit().getPlayer() != null && this.getBukkit().isOnline())
            this.getBukkit().getPlayer().sendMessage(ChatUtils.color("<red>Je data wordt geladen..."));

        this.playtime = this.playerModel.getPlaytime();
        this.startTime = System.currentTimeMillis();
        this.staffchatEnabled = this.playerModel.getStaffchatEnabled();
        this.commandSpyEnabled = this.playerModel.getCommandSpyEnabled();
        this.chatSpyEnabled = this.playerModel.getChatSpyEnabled();

        this.colors = colorModule.getColorsFromPlayer(this.playerModel);
        this.activeChatColor = (ChatColor) colorModule.getActiveColorFromPlayer(this.playerModel, OwnableColorType.CHAT)
                .orElse(OwnableColorType.CHAT.defaultColor());

        this.activeNameColor = (NameColor) colorModule.getActiveColorFromPlayer(this.playerModel, OwnableColorType.NAME)
                .orElse(OwnableColorType.NAME.defaultColor());

        this.activePrefixColor = (PrefixColor) colorModule.getActiveColorFromPlayer(this.playerModel, OwnableColorType.PREFIX)
                .orElse(OwnableColorType.PREFIX.defaultColor());

        this.activeLevelColor = (LevelColor) colorModule.getActiveColorFromPlayer(this.playerModel, OwnableColorType.LEVEL)
                .orElse(OwnableColorType.LEVEL.defaultColor());

        this.prefixes = prefixModule.getPrefixesFromPlayer(this.playerModel);
        this.activePrefix = prefixModule.getActivePrefixFromPlayer(playerModel)
                .orElse(new Prefix(-1, configuration.getDefaultPrefix(), -1));

        loadFuture.complete(null);
        return loadFuture;
    }

    public CompletableFuture<Void> save() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        StormDatabase.getInstance().saveStormModel(this.playerModel);

        future.complete(null);
        return future;
    }


    public OfflinePlayer getBukkit() {
        return Bukkit.getOfflinePlayer(uuid);
    }


    /* ---------- Places ---------- */

    public boolean isInPlace() {
        return getPlace() != null;
    }

    public MTPlace getPlace() {
        return MTPlaceManager.getInstance().getPlace(getBukkit().getLocation());
    }

    public WorldModel getWorld() {
        if (getBukkit().getPlayer() == null) {
            return null;
        }
        return placesModule.getWorldModels().stream()
                .filter(worldModel -> worldModel.getName().equalsIgnoreCase(getBukkit().getPlayer().getWorld().getName()))
                .findFirst().orElse(null);
    }


    /* ---------- Staffchat ---------- */

    public void setStaffchatEnabled(boolean staffchatEnabled) {
        this.staffchatEnabled = staffchatEnabled;
        playerModel.setStaffchatEnabled(staffchatEnabled);
        StormDatabase.getInstance().saveStormModel(playerModel);
    }

    /* ---------- Spy ---------- */

    public void setCommandSpyEnabled(boolean commandSpyEnabled) {
        this.commandSpyEnabled = commandSpyEnabled;
        playerModel.setCommandSpyEnabled(commandSpyEnabled);
        StormDatabase.getInstance().saveStormModel(playerModel);
    }

    public void setChatSpyEnabled(boolean chatSpyEnabled) {
        this.chatSpyEnabled = chatSpyEnabled;
        this.playerModel.setChatSpyEnabled(chatSpyEnabled);
        StormDatabase.getInstance().saveStormModel(this.playerModel);
    }

    /* ---------- Prefix ---------- */


    public void addPrefix(Prefix prefix) {
        prefixModule.addPrefix(this, prefix).whenComplete((id, throwable) -> {
            if (throwable != null) {
                OpenMinetopia.getInstance().getLogger().severe("Failed to add prefix: " + throwable.getMessage());
                return;
            }
            prefixes.add(new Prefix(id, prefix.getPrefix(), prefix.getExpiresAt()));
        });
    }


    public void removePrefix(Prefix prefix) {
        DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();
        prefixes.remove(prefix);

        if (activePrefix == prefix) {
            activePrefix = new Prefix(-1, configuration.getDefaultPrefix(), -1);
            this.setActivePrefix(activePrefix);
        }

        prefixModule.removePrefix(prefix);
    }


    public void setActivePrefix(Prefix prefix) {
        this.activePrefix = prefix;
        this.playerModel.setActivePrefixId(prefix.getId());
    }


    public Prefix getActivePrefix() {
        DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();
        if (activePrefix == null) {
            activePrefix = new Prefix(-1, configuration.getDefaultPrefix(), -1);
        }

        if (activePrefix.isExpired()) {
            Player player = this.getBukkit().getPlayer();
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatUtils.color("<red>Je prefix <dark_red>" + activePrefix.getPrefix() + " <red>is verlopen!"));
            }

            this.removePrefix(activePrefix);
            this.setActivePrefix(new Prefix(-1, configuration.getDefaultPrefix(), -1));
        }

        return activePrefix;
    }

    /* ---------- Colors ---------- */

    public void addColor(OwnableColor color) {
        this.colorModule.addColor(this, color).whenComplete((id, throwable) -> {
            if (throwable != null) {
                OpenMinetopia.getInstance().getLogger().severe("Failed to add color: " + throwable.getMessage());
                return;
            }

            switch (color.getType()) {
                case PREFIX -> colors.add(new PrefixColor(id, color.getColorId(), color.getExpiresAt()));
                case NAME -> colors.add(new NameColor(id, color.getColorId(), color.getExpiresAt()));
                case CHAT -> colors.add(new ChatColor(id, color.getColorId(), color.getExpiresAt()));
                case LEVEL -> colors.add(new LevelColor(id, color.getColorId(), color.getExpiresAt()));
            }
        });
    }

    public void removeColor(OwnableColor color) {
        this.colors.remove(color);
        this.colorModule.removeColor(color);
    }

    public void setActiveColor(OwnableColor color, OwnableColorType type) {
        if (color == null || color.getId() == 0) {
            color = type.defaultColor();
        }

        switch (type) {
            case PREFIX:
                this.activePrefixColor = (PrefixColor) color;
                this.playerModel.setActivePrefixColorId(color.getId());
                break;
            case NAME:
                this.activeNameColor = (NameColor) color;
                this.playerModel.setActiveNameColorId(color.getId());
                break;
            case CHAT:
                this.activeChatColor = (ChatColor) color;
                this.playerModel.setActiveChatColorId(color.getId());
                break;
            case LEVEL:
                this.activeLevelColor = (LevelColor) color;
                this.playerModel.setActiveLevelColorId(color.getId());
                break;
        }
    }

    public OwnableColor getActiveColor(OwnableColorType type) {
        OwnableColor color = switch (type) {
            case PREFIX -> this.activePrefixColor;
            case NAME -> this.activeNameColor;
            case CHAT -> this.activeChatColor;
            case LEVEL -> this.activeLevelColor;
        };

        if (color == null || color.getId() == 0) {
            color = type.defaultColor();
        }

        if (color.isExpired()) {
            Player player = this.getBukkit().getPlayer();
            if (player != null && player.isOnline())
                player.sendMessage(ChatUtils.color("<red>Je " + type.name().toLowerCase() + " kleur <dark_red>" + color.getColorId() + " is verlopen!"));
            removeColor(color);
            setActiveColor(type.defaultColor(), type);
        }
        return color;
    }

    /* ---------- Criminal record ---------- */

    public void addCriminalRecord(String description, UUID officer, Long date) {
        policeModule.addCriminalRecord(this.playerModel, description, officer, date);
    }

    public void removeCriminalRecord(CriminalRecordModel criminalRecord) {
        policeModule.removeCriminalRecord(this.playerModel, criminalRecord);
    }

    public List<CriminalRecordModel> getCriminalRecords() {
        return playerModel.getCriminalRecords();
    }

    /* ---------- Playtime ---------- */

    public void updatePlaytime(){
        this.playtime = this.playtime + resetStart();
        this.playerModel.setPlaytime(this.playtime);
    }


    private long sinceStart() {
        return System.currentTimeMillis() - this.startTime;
    }

    private long resetStart() {
        long since = sinceStart();
        this.startTime = System.currentTimeMillis();
        return since;
    }
}