package com.electrostore.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ImgUtil {

    private static final int MAX_EDGE = 1280;

    public static File photoDir(Context c) {
        File dir = new File(c.getFilesDir(), "photos");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File newPhotoFile(Context c) {
        return new File(photoDir(c), "p_" + System.currentTimeMillis() + ".jpg");
    }


    public static String importFrom(Context c, Uri src) {
        try {
            InputStream in = c.getContentResolver().openInputStream(src);
            if (in == null) return "";
            File tmp = new File(c.getCacheDir(), "in_" + System.currentTimeMillis());
            OutputStream out = new FileOutputStream(tmp);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.close();
            in.close();
            String path = shrinkInto(c, tmp.getAbsolutePath());
            tmp.delete();
            return path;
        } catch (Throwable t) {
            return "";
        }
    }


    public static String shrinkInto(Context c, String sourcePath) {
        try {
            BitmapFactory.Options probe = new BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(sourcePath, probe);
            int big = Math.max(probe.outWidth, probe.outHeight);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (big / opts.inSampleSize > MAX_EDGE * 2) opts.inSampleSize *= 2;

            Bitmap bm = BitmapFactory.decodeFile(sourcePath, opts);
            if (bm == null) return "";

            int w = bm.getWidth(), h = bm.getHeight();
            float scale = Math.min(1f, (float) MAX_EDGE / Math.max(w, h));
            if (scale < 1f) {
                Bitmap sc = Bitmap.createScaledBitmap(
                        bm, Math.round(w * scale), Math.round(h * scale), true);
                if (sc != bm) bm.recycle();
                bm = sc;
            }

            int rotate = rotationOf(sourcePath);
            if (rotate != 0) {
                Matrix m = new Matrix();
                m.postRotate(rotate);
                Bitmap r = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
                if (r != bm) bm.recycle();
                bm = r;
            }

            File dest = newPhotoFile(c);
            FileOutputStream fos = new FileOutputStream(dest);
            bm.compress(Bitmap.CompressFormat.JPEG, 88, fos);
            fos.close();
            bm.recycle();
            return dest.getAbsolutePath();
        } catch (Throwable t) {
            return "";
        }
    }

    private static int rotationOf(String path) {
        try {
            ExifInterface ex = new ExifInterface(path);
            int o = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            if (o == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (o == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (o == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Throwable ignored) { }
        return 0;
    }


    private static final android.util.LruCache<String, Bitmap> CACHE =
            new android.util.LruCache<String, Bitmap>(6 * 1024 * 1024) {
                @Override protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };


    private static final ExecutorService THUMBNAILS = Executors.newFixedThreadPool(2);


    public static void loadThumb(final ImageView image, final View placeholder,
                                 final String path, final int edge) {
        final String key = (path == null ? "" : path) + "@" + edge;
        image.setTag(key);
        Bitmap hit = CACHE.get(key);
        if (hit != null && !hit.isRecycled()) {
            image.setImageBitmap(hit);
            if (placeholder != null) placeholder.setVisibility(View.GONE);
            return;
        }

        image.setImageDrawable(null);
        if (placeholder != null) placeholder.setVisibility(View.VISIBLE);
        if (path == null || path.length() == 0 || !new File(path).isFile()) return;

        final WeakReference<ImageView> imageRef = new WeakReference<ImageView>(image);
        final WeakReference<View> holderRef = new WeakReference<View>(placeholder);
        final ViewTreeObserver.OnPreDrawListener[] watch =
                new ViewTreeObserver.OnPreDrawListener[1];
        watch[0] = new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                ImageView target = imageRef.get();
                if (target == null) return true;
                if (!key.equals(target.getTag())) {
                    removeWatch(target, watch[0]);
                    return true;
                }
                Rect visible = new Rect();
                if (!target.isShown() || !target.getGlobalVisibleRect(visible)
                        || visible.width() == 0 || visible.height() == 0) return true;
                removeWatch(target, watch[0]);
                THUMBNAILS.execute(new Runnable() {
                    @Override public void run() {
                        final Bitmap bitmap = thumb(path, edge);
                        final ImageView current = imageRef.get();
                        if (current == null) return;
                        current.post(new Runnable() {
                            @Override public void run() {
                                ImageView view = imageRef.get();
                                if (view == null || !key.equals(view.getTag())) return;
                                View holder = holderRef.get();
                                if (bitmap != null) {
                                    view.setImageBitmap(bitmap);
                                    if (holder != null) holder.setVisibility(View.GONE);
                                } else {
                                    view.setImageDrawable(null);
                                    if (holder != null) holder.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }
                });
                return true;
            }
        };
        image.getViewTreeObserver().addOnPreDrawListener(watch[0]);
        image.invalidate();
    }

    private static void removeWatch(View view, ViewTreeObserver.OnPreDrawListener listener) {
        ViewTreeObserver tree = view.getViewTreeObserver();
        if (tree.isAlive()) tree.removeOnPreDrawListener(listener);
    }


    public static Bitmap thumb(String path, int edge) {
        if (path == null || path.length() == 0) return null;
        String key = path + "@" + edge;
        Bitmap hit = CACHE.get(key);
        if (hit != null && !hit.isRecycled()) return hit;
        Bitmap fresh = decode(path, edge);
        if (fresh != null && edge <= 400) CACHE.put(key, fresh);
        return fresh;
    }

    private static Bitmap decode(String path, int edge) {
        if (path == null || path.length() == 0) return null;
        File f = new File(path);
        if (!f.exists()) return null;
        try {
            BitmapFactory.Options probe = new BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, probe);
            int big = Math.max(probe.outWidth, probe.outHeight);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 1;
            while (edge > 0 && big / o.inSampleSize > edge * 2) o.inSampleSize *= 2;
            return BitmapFactory.decodeFile(path, o);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void delete(String path) {
        try {
            if (path != null && path.length() > 0) {
                new File(path).delete();
                CACHE.evictAll();
            }
        } catch (Throwable ignored) { }
    }
}
