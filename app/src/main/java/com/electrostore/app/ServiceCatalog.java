package com.electrostore.app;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import java.util.Locale;


public final class ServiceCatalog {
    public static final String[] KEYS = {"format", "apps", "diagnosis", "repair",
            "cleanup", "backup", "security", "upgrade", "screen", "battery", "printer", "wifi"};
    private static final int[] TITLES = {R.string.svc_format, R.string.svc_apps,
            R.string.svc_diagnosis, R.string.svc_repair, R.string.svc_cleanup,
            R.string.svc_backup, R.string.svc_security, R.string.svc_upgrade,
            R.string.svc_screen, R.string.svc_battery, R.string.svc_printer, R.string.svc_wifi};
    private static final int[] GUIDES = {R.array.tips_format, R.array.tips_apps,
            R.array.tips_diagnosis, R.array.tips_repair, R.array.tips_cleanup,
            R.array.tips_backup, R.array.tips_security, R.array.tips_upgrade,
            R.array.tips_screen, R.array.tips_battery, R.array.tips_printer, R.array.tips_wifi};
    private static final int[] ICONS = {R.drawable.ic_svc_laptop, R.drawable.ic_svc_apps,
            R.drawable.ic_search, R.drawable.ic_maintenance, R.drawable.ic_svc_clean,
            R.drawable.ic_svc_backup, R.drawable.ic_svc_shield, R.drawable.ic_svc_chip,
            R.drawable.ic_svc_phone, R.drawable.ic_svc_battery, R.drawable.ic_print, R.drawable.ic_svc_wifi};

    private static int index(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return i;
        return -1;
    }
    public static String title(Context c, String key) {
        int i = index(key);
        return c.getString(i < 0 ? R.string.svc_custom : TITLES[i]);
    }
    public static String guide(Context c, String key) {
        int i = index(key);
        return TextUtils.join("\n", c.getResources().getStringArray(i < 0 ? R.array.tips_custom : GUIDES[i]));
    }
    public static int icon(String key) {
        int i = index(key);
        return i < 0 ? R.drawable.ic_maintenance : ICONS[i];
    }
    public static Context locale(Context c, String lang) {
        Configuration cfg = new Configuration(c.getResources().getConfiguration());
        cfg.setLocale(new Locale(lang));
        return c.createConfigurationContext(cfg);
    }
}
