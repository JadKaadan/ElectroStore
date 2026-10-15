package com.electrostore.app;

import java.math.BigDecimal;
import java.util.TimeZone;


public final class ProfitChecks {
    private static int checks;
    private static void equal(Object actual,Object expected,String name) {
        if(!actual.equals(expected)) throw new AssertionError(name+": "+actual+" != "+expected);
        checks++; System.out.println("PASS "+name);
    }
    private static void money(BigDecimal actual,String expected,String name) {
        equal(actual,new BigDecimal(expected),name);
    }
    public static void main(String[] args) {
        ProfitTotals august=new ProfitTotals(); august.addSale("700","600",1);
        money(august.profit,"100.00","$700 sale less $600 historical cost");
        ProfitTotals september=new ProfitTotals(); september.addSale("1400","600",2);
        money(september.profit,"200.00","quantity multiplies cost exactly once");
        september.addSale("80","100",1);
        money(september.profit,"180.00","losses reduce profit");
        september.addSale("75",null,1); september.addSale("50","0",2);
        money(september.profit,"180.00","unknown costs do not become free stock");
        equal(september.missingCostPieces,3,"null and zero cost pieces flagged");
        money(september.sold,"1605.00","selling totals include missing-cost sales");
        money(september.cost,"1300.00","only known historical costs included");
        ProfitTotals year=new ProfitTotals(); year.add(august); year.add(september);
        money(year.profit,"280.00","annual profit equals sum of recorded months");
        equal(year.sales,5,"year counts recorded sale rows only");
        ProfitTotals empty=new ProfitTotals(); money(empty.profit,"0.00","empty next year begins with zero recorded profit");
        equal(empty.sales,0,"no records distinguishable from breakeven");
        ProfitTotals decimal=new ProfitTotals(); for(int i=0;i<1000;i++) decimal.addSale("0.30","0.10",1);
        money(decimal.profit,"200.00","1000 decimal sales have no floating point drift");
        ProfitTotals discount=new ProfitTotals(); discount.addSale("19.99","4.25",3);
        money(discount.profit,"7.24","actual sale total used, including discounts");
        ProfitTotals rounding=new ProfitTotals();rounding.addSale("1.005","0.335",2);
        money(rounding.sold,"1.01","sale line rounds to cents half-up");
        money(rounding.cost,"0.67","extended cost rounds after multiplying quantity");
        money(rounding.profit,"0.34","rounded sold minus rounded cost reconciles");
        equal(ProfitDates.yearStart(2026),"2026-01-01","year lower bound");
        equal(ProfitDates.yearStart(2027),"2027-01-01","year upper bound excludes next year");
        equal(ProfitDates.nextMonth("2026-12"),"2027-01-01","December rolls to next January");
        equal(ProfitDates.nextMonth("2026-08"),"2026-09-01","August month boundary");
        for(String zone:new String[]{"UTC","Asia/Beirut","America/New_York"}) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            equal(ProfitDates.nextDay("2028-02-28"),"2028-02-29","leap day "+zone);
            equal(ProfitDates.nextDay("2028-02-29"),"2028-03-01","end of leap February "+zone);
            equal(ProfitDates.nextDay("2026-12-31"),"2027-01-01","New Year day boundary "+zone);
            equal(ProfitDates.nextDay("2026-03-29"),"2026-03-30","DST calendar-day boundary "+zone);
        }
        System.out.println(checks+" profit/date checks passed.");
    }
}
