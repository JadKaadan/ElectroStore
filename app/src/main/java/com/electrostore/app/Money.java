package com.electrostore.app;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;


public class Money {

    public static final String KEY_RATE = "usd_lbp";
    public static final double DEFAULT_RATE = 89500;

    private static final DecimalFormat USD;
    private static final DecimalFormat LBP;
    private static final DecimalFormat PLAIN;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        USD = new DecimalFormat("#,##0.00", sym);
        LBP = new DecimalFormat("#,##0", sym);
        PLAIN = new DecimalFormat("0.##", sym);
    }


    private static double cachedRate = 0;

    public static double rate(Context c) {
        if (cachedRate > 0) return cachedRate;
        try {
            double r = Double.parseDouble(Db.get(c).setting(KEY_RATE, String.valueOf(DEFAULT_RATE)));
            cachedRate = r > 0 ? r : DEFAULT_RATE;
        } catch (Exception e) {
            cachedRate = DEFAULT_RATE;
        }
        return cachedRate;
    }

    public static void setRate(Context c, double r) {
        Db.get(c).setSetting(KEY_RATE, String.valueOf(r));
        cachedRate = r > 0 ? r : DEFAULT_RATE;
    }


    private static final String LRI = "\u2066";
    private static final String PDI = "\u2069";


    public static String ltr(String s) {
        if (s == null || s.length() == 0) return "";
        return LRI + s + PDI;
    }


    public static String usd(double v) { return ltr("$" + USD.format(v)); }


    public static String lbp(Context c, double usdValue) {
        return ltr(LBP.format(Math.round(usdValue * rate(c))))
                + " " + c.getString(R.string.lbp_suffix);
    }


    public static String both(Context c, double usdValue) {
        return usd(usdValue) + "   ·   " + lbp(c, usdValue);
    }


    public static String plain(double v) {
        if (v == 0) return "";
        return PLAIN.format(v);
    }


    public static double parse(CharSequence text) {
        if (text == null) return 0;
        String s = text.toString().trim().replace(",", "").replace("$", "").replace(" ", "");
        if (s.length() == 0) return 0;
        try {
            double v = Double.parseDouble(s);
            return v < 0 ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }

    public static int parseInt(CharSequence text) {
        if (text == null) return 0;
        String s = text.toString().trim().replace(",", "").replace(" ", "");
        if (s.length() == 0) return 0;
        try {
            int v = (int) Double.parseDouble(s);
            return v < 0 ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }
}
