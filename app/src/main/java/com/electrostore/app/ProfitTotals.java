package com.electrostore.app;

import java.math.BigDecimal;
import java.math.RoundingMode;


public final class ProfitTotals {
    public BigDecimal sold = BigDecimal.ZERO.setScale(2);
    public BigDecimal cost = BigDecimal.ZERO.setScale(2);
    public BigDecimal profit = BigDecimal.ZERO.setScale(2);
    public int pieces, sales, missingCostPieces;

    public void addSale(String total, String unitCost, int quantity) {
        BigDecimal sale = decimal(total).setScale(2, RoundingMode.HALF_UP);
        BigDecimal price = decimal(unitCost);
        sold = sold.add(sale);
        pieces += quantity;
        sales++;
        if (price.signum() > 0) {
            BigDecimal wholesale = price.multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            cost = cost.add(wholesale);
            profit = profit.add(sale.subtract(wholesale));
        } else {

            missingCostPieces += quantity;
        }
    }

    public void add(ProfitTotals other) {
        sold = sold.add(other.sold);
        cost = cost.add(other.cost);
        profit = profit.add(other.profit);
        pieces += other.pieces;
        sales += other.sales;
        missingCostPieces += other.missingCostPieces;
    }

    private static BigDecimal decimal(String value) {
        return value == null || value.length() == 0 ? BigDecimal.ZERO : new BigDecimal(value);
    }
}
