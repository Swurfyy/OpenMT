package nl.openminetopia.modules.belasting.calculator;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlotTaxEntry {
    private final String worldName;
    private final String plotId;
    private final long wozValue;
    private final double taxAmount;
}
