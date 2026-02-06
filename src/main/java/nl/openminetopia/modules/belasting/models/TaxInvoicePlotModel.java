package nl.openminetopia.modules.belasting.models;

import com.craftmend.storm.api.StormModel;
import com.craftmend.storm.api.markers.Column;
import com.craftmend.storm.api.markers.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "belasting_invoice_plots")
public class TaxInvoicePlotModel extends StormModel {

    @Column(name = "invoice_id")
    private Integer invoiceId;

    @Column(name = "world_name")
    private String worldName;

    @Column(name = "plot_id")
    private String plotId;

    @Column(name = "woz_value")
    private Long wozValue;

    @Column(name = "tax_amount")
    private Double taxAmount;
}
