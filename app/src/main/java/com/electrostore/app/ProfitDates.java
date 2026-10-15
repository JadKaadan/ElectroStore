package com.electrostore.app;

import java.util.Calendar;
import java.util.Locale;


public final class ProfitDates {
    private ProfitDates() { }
    public static int currentYear() { return Calendar.getInstance().get(Calendar.YEAR); }
    public static String month(int year, int zeroBasedMonth) {
        if (year < 1 || year > 9998 || zeroBasedMonth < 0 || zeroBasedMonth > 11)
            throw new IllegalArgumentException("Invalid calendar month");
        return String.format(Locale.US, "%04d-%02d", year, zeroBasedMonth + 1);
    }
    public static String yearStart(int year) { return month(year, 0) + "-01"; }
    public static String nextMonth(String month) {
        int year = Integer.parseInt(month.substring(0, 4));
        int next = Integer.parseInt(month.substring(5, 7));
        return month(next == 12 ? year + 1 : year, next == 12 ? 0 : next) + "-01";
    }
    public static String nextDay(String day) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(day.substring(0, 4)), Integer.parseInt(day.substring(5, 7)) - 1,
                Integer.parseInt(day.substring(8, 10)), 12, 0);
        c.add(Calendar.DATE, 1);
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }
}
