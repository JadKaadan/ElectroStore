package com.electrostore.app;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;


public class Lang {

    public static final String EN = "en";
    public static final String AR = "ar";

    private static final String PREF = "electro_prefs";
    private static final String KEY = "lang";


    public static String current(Context c) {
        try {
            String saved = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .getString(KEY, null);
            if (saved != null) return saved;
            String phone = Locale.getDefault().getLanguage();
            return AR.equals(phone) ? AR : EN;
        } catch (Throwable t) {
            return EN;
        }
    }

    public static void set(Context c, String lang) {
        try {
            c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit().putString(KEY, lang).apply();
        } catch (Throwable ignored) { }
    }

    public static boolean isArabic(Context c) { return AR.equals(current(c)); }


    public static Context wrap(Context base) {
        try {
            Locale locale = new Locale(current(base));
            Locale.setDefault(locale);
            Configuration cfg = new Configuration(base.getResources().getConfiguration());
            cfg.setLocale(locale);
            cfg.setLayoutDirection(locale);
            return base.createConfigurationContext(cfg);
        } catch (Throwable t) {
            return base;
        }
    }
}
