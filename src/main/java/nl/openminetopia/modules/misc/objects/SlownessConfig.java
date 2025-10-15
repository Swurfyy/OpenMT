package nl.openminetopia.modules.misc.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class SlownessConfig {
    private final boolean enabled;
    private final int duration;
    private final int amplifier;
}

