package com.electrostore.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class Backup {

    private static final String DB_ENTRY = "store.db";
    private static final String PHOTO_DIR = "photos/";


    public static class Info {
        public int products, sales, orders, photos, jobs, services, customers;
        public boolean valid;
        public boolean hasPhotoFolder;

        public boolean isEmpty() { return products == 0 && sales == 0 && orders == 0 && jobs == 0 && services == 0 && customers == 0; }

        private String maintenanceSummary(Context c) {
            return c.getString(R.string.desk_backup, jobs, services, customers);
        }

        public String summary(Context c) {
            if (hasPhotoFolder) {
                return c.getString(R.string.backup_contents_photos,
                        String.valueOf(products), String.valueOf(sales),
                        String.valueOf(orders), String.valueOf(photos)) + maintenanceSummary(c);
            }
            return c.getString(R.string.backup_contents,
                    String.valueOf(products), String.valueOf(sales), String.valueOf(orders)) + maintenanceSummary(c);
        }
    }


    private static void checkpoint(Context c) {
        SQLiteDatabase db = Db.get(c).getWritableDatabase();
        Cursor cur = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null);
        try {


            if (!cur.moveToFirst() || cur.getInt(0) != 0)
                throw new IllegalStateException("Database is busy; retry backup");
        } finally { cur.close(); }
    }

    public static void send(Activity a) {
        try {
            checkpoint(a);

            File src = a.getDatabasePath(Db.FILE);
            if (!src.exists()) {
                Ui.toast(a, a.getString(R.string.backup_none));
                return;
            }

            String stamp = new SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(new Date());
            File zip = new File(PhotoProvider.shareDir(a), "imad-computers-" + stamp + ".zip");

            int photoCount = writeZip(a, src, zip);


            Info info = countLive(a);
            if (!info.valid) {
                Ui.toast(a, a.getString(R.string.backup_failed));
                return;
            }
            info.photos = photoCount;
            info.hasPhotoFolder = true;

            Uri uri = PhotoProvider.uriFor(zip);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("application/zip");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, a.getString(R.string.backup_subject, stamp));
            i.putExtra(Intent.EXTRA_TEXT, info.summary(a));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(Intent.createChooser(i, a.getString(R.string.backup_send_to)));

            Ui.toast(a, info.summary(a));
        } catch (Throwable t) {
            Ui.toast(a, a.getString(R.string.backup_failed));
        }
    }


    private static Info countLive(Context c) {
        Info i = new Info();
        try {
            SQLiteDatabase db = Db.get(c).getReadableDatabase();
            i.products = count(db, "products");
            i.sales = count(db, "sales");
            i.orders = count(db, "orders");
            i.jobs = optionalCount(db, "service_jobs");
            i.services = optionalCount(db, "services");
            i.customers = optionalCount(db, "service_customers");
            i.valid = i.products >= 0 && i.sales >= 0 && i.orders >= 0;
        } catch (Throwable t) {
            i.valid = false;
        }
        return i;
    }


    private static int writeZip(Context c, File db, File dest) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest));
        try {
            zos.putNextEntry(new ZipEntry(DB_ENTRY));
            pump(new FileInputStream(db), zos, true, false);
            zos.closeEntry();

            int n = 0;
            File[] photos = ImgUtil.photoDir(c).listFiles();
            if (photos != null) {
                for (File p : photos) {
                    if (!p.isFile()) continue;
                    zos.putNextEntry(new ZipEntry(PHOTO_DIR + p.getName()));
                    pump(new FileInputStream(p), zos, true, false);
                    zos.closeEntry();
                    n++;
                }
            }
            return n;
        } finally {
            try { zos.close(); } catch (Throwable ignored) { }
        }
    }


    public static File stage(Context c, Uri uri) throws Exception {
        File tmp = new File(c.getCacheDir(), "restore_candidate");
        InputStream in = c.getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("cannot open");
        pump(in, new FileOutputStream(tmp), true, true);
        return tmp;
    }

    private static boolean looksLikeZip(File f) {
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            byte[] head = new byte[4];
            if (in.read(head) < 4) return false;
            return head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4;
        } catch (Throwable t) {
            return false;
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignored) { }
        }
    }


    public static Info inspect(Context c, File staged, File workDir) {
        Info info = new Info();
        try {
            wipe(workDir);
            workDir.mkdirs();
            File db = new File(workDir, DB_ENTRY);
            File photos = new File(workDir, "photos");

            if (looksLikeZip(staged)) {
                info.hasPhotoFolder = true;
                photos.mkdirs();
                ZipInputStream zis = new ZipInputStream(new FileInputStream(staged));
                try {
                    ZipEntry e;
                    while ((e = zis.getNextEntry()) != null) {
                        String name = e.getName();
                        if (e.isDirectory()) continue;
                        if (DB_ENTRY.equals(name)) {
                            pump(zis, new FileOutputStream(db), false, true);
                        } else if (name.startsWith(PHOTO_DIR)) {
                            String base = new File(name).getName();
                            if (base.length() == 0) continue;
                            pump(zis, new FileOutputStream(new File(photos, base)), false, true);
                            info.photos++;
                        }
                        zis.closeEntry();
                    }
                } finally {
                    try { zis.close(); } catch (Throwable ignored) { }
                }
                if (!db.exists()) { info.valid = false; return info; }
            } else {

                info.hasPhotoFolder = false;
                copy(staged, db);
            }

            countInto(info, db);
        } catch (Throwable t) {
            info.valid = false;
        }
        return info;
    }

    private static void countInto(Info info, File db) {
        SQLiteDatabase h = null;
        try {


            h = SQLiteDatabase.openDatabase(db.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READWRITE);
            info.products = count(h, "products");
            info.sales = count(h, "sales");
            info.orders = count(h, "orders");
            info.jobs = optionalCount(h, "service_jobs");
            info.services = optionalCount(h, "services");
            info.customers = optionalCount(h, "service_customers");
            info.valid = info.products >= 0 && info.sales >= 0 && info.orders >= 0;
        } catch (Throwable t) {
            info.valid = false;
        } finally {
            if (h != null) try { h.close(); } catch (Throwable ignored) { }
        }
    }

    private static int optionalCount(SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[]{table});
        try { if (!c.moveToFirst()) return 0; } finally { c.close(); }
        return Math.max(0, count(db, table));
    }

    private static int count(SQLiteDatabase db, String table) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
            return c.moveToFirst() ? c.getInt(0) : -1;
        } catch (Throwable t) {
            return -1;
        } finally {
            if (c != null) c.close();
        }
    }


    public static void install(Context c, File workDir) throws Exception {
        Db.reset();

        File dest = c.getDatabasePath(Db.FILE);
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();


        new File(dest.getAbsolutePath() + "-wal").delete();
        new File(dest.getAbsolutePath() + "-shm").delete();
        new File(dest.getAbsolutePath() + "-journal").delete();

        copy(new File(workDir, DB_ENTRY), dest);

        File photos = new File(workDir, "photos");
        File[] incoming = photos.listFiles();
        if (incoming != null) {
            File target = ImgUtil.photoDir(c);
            for (File p : incoming) {
                if (p.isFile()) copy(p, new File(target, p.getName()));
            }
        }

        repointPhotos(c);
        wipe(workDir);
    }


    private static void repointPhotos(Context c) {
        SQLiteDatabase db = null;
        try {
            db = Db.get(c).getWritableDatabase();
            List<long[]> ids = new ArrayList<long[]>();
            List<String> paths = new ArrayList<String>();

            Cursor cur = db.rawQuery(
                    "SELECT id, image_path FROM products WHERE image_path IS NOT NULL"
                    + " AND image_path <> ''", null);
            try {
                while (cur.moveToNext()) {
                    ids.add(new long[]{cur.getLong(0)});
                    paths.add(cur.getString(1));
                }
            } finally { cur.close(); }

            String dir = ImgUtil.photoDir(c).getAbsolutePath();
            for (int i = 0; i < ids.size(); i++) {
                String base = new File(paths.get(i)).getName();
                String fixed = dir + File.separator + base;
                if (!fixed.equals(paths.get(i))) {
                    db.execSQL("UPDATE products SET image_path=? WHERE id=?",
                            new Object[]{fixed, ids.get(i)[0]});
                }
            }
        } catch (Throwable ignored) {
        }
    }


    private static void pump(InputStream in, OutputStream out,
                             boolean closeIn, boolean closeOut) throws Exception {
        try {
            byte[] buf = new byte[32768];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
        } finally {
            if (closeOut) try { out.close(); } catch (Throwable ignored) { }
            if (closeIn) try { in.close(); } catch (Throwable ignored) { }
        }
    }

    private static void copy(File from, File to) throws Exception {
        FileInputStream in = new FileInputStream(from);
        FileOutputStream out = new FileOutputStream(to);
        byte[] buf = new byte[32768];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        out.flush();
        out.close();
        in.close();
    }

    public static void wipe(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] kids = dir.listFiles();
        if (kids != null) for (File k : kids) {
            if (k.isDirectory()) wipe(k); else k.delete();
        }
        dir.delete();
    }
}
