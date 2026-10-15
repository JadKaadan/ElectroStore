package com.electrostore.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class EscPos {


    public static Bitmap render(Context c, Order o, int dotsWide) {
        boolean rtl = c.getResources().getConfiguration().getLayoutDirection()
                == android.view.View.LAYOUT_DIRECTION_RTL;

        int pad = Math.round(dotsWide * 0.045f);
        int w = dotsWide - pad * 2;


        Sheet s = new Sheet(c, w, pad, rtl, null);
        int height = fill(c, s, o);

        Bitmap bm = Bitmap.createBitmap(dotsWide, height + pad, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        canvas.drawColor(Color.WHITE);
        fill(c, new Sheet(c, w, pad, rtl, canvas), o);
        return bm;
    }


    private static int fill(Context c, Sheet s, Order o) {
        String shop = Db.get(c).setting("shop_name", c.getString(R.string.app_name));
        String info = Db.get(c).setting("shop_info", "");

        s.gap(6);
        s.line(shop, s.big, Align.CENTER);
        if (info.trim().length() > 0) s.line(info, s.small, Align.CENTER);

        s.gap(10);
        s.rule();
        s.gap(8);

        s.line(o.receiptNo, s.medBold, Align.CENTER);
        s.line(c.getString(R.string.rc_order_receipt), s.small, Align.CENTER);
        s.gap(8);

        String who = o.customer == null || o.customer.trim().length() == 0
                ? c.getString(R.string.walk_in) : o.customer;
        s.line(who, s.body, Align.START);
        if (o.phone != null && o.phone.trim().length() > 0) {
            s.line(o.phone, s.small, Align.START);
        }
        s.line(new SimpleDateFormat("d MMMM yyyy  ·  h:mm a", Locale.getDefault())
                .format(new Date(o.ts == 0 ? System.currentTimeMillis() : o.ts)),
                s.small, Align.START);

        s.gap(8);
        s.rule();
        s.gap(6);

        for (OrderItem it : o.items) {
            s.row(it.productName, Money.usd(it.lineTotal), s.body);
            s.line(it.qty + " × " + Money.usd(it.unitPrice), s.small, Align.START);
            s.gap(6);
        }

        s.rule();
        s.gap(8);

        s.row(c.getString(R.string.rc_total), Money.usd(o.total), s.medBold);
        s.row("", Money.lbp(c, o.total), s.small);

        if (o.paid > 0.005) {
            s.gap(4);
            s.row(c.getString(R.string.rc_deposit), "- " + Money.usd(o.paid), s.body);
        }
        double bal = o.balance();
        s.gap(6);
        if (bal > 0.005) {
            s.row(c.getString(R.string.rc_to_pay), Money.usd(bal), s.medBold);
            s.row("", Money.lbp(c, bal), s.small);
        } else {
            s.line(c.getString(R.string.rc_paid_full), s.medBold, Align.CENTER);
        }

        if (o.note != null && o.note.trim().length() > 0) {
            s.gap(10);
            s.rule();
            s.gap(6);
            s.wrapped(o.note.trim(), s.small);
        }

        s.gap(14);
        s.line(c.getString(R.string.rc_thanks), s.small, Align.CENTER);
        s.wrappedCentered(c.getString(R.string.rc_keep), s.small);
        s.gap(20);
        return s.y;
    }

    private enum Align { START, CENTER, END }


    private static class Sheet {
        final Canvas canvas;
        final int w, pad;
        final boolean rtl;
        int y = 0;
        final TextPaint big, medBold, body, small;
        final Paint rulePaint;

        Sheet(Context c, int w, int pad, boolean rtl, Canvas canvas) {
            this.canvas = canvas; this.w = w; this.pad = pad; this.rtl = rtl;
            float k = w / 384f;
            big = paint(38 * k, true);
            medBold = paint(30 * k, true);
            body = paint(25 * k, false);
            small = paint(21 * k, false);
            rulePaint = new Paint();
            rulePaint.setColor(Color.BLACK);
            rulePaint.setStrokeWidth(Math.max(1f, 2f * k));
        }

        private TextPaint paint(float size, boolean bold) {
            TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.BLACK);
            p.setTextSize(size);
            p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            return p;
        }

        void gap(int px) { y += px; }

        void rule() {
            if (canvas != null) canvas.drawLine(pad, y, pad + w, y, rulePaint);
            y += 2;
        }

        void line(String text, TextPaint p, Align a) {
            if (text == null) return;
            float tw = p.measureText(text);
            float x;
            Align eff = a;
            if (rtl && a == Align.START) eff = Align.END;
            else if (rtl && a == Align.END) eff = Align.START;
            if (eff == Align.CENTER) x = pad + (w - tw) / 2f;
            else if (eff == Align.END) x = pad + w - tw;
            else x = pad;
            Paint.FontMetrics fm = p.getFontMetrics();
            y += -fm.ascent;
            if (canvas != null) canvas.drawText(text, x, y, p);
            y += fm.descent + 2;
        }


        void row(String left, String right, TextPaint p) {
            Paint.FontMetrics fm = p.getFontMetrics();
            float rw = p.measureText(right);
            float avail = w - rw - 12;
            String l = TextUtils.ellipsize(left == null ? "" : left, p, avail,
                    TextUtils.TruncateAt.END).toString();
            y += -fm.ascent;
            if (canvas != null) {
                if (rtl) {
                    canvas.drawText(l, pad + w - p.measureText(l), y, p);
                    canvas.drawText(right, pad, y, p);
                } else {
                    canvas.drawText(l, pad, y, p);
                    canvas.drawText(right, pad + w - rw, y, p);
                }
            }
            y += fm.descent + 2;
        }

        void wrapped(String text, TextPaint p) { block(text, p, Layout.Alignment.ALIGN_NORMAL); }

        void wrappedCentered(String text, TextPaint p) {
            block(text, p, Layout.Alignment.ALIGN_CENTER);
        }


        private void block(String text, TextPaint p, Layout.Alignment a) {
            StaticLayout sl = new StaticLayout(text, p, w, a, 1.05f, 0f, false);
            if (canvas != null) {
                canvas.save();
                canvas.translate(pad, y);
                sl.draw(canvas);
                canvas.restore();
            }
            y += sl.getHeight() + 2;
        }
    }


    public static byte[] bytes(Bitmap bm) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x1B); out.write(0x40);
        out.write(0x1B); out.write(0x61); out.write(0x00);

        int w = bm.getWidth(), h = bm.getHeight();
        int bytesWide = (w + 7) / 8;


        final int BAND = 128;
        int[] row = new int[w];
        for (int top = 0; top < h; top += BAND) {
            int rows = Math.min(BAND, h - top);
            out.write(0x1D); out.write(0x76); out.write(0x30); out.write(0x00);
            out.write(bytesWide & 0xFF); out.write((bytesWide >> 8) & 0xFF);
            out.write(rows & 0xFF); out.write((rows >> 8) & 0xFF);

            for (int yy = 0; yy < rows; yy++) {
                bm.getPixels(row, 0, w, 0, top + yy, w, 1);
                for (int bx = 0; bx < bytesWide; bx++) {
                    int b = 0;
                    for (int bit = 0; bit < 8; bit++) {
                        int x = bx * 8 + bit;
                        if (x < w && dark(row[x])) b |= (0x80 >> bit);
                    }
                    out.write(b);
                }
            }
        }

        out.write(0x1B); out.write(0x64); out.write(0x04);
        out.write(0x1D); out.write(0x56); out.write(0x01);
        return out.toByteArray();
    }

    private static boolean dark(int argb) {
        if (((argb >> 24) & 0xFF) < 128) return false;
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 < 140;
    }


    public static byte[] testPage(Context c, int dotsWide) {
        int pad = Math.round(dotsWide * 0.045f);
        int w = dotsWide - pad * 2;
        Sheet measure = new Sheet(c, w, pad, false, null);
        int h = testFill(c, measure);
        Bitmap bm = Bitmap.createBitmap(dotsWide, h + pad, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        canvas.drawColor(Color.WHITE);
        testFill(c, new Sheet(c, w, pad, false, canvas));
        byte[] b = bytes(bm);
        bm.recycle();
        return b;
    }

    private static int testFill(Context c, Sheet s) {
        String shop = Db.get(c).setting("shop_name", c.getString(R.string.app_name));
        s.gap(6);
        s.line(shop, s.big, Align.CENTER);
        s.gap(8);
        s.rule();
        s.gap(8);
        s.line(c.getString(R.string.print_test_line), s.body, Align.CENTER);
        s.gap(6);
        s.row("ABCDEFGHIJKLM", "$123.45", s.body);
        s.row("أبجد هوز حطي", "1,000,000", s.body);
        s.gap(8);
        s.rule();
        s.gap(20);
        return s.y;
    }
}
