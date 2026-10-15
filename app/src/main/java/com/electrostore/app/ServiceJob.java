package com.electrostore.app;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ServiceJob {
    public static final String[] STATUSES = {"RECEIVED", "WORKING", "WAITING", "READY", "COLLECTED", "CANCELLED"};
    private static final int[] STATUS_LABELS = {R.string.job_received, R.string.job_working,
            R.string.job_waiting, R.string.job_ready, R.string.job_collected, R.string.job_cancelled};
    public long id, customerId, serviceId, createdAt, updatedAt, messageOpenedAt;
    public String presetKey = "", nameEn = "", nameAr = "", tipsEn = "", tipsAr = "";
    public String customer = "", phone = "", device = "", notes = "", dueDay = "";
    public String status = "RECEIVED";
    public long checkedSteps;
    public double fee, paid;

    public void snapshot(Context c, ServiceType s) {
        serviceId = s.id;
        presetKey = s.presetKey;
        Context en = ServiceCatalog.locale(c, "en"), ar = ServiceCatalog.locale(c, "ar");
        nameEn = s.title(en); nameAr = s.title(ar);
        tipsEn = s.guide(en); tipsAr = s.guide(ar);
        checkedSteps = 0;
    }
    public String title(Context c) { return Lang.isArabic(c) ? nameAr : nameEn; }
    public String reference() { return String.format(Locale.US, "M-%04d", id); }
    public boolean active() { return !"COLLECTED".equals(status) && !"CANCELLED".equals(status); }
    public boolean overdue() { return active() && !dueDay.isEmpty() && dueDay.compareTo(Db.today()) < 0; }
    public double balance() { return Math.max(0, fee - paid); }
    public List<String> steps(Context c) {
        List<String> out = new ArrayList<String>();
        for (String line : (Lang.isArabic(c) ? tipsAr : tipsEn).split("\n"))
            if (!line.trim().isEmpty() && out.size() < 30) out.add(line.trim());
        return out;
    }
    public static int statusIndex(String status) {
        for (int i = 0; i < STATUSES.length; i++) if (STATUSES[i].equals(status)) return i;
        return 0;
    }
    public static String statusLabel(Context c, String status) { return c.getString(STATUS_LABELS[statusIndex(status)]); }
    public static int statusColor(String status) {
        if ("READY".equals(status) || "COLLECTED".equals(status)) return R.color.desk_accent;
        if ("WAITING".equals(status)) return R.color.amber;
        if ("CANCELLED".equals(status)) return R.color.red;
        return R.color.ink;
    }
}
