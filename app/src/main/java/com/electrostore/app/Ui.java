package com.electrostore.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class Ui {


    @SuppressWarnings("deprecation")
    public static int color(Context c, int resId) {
        return c.getResources().getColor(resId);
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }


    public static int keyHeight(Context c) {
        int screenDp = c.getResources().getConfiguration().screenHeightDp;
        int h = Math.round(screenDp * 0.062f);
        if (h < 40) h = 40;
        if (h > 52) h = 52;
        return dp(c, h);
    }

    public static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    public static void tint(ImageView v, int color) {
        v.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    public static void show(View v, boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static void hideKeyboard(Activity a) {
        try {
            View f = a.getCurrentFocus();
            if (f == null) return;
            InputMethodManager im = (InputMethodManager)
                    a.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (im != null) im.hideSoftInputFromWindow(f.getWindowToken(), 0);
        } catch (Throwable ignored) { }
    }

    public interface Watch { void onChanged(String text); }

    public static void onType(EditText e, final Watch w) {
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) { w.onChanged(s.toString()); }
        });
    }

    public static void confirm(Context c, String title, String message,
                               String yes, final Runnable onYes) {
        new AlertDialog.Builder(c, R.style.AppTheme_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(c.getString(R.string.cancel), null)
                .setPositiveButton(yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { onYes.run(); }
                })
                .show();
    }


    public static String prettyDay(String isoDay) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDay);
            return new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(d);
        } catch (Throwable t) {
            return isoDay;
        }
    }


    public static String shortDay(Context c, String isoDay) {
        try {
            String today = Db.today();
            if (isoDay.equals(today)) return c.getString(R.string.today);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String y = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
            if (isoDay.equals(y)) return c.getString(R.string.yesterday);
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDay);
            return new SimpleDateFormat("EEEE d MMM yyyy", Locale.US).format(d);
        } catch (Throwable t) {
            return isoDay;
        }
    }

    public static String dayNumber(String isoDay) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDay);
            return new SimpleDateFormat("d", Locale.US).format(d);
        } catch (Throwable t) { return "?"; }
    }

    public static String monthShort(String isoDay) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDay);
            return new SimpleDateFormat("MMM", Locale.US).format(d).toUpperCase(Locale.US);
        } catch (Throwable t) { return ""; }
    }


    public static String prettyMonth(String isoMonth) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM", Locale.US).parse(isoMonth);
            return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d);
        } catch (Throwable t) { return isoMonth; }
    }

    public static String clock(long ts) {
        return new SimpleDateFormat("h:mm a", Locale.US).format(new Date(ts));
    }

    public static void setOrHide(TextView tv, String text) {
        if (text == null || text.trim().length() == 0) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(text);
        }
    }
}
