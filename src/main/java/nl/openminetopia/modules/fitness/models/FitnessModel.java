package nl.openminetopia.modules.fitness.models;


import com.craftmend.storm.api.StormModel;
import com.craftmend.storm.api.enums.ColumnType;
import com.craftmend.storm.api.enums.KeyType;
import com.craftmend.storm.api.markers.Column;
import com.craftmend.storm.api.markers.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.openminetopia.modules.player.models.PlayerModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper=false)
@Table(name = "fitness")
public class FitnessModel extends StormModel {

    @Column(
            keyType = KeyType.FOREIGN,
            references = {PlayerModel.class}
    )
    private Integer playerId;

    @Column(
            type = ColumnType.ONE_TO_MANY,
            references = FitnessStatisticModel.class,
            matchTo = "fitness_id"
    )
    private List<FitnessStatisticModel> statistics = new ArrayList<>();

    @Column(
            type = ColumnType.ONE_TO_MANY,
            references = FitnessBoosterModel.class,
            matchTo = "fitness_id"
    )
    private List<FitnessBoosterModel> boosters = new ArrayList<>();
}
