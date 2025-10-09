package nl.openminetopia.modules.banking.models;

import lombok.Data;
import org.bukkit.Location;

import java.util.UUID;

@Data
public class PinRequestModel {

    private UUID sellerUuid;
    private UUID buyerUuid;
    private double amount;
    private UUID buyerAccountId;
    private Location terminalLocation;
    private long createdAt;

    public PinRequestModel(UUID sellerUuid, UUID buyerUuid, double amount, UUID buyerAccountId) {
        this.sellerUuid = sellerUuid;
        this.buyerUuid = buyerUuid;
        this.amount = amount;
        this.buyerAccountId = buyerAccountId;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        // Request expires after 5 minutes
        return System.currentTimeMillis() - createdAt > 300000;
    }

    public boolean hasTerminalLocation() {
        return terminalLocation != null;
    }
}

