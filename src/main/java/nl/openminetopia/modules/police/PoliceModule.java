package nl.openminetopia.modules.police;

import com.craftmend.storm.api.enums.Where;
import lombok.Getter;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
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
import nl.openminetopia.modules.prefix.models.PrefixModel;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class PoliceModule extends Module {

    public HashMap<UUID, Long> emergencyCooldowns = new HashMap<>();

    @Override
    public void enable() {
        registerCommand(new EmergencyCommand());
        registerCommand(new CriminalRecordCommand());
        registerCommand(new BodysearchCommand());
        registerListener(new PlayerArmorChangeListener());

        registerListener(new PlayerInteractEntityListener());
        registerListener(new PlayerMoveListener());
        registerListener(new PlayerDropItemListener());
        registerListener(new PlayerEntityDamageListener());
        registerListener(new PlayerOpenInventoryListener());
        registerListener(new PlayerPickupItemListener());
        registerListener(new PlayerSlotChangeListener());
        registerListener(new PlayerInventoryClickListener());
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
