package com.electrostore.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;


public class PhotoProvider extends ContentProvider {

    public static final String AUTHORITY = "com.electrostore.app.photos";

    public static Uri uriFor(File file) {
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(file.getName())
                .build();
    }


    public static File shareDir(android.content.Context c) {
        File dir = new File(c.getFilesDir(), "share");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private File resolve(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name == null) return null;
        name = new File(name).getName();
        File inPhotos = new File(ImgUtil.photoDir(getContext()), name);
        if (inPhotos.exists()) return inPhotos;
        File inShare = new File(shareDir(getContext()), name);
        if (inShare.exists()) return inShare;
        return inPhotos;
    }

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        File f = resolve(uri);
        if (f == null) return null;
        int flags = ParcelFileDescriptor.MODE_READ_ONLY;
        if (mode != null && (mode.contains("w") || mode.contains("a"))) {
            flags = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
        }
        try {
            return ParcelFileDescriptor.open(f, flags);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String sel, String[] selArgs, String sort) {
        File f = resolve(uri);
        if (f == null) return null;
        MatrixCursor c = new MatrixCursor(
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.length()});
        return c;
    }

    @Override
    public String getType(Uri uri) {
        String n = uri.getLastPathSegment();
        if (n != null && n.endsWith(".zip")) return "application/zip";
        if (n != null && n.endsWith(".db")) return "application/octet-stream";
        return "image/jpeg";
    }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String sel, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String sel, String[] args) { return 0; }
}
