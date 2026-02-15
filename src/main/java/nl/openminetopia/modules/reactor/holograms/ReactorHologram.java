package nl.openminetopia.modules.reactor.holograms;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import nl.openminetopia.modules.reactor.objects.Reactor;
import org.bukkit.Location;

import java.util.List;

@Getter
public class ReactorHologram {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final Reactor reactor;
    private final Hologram hologram;
    private final Location location;
    
    // Cache for last hologram content to avoid unnecessary updates
    private List<String> lastContent = null;

    public ReactorHologram(Reactor reactor, Location location) {
        this.reactor = reactor;
        this.location = location;

        // Create hologram using DecentHolograms DHAPI
        String hologramId = "reactor_" + reactor.getRegionName();
        List<String> initialLines = buildHologramLines();
        // Convert MiniMessage to legacy color codes for DecentHolograms
        List<String> convertedLines = initialLines.stream()
                .map(this::convertMiniMessageToLegacy)
                .toList();
        this.hologram = DHAPI.createHologram(hologramId, location, convertedLines);
        
        // Set initial content
        updateContent();
        
        // Show hologram
        hologram.showAll();
    }

    /**
     * Converts MiniMessage string to legacy color code string for DecentHolograms
     */
    private String convertMiniMessageToLegacy(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return miniMessage;
        }
        
        try {
            Component component = MINI_MESSAGE.deserialize(miniMessage);
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Exception e) {
            // Fallback: return original string if parsing fails
            reactor.getModule().getLogger().warn("[ReactorHologram] Failed to parse MiniMessage: " + miniMessage + " - " + e.getMessage());
            return miniMessage;
        }
    }

    /**
     * Updates the hologram content
     * OPTIMIZED: Only updates if content has actually changed
     */
    public void updateContent() {
        try {
            List<String> lines = buildHologramLines();
            
            // Check if content has changed
            boolean contentChanged = false;
            if (lastContent == null || lastContent.size() != lines.size()) {
                contentChanged = true;
            } else {
                // Compare each line
                for (int i = 0; i < lines.size(); i++) {
                    if (i >= lastContent.size() || !lines.get(i).equals(lastContent.get(i))) {
                        contentChanged = true;
                        break;
                    }
                }
            }
            
            // Only update if content has changed
            if (!contentChanged) {
                return;
            }
            
            // Get the first page
            HologramPage page = hologram.getPage(0);
            if (page == null) {
                reactor.getModule().getLogger().warn("[ReactorHologram] Page 0 is null for reactor " + reactor.getRegionName());
                return; // Page should exist
            }
            
            // Update lines - convert MiniMessage to legacy color codes
            int lineCount = page.size();
            boolean linesChanged = false;
            for (int i = 0; i < lines.size(); i++) {
                String lineText = convertMiniMessageToLegacy(lines.get(i));
                if (i < lineCount) {
                    // Check if line actually changed before updating
                    HologramLine existingLine = page.getLine(i);
                    if (existingLine == null || !lineText.equals(existingLine.getContent())) {
                        page.setLine(i, lineText);
                        linesChanged = true;
                    }
                } else {
                    // Add new line - need to create HologramLine for addLine
                    page.addLine(new HologramLine(page, page.getNextLineLocation(), lineText));
                    linesChanged = true;
                }
            }
            
            // Remove excess lines if new content has fewer lines
            while (page.size() > lines.size()) {
                page.removeLine(page.size() - 1);
                linesChanged = true;
            }
            
            // Only call showAll() if lines actually changed
            if (linesChanged) {
                hologram.showAll();
            }
            
            // Update cached content
            lastContent = new java.util.ArrayList<>(lines);
        } catch (Exception e) {
            reactor.getModule().getLogger().warn("[ReactorHologram] Error updating hologram for " + reactor.getRegionName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Builds the hologram lines based on reactor state
     * Uses MiniMessage format for colors
     */
    private List<String> buildHologramLines() {
        List<String> lines = new java.util.ArrayList<>();

        // Title - using MiniMessage format
        lines.add("<light_purple>Nucleaire Reactor");

        if (reactor.isExhausted()) {
            // Exhausted state
            long remainingTime = (reactor.getExhaustionEndTime() - System.currentTimeMillis()) / 1000;
            long minutes = remainingTime / 60;
            long seconds = remainingTime % 60;
            lines.add("<red>Uitgeput voor " + minutes + "m " + seconds + "s");
            return lines;
        }

        // Team member counts
        List<String> teamCounts = reactor.getTeamMemberCounts();
        // Dynamically join team counts instead of hardcoding indices
        // This works with any number of required teams (2, 3, 4, etc.)
        String countsLine = String.join(" | ", teamCounts);
        lines.add(countsLine);

        // Progress bar
        String progressBar = buildProgressBar(reactor.getTimerProgress());
        lines.add(progressBar);

        // Percentage
        lines.add("<green>" + reactor.getTimerProgress() + "%");

        // Claiming team name
        String claimingTeam = reactor.getClaimingTeamName();
        if (claimingTeam != null) {
            lines.add("<yellow>" + claimingTeam);
        } else {
            lines.add("<gray>Geen team aan het claimen");
        }

        return lines;
    }

    /**
     * Builds a progress bar with green and red segments
     * Uses MiniMessage format for colors
     */
    private String buildProgressBar(int progress) {
        StringBuilder bar = new StringBuilder();
        int greenSegments = progress;
        int redSegments = 100 - progress;

        String segmentChar = "▏";

        // Add green segments directly adjacent (no spacing)
        // MiniMessage format: <green>text
        bar.append("<green>");
        for (int i = 0; i < greenSegments; i++) {
            bar.append(segmentChar);
        }

        // Add red segments directly adjacent (no spacing)
        // MiniMessage format: <red>text
        bar.append("<red>");
        for (int i = 0; i < redSegments; i++) {
            bar.append(segmentChar);
        }

        return bar.toString();
    }


    /**
     * Deletes the hologram
     */
    public void delete() {
        if (hologram != null) {
            try {
                hologram.delete();
            } catch (Exception e) {
                // Ignore errors during shutdown (DecentHolograms might already be disabled)
                reactor.getModule().getLogger().debug("Could not delete hologram during shutdown: " + e.getMessage());
            }
        }
    }
}
