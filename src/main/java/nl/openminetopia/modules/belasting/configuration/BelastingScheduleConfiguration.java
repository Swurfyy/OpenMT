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
        // Use mergeDefaults = false to prevent overwriting user's manual edits
        // Cycles are hardcoded, so we don't need to merge defaults
        super(dataFolder, "schedule.yml", "default/schedule.yml", false);
        this.zone = ZoneId.systemDefault();
        this.cycles = parseCycles();
    }
     
    /**
     * Override saveConfiguration to prevent any writes to schedule.yml.
     * Cycles are hardcoded, so the file should never be modified by the plugin.
     */
    @Override
    public void saveConfiguration() {
        // Do nothing - cycles are hardcoded and we don't want to overwrite user edits
        // This prevents any accidental writes that could corrupt the schedule.yml file
    }

    private List<CycleWindow> parseCycles() {
        List<CycleWindow> out = new ArrayList<>();
        
        // Hardcoded cycles
        // cycle1: 09/02/2026 00:00 - 22/02/2026 23:59
        out.add(new CycleWindow(toEpochMs("09/02/2026", "00:00"), toEpochMs("22/02/2026", "23:59")));
        
        // cycle2: 23/02/2026 00:00 - 08/03/2026 23:59
        out.add(new CycleWindow(toEpochMs("23/02/2026", "00:00"), toEpochMs("08/03/2026", "23:59")));
        
        // cycle3: 09/03/2026 00:00 - 22/03/2026 23:59
        out.add(new CycleWindow(toEpochMs("09/03/2026", "00:00"), toEpochMs("22/03/2026", "23:59")));
        
        // cycle4: 23/03/2026 00:00 - 08/04/2026 23:59
        out.add(new CycleWindow(toEpochMs("23/03/2026", "00:00"), toEpochMs("08/04/2026", "23:59")));
        
        // cycle5: 09/04/2026 00:00 - 22/04/2026 23:59
        out.add(new CycleWindow(toEpochMs("09/04/2026", "00:00"), toEpochMs("22/04/2026", "23:59")));
        
        // cycle6: 23/04/2026 00:00 - 08/05/2026 23:59
        out.add(new CycleWindow(toEpochMs("23/04/2026", "00:00"), toEpochMs("08/05/2026", "23:59")));
        
        // cycle7: 09/05/2026 00:00 - 22/05/2026 23:59
        out.add(new CycleWindow(toEpochMs("09/05/2026", "00:00"), toEpochMs("22/05/2026", "23:59")));
        
        // cycle8: 23/05/2026 00:00 - 08/06/2026 23:59
        out.add(new CycleWindow(toEpochMs("23/05/2026", "00:00"), toEpochMs("08/06/2026", "23:59")));
        
        // cycle9: 09/06/2026 00:00 - 22/06/2026 23:59
        out.add(new CycleWindow(toEpochMs("09/06/2026", "00:00"), toEpochMs("22/06/2026", "23:59")));
        
        // cycle10: 23/06/2026 00:00 - 08/07/2026 23:59
        out.add(new CycleWindow(toEpochMs("23/06/2026", "00:00"), toEpochMs("08/07/2026", "23:59")));
        
        // Sort cycles by start time
        out.sort(Comparator.comparingLong(CycleWindow::startMs));
        
        // Check for overlapping cycles
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
        // First try list format (default format)
        if (cycleNode.isList() && cycleNode.childrenList().size() >= 2) {
            var list = cycleNode.childrenList();
            String dateStr = list.get(0).getString("");
            String timeStr = list.get(1).getString("");
            if (!dateStr.isBlank() && !timeStr.isBlank()) {
                return toEpochMs(dateStr, timeStr);
            }
        }
        
        // Fallback to object format (if someone manually edited the config)
        String startDate = cycleNode.node("start-date").getString("");
        String startTime = cycleNode.node("start-time").getString("");
        
        // Validate that we actually have values (not empty strings from non-existent nodes)
        if (startDate.isBlank() || startTime.isBlank()) {
            // If object format doesn't have values, return 0 to skip this cycle
            return 0;
        }
        
        return toEpochMs(startDate, startTime);
    }

    private long parseEnd(ConfigurationNode cycleNode) {
        // First try list format (default format)
        if (cycleNode.isList() && cycleNode.childrenList().size() >= 4) {
            var list = cycleNode.childrenList();
            String dateStr = list.get(2).getString("");
            String timeStr = list.get(3).getString("");
            if (!dateStr.isBlank() && !timeStr.isBlank()) {
                return toEpochMs(dateStr, timeStr);
            }
        }
        
        // Fallback to object format (if someone manually edited the config)
        String endDate = cycleNode.node("end-date").getString("");
        String endTime = cycleNode.node("end-time").getString("");
        
        // Validate that we actually have values (not empty strings from non-existent nodes)
        if (endDate.isBlank() || endTime.isBlank()) {
            // If object format doesn't have values, return 0 to skip this cycle
            return 0;
        }
        
        return toEpochMs(endDate, endTime);
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
