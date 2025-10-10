package nl.openminetopia.modules.misc.objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BootEffectType {
    SPEED("Speed"),
    ICE("Frost Walker"),
    BLUB("Depth Strider");

    private final String displayName;

    public static BootEffectType fromString(String name) {
        for (BootEffectType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}

