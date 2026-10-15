package com.electrostore.app;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;

final class LegacyMaintenanceSchema {
    static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE services (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "preset_key TEXT UNIQUE, name TEXT NOT NULL DEFAULT '', tips TEXT NOT NULL DEFAULT '',"
                + "active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE service_jobs (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "service_id INTEGER NOT NULL DEFAULT 0, preset_key TEXT NOT NULL DEFAULT '',"
                + "name_en TEXT NOT NULL DEFAULT '', name_ar TEXT NOT NULL DEFAULT '',"
                + "tips_en TEXT NOT NULL DEFAULT '', tips_ar TEXT NOT NULL DEFAULT '',"
                + "customer TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '',"
                + "device TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '',"
                + "due_day TEXT NOT NULL DEFAULT '', status TEXT NOT NULL DEFAULT 'RECEIVED',"
                + "checked_steps INTEGER NOT NULL DEFAULT 0, fee REAL NOT NULL DEFAULT 0,"
                + "paid REAL NOT NULL DEFAULT 0, created_at INTEGER NOT NULL DEFAULT 0,"
                + "updated_at INTEGER NOT NULL DEFAULT 0, message_opened_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_jobs_status_due ON service_jobs(status,due_day)");
        db.execSQL("CREATE INDEX idx_jobs_service ON service_jobs(service_id)");
        for (String key : ServiceCatalog.KEYS) {
            ContentValues v = new ContentValues(); v.put("preset_key", key);
            db.insertOrThrow("services", null, v);
        }
    }

}
