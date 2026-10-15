package com.electrostore.app;

import android.content.Context;


public class ServiceType {
    public long id;
    public String presetKey = "", name = "", tips = "";
    public boolean active = true;

    public String title(Context c) {
        return name.trim().isEmpty() ? ServiceCatalog.title(c, presetKey) : name;
    }

    public String guide(Context c) {
        return tips.trim().isEmpty() ? ServiceCatalog.guide(c, presetKey) : tips;
    }
}
