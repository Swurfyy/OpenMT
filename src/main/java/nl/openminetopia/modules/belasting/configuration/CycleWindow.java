package nl.openminetopia.modules.belasting.configuration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * A single tax cycle window: start and end as epoch millis.
 * Display format: dd/MM/yyyy | HH:mm
 */
public record CycleWindow(long startMs, long endMs) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    public static String format(long epochMs, ZoneId zone) {
        if (epochMs <= 0) return "";
        var zdt = Instant.ofEpochMilli(epochMs).atZone(zone);
        return DATE.format(zdt) + " | " + TIME.format(zdt);
    }

    public boolean contains(long epochMs) {
        return epochMs >= startMs && epochMs <= endMs;
    }
}
