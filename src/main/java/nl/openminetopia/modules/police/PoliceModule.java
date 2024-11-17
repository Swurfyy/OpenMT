package nl.openminetopia.modules.police;

import com.craftmend.storm.api.enums.Where;
import lombok.Getter;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.data.utils.StormUtils;
import nl.openminetopia.modules.player.models.PlayerModel;
import nl.openminetopia.modules.police.commands.BodysearchCommand;
import nl.openminetopia.modules.police.commands.CriminalRecordCommand;
import nl.openminetopia.modules.police.commands.EmergencyCommand;
import nl.openminetopia.modules.police.balaclava.listeners.PlayerArmorChangeListener;
import nl.openminetopia.modules.police.handcuff.HandcuffManager;
import nl.openminetopia.modules.police.handcuff.listeners.*;
import nl.openminetopia.modules.police.handcuff.objects.HandcuffedPlayer;
import nl.openminetopia.modules.police.models.CriminalRecordModel;
import nl.openminetopia.modules.police.nightvision.listeners.PlayerEquipNightvisionListener;
import nl.openminetopia.modules.police.pepperspray.listeners.PlayerPeppersprayListener;
import nl.openminetopia.modules.police.taser.TaserManager;
import nl.openminetopia.modules.police.taser.listeners.PlayerTaserListener;
import nl.openminetopia.modules.police.walkietalkie.WalkieTalkieManager;
import nl.openminetopia.modules.police.walkietalkie.listeners.PlayerChatListener;
import nl.openminetopia.modules.police.walkietalkie.listeners.PlayerInteractListener;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class PoliceModule extends Module {

    private final HashMap<UUID, Long> emergencyCooldowns = new HashMap<>();

    private WalkieTalkieManager walkieTalkieManager;
    private TaserManager taserManager;

    @Override
    public void enable() {
        walkieTalkieManager = new WalkieTalkieManager();
        taserManager = new TaserManager();

        registerCommand(new EmergencyCommand());
        registerCommand(new CriminalRecordCommand());
        registerCommand(new BodysearchCommand());
        registerListener(new PlayerArmorChangeListener());

        /* ---- Handcuff ---- */
        registerListener(new PlayerHandcuffListener());
        registerListener(new PlayerMoveListener());
        registerListener(new PlayerDropItemListener());
        registerListener(new PlayerEntityDamageListener());
        registerListener(new PlayerOpenInventoryListener());
        registerListener(new PlayerPickupItemListener());
        registerListener(new PlayerSlotChangeListener());
        registerListener(new PlayerInventoryClickListener());

        /* ---- Walkie-talkie ---- */
        registerListener(new PlayerChatListener());
        registerListener(new PlayerInteractListener());

        /* ---- Pepperspray ---- */
        registerListener(new PlayerPeppersprayListener());

        /* ---- Taser ---- */
        registerListener(new PlayerTaserListener());

        /* ---- Nightvision ---- */
        registerListener(new PlayerEquipNightvisionListener());
    }

    @Override
    public void disable() {
        HandcuffManager.getInstance().getHandcuffedPlayers().forEach(HandcuffedPlayer::release);
    }

    public CompletableFuture<Integer> addCriminalRecord(PlayerModel playerModel, String description, UUID officerUuid, Long date) {
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            CriminalRecordModel criminalRecordModel = new CriminalRecordModel();
            criminalRecordModel.setPlayerId(playerModel.getId());
            criminalRecordModel.setDescription(description);
            criminalRecordModel.setOfficerId(officerUuid);
            criminalRecordModel.setDate(date);

            playerModel.getCriminalRecords().add(criminalRecordModel);

            int id = StormDatabase.getInstance().saveStormModel(criminalRecordModel).join();
            completableFuture.complete(id);
        });
        return completableFuture;
    }

    public CompletableFuture<Void> removeCriminalRecord(PlayerModel playerModel, CriminalRecordModel criminalRecordModel) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        StormDatabase.getExecutorService().submit(() -> {
            playerModel.getCriminalRecords().remove(criminalRecordModel);
            StormUtils.deleteModelData(CriminalRecordModel.class, query ->
                    query.where("id", Where.EQUAL, criminalRecordModel.getId()));
            completableFuture.complete(null);
        });
        return completableFuture;
    }
}
