package nl.openminetopia.modules.belasting.models;

import com.craftmend.storm.api.StormModel;
import com.craftmend.storm.api.markers.Column;
import com.craftmend.storm.api.markers.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.openminetopia.modules.belasting.enums.InvoiceStatus;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "belasting_invoices")
public class TaxInvoiceModel extends StormModel {

    @Column(name = "player_uuid")
    private UUID playerUuid;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "status", defaultValue = "UNPAID")
    private InvoiceStatus status;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "paid_at")
    private Long paidAt;

    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }
}
