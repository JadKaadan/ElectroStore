package com.electrostore.app;

import android.content.Context;


public class Cat {

    private static final int[] PALETTE = {
            R.color.tab_products, R.color.green, R.color.orange,
            R.color.cyan, R.color.pink, R.color.amber
    };
    private static final int[] SOFT = {
            R.color.brand_soft, R.color.green_soft, R.color.orange_soft,
            R.color.cyan_soft, R.color.pink_soft, R.color.amber_soft
    };

    private static int slot(String name) {
        if (name == null || name.length() == 0) return 0;
        int h = 0;
        for (int i = 0; i < name.length(); i++) h = h * 31 + name.charAt(i);
        return Math.abs(h) % PALETTE.length;
    }

    public static int color(Context c, String name) {
        return Ui.color(c, PALETTE[slot(name)]);
    }

    public static int soft(Context c, String name) {
        return Ui.color(c, SOFT[slot(name)]);
    }


    public static String initial(String name) {
        if (name == null) return "";
        String s = name.trim();
        return s.length() == 0 ? "" : s.substring(0, 1).toUpperCase();
    }
}
