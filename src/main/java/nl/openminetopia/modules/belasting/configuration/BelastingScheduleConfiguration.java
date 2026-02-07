package nl.openminetopia.modules.belasting.configuration;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.utils.config.ConfigurateConfig;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Loads schedule.yml with fixed cycle windows (date + time in readable format).
 * Format: dd/MM/yyyy and HH:mm. Uses server default timezone.
 */
public class BelastingScheduleConfiguration extends ConfigurateConfig {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private final List<CycleWindow> cycles;
    private final ZoneId zone;

    public BelastingScheduleConfiguration(File dataFolder) {
        super(dataFolder, "schedule.yml", "default/schedule.yml", true);
        this.zone = ZoneId.systemDefault();
        this.cycles = parseCycles();
    }

    private List<CycleWindow> parseCycles() {
        List<CycleWindow> out = new ArrayList<>();
        ConfigurationNode scheduleNode = rootNode.node("schedule");
        if (scheduleNode.empty()) return out;

        for (var entry : scheduleNode.childrenMap().entrySet()) {
            ConfigurationNode cycleNode = entry.getValue();
            long startMs = parseStart(cycleNode);
            long endMs = parseEnd(cycleNode);
            if (startMs > 0 && endMs >= startMs) {
                out.add(new CycleWindow(startMs, endMs));
            }
        }
        out.sort(Comparator.comparingLong(CycleWindow::startMs));
        for (int i = 0; i < out.size() - 1; i++) {
            CycleWindow a = out.get(i);
            CycleWindow b = out.get(i + 1);
            if (a.endMs() > b.startMs()) {
                OpenMinetopia.getInstance().getLogger().warning("Schedule: overlappende cycles gedetecteerd (" + format(a.startMs()) + " t/m " + format(a.endMs()) + " en " + format(b.startMs()) + " t/m " + format(b.endMs()) + "). Eerste match wordt gebruikt.");
            }
        }
        return out;
    }

    private long parseStart(ConfigurationNode cycleNode) {
        if (cycleNode.isList() && cycleNode.childrenList().size() >= 2) {
            var list = cycleNode.childrenList();
            return toEpochMs(list.get(0).getString(""), list.get(1).getString(""));
        }
        return toEpochMs(
                cycleNode.node("start-date").getString(""),
                cycleNode.node("start-time").getString("")
        );
    }

    private long parseEnd(ConfigurationNode cycleNode) {
        if (cycleNode.isList() && cycleNode.childrenList().size() >= 4) {
            var list = cycleNode.childrenList();
            return toEpochMs(list.get(2).getString(""), list.get(3).getString(""));
        }
        return toEpochMs(
                cycleNode.node("end-date").getString(""),
                cycleNode.node("end-time").getString("")
        );
    }

    private long toEpochMs(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.isBlank() || timeStr == null || timeStr.isBlank()) return 0;
        try {
            LocalDate date = LocalDate.parse(dateStr.trim(), DATE);
            LocalTime time = LocalTime.parse(timeStr.trim(), TIME);
            return LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            OpenMinetopia.getInstance().getLogger().warning("Schedule: ongeldige datum/tijd '" + dateStr + "' / '" + timeStr + "': " + e.getMessage());
            return 0;
        }
    }

    public List<CycleWindow> getCycles() {
        return cycles;
    }

    public ZoneId getZone() {
        return zone;
    }

    /** Returns the cycle that contains the given timestamp, or null. */
    public CycleWindow getCurrentCycle(long nowMs) {
        for (CycleWindow w : cycles) {
            if (w.contains(nowMs)) return w;
        }
        return null;
    }

    /** Start of the next cycle after the given time, or 0 if none. */
    public long getNextCycleStart(long afterMs) {
        for (CycleWindow w : cycles) {
            if (w.startMs() > afterMs) return w.startMs();
        }
        return 0;
    }

    /** Start of the cycle that contains now, or 0. */
    public long getCurrentCycleStart(long nowMs) {
        CycleWindow w = getCurrentCycle(nowMs);
        return w != null ? w.startMs() : 0;
    }

    /** Returns the cycle window that starts at the given timestamp, or null. */
    public CycleWindow getCycleByStart(long startMs) {
        if (startMs <= 0) return null;
        for (CycleWindow w : cycles) {
            if (w.startMs() == startMs) return w;
        }
        return null;
    }

    public String format(long epochMs) {
        return CycleWindow.format(epochMs, zone);
    }
}
