package com.electrostore.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.security.MessageDigest;
import java.security.SecureRandom;


public class Lock {

    private static final String KEY_HASH = "pin_hash";
    private static final String KEY_SALT = "pin_salt";


    public static boolean isSet(Context c) {
        return Db.get(c).setting(KEY_HASH, "").length() > 0;
    }

    public static void save(Context c, String pin) {
        String salt = Db.get(c).setting(KEY_SALT, "");
        if (salt.length() == 0) {
            byte[] b = new byte[16];
            new SecureRandom().nextBytes(b);
            salt = hex(b);
            Db.get(c).setSetting(KEY_SALT, salt);
        }
        Db.get(c).setSetting(KEY_HASH, hash(salt, pin));
    }

    public static void clear(Context c) {
        Db.get(c).setSetting(KEY_HASH, "");
    }

    private static boolean matches(Context c, String pin) {
        String salt = Db.get(c).setting(KEY_SALT, "");
        String want = Db.get(c).setting(KEY_HASH, "");
        return want.length() > 0 && want.equals(hash(salt, pin));
    }

    private static String hash(String salt, String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest((salt + "|" + pin).getBytes("UTF-8")));
        } catch (Throwable t) {
            return "";
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }


    public static void require(final Activity a, final Runnable onOk) {
        if (!isSet(a)) {
            onOk.run();
            return;
        }
        ask(a, a.getString(R.string.pin_title_enter), a.getString(R.string.pin_sub_enter),
                new OnPin() {
                    public boolean got(String pin) {
                        if (!matches(a, pin)) return false;
                        onOk.run();
                        return true;
                    }
                });
    }

    public interface OnPin {

        boolean got(String pin);
    }


    public static void ask(final Activity a, String title, String sub, final OnPin cb) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_pin, null);
        ((TextView) v.findViewById(R.id.pinTitle)).setText(title);
        ((TextView) v.findViewById(R.id.pinSub)).setText(sub);
        final TextView error = (TextView) v.findViewById(R.id.pinError);
        final LinearLayout dots = (LinearLayout) v.findViewById(R.id.pinDots);
        final StringBuilder typed = new StringBuilder();

        final View[] dotViews = new View[4];
        for (int i = 0; i < 4; i++) {
            View d = new View(a);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dp(a, 15), Ui.dp(a, 15));
            lp.leftMargin = lp.rightMargin = Ui.dp(a, 9);
            d.setLayoutParams(lp);
            d.setBackgroundResource(R.drawable.pin_dot_empty);
            dots.addView(d);
            dotViews[i] = d;
        }

        final Dialog dlg = new Dialog(a, R.style.SheetDialog);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setContentView(v);
        dlg.setCanceledOnTouchOutside(false);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
        }

        final Runnable paint = new Runnable() {
            public void run() {
                for (int i = 0; i < 4; i++) {
                    dotViews[i].setBackgroundResource(i < typed.length()
                            ? R.drawable.pin_dot_full : R.drawable.pin_dot_empty);
                }
            }
        };

        String[][] keys = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"", "0", "<"}};
        LinearLayout grid = (LinearLayout) v.findViewById(R.id.pinGrid);
        for (String[] rowKeys : keys) {
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            grid.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            for (final String k : rowKeys) {
                TextView key = new TextView(a);
                key.setText("<".equals(k) ? "⌫" : k);
                key.setTextSize("<".equals(k) ? 20 : 23);
                key.setTypeface(Typeface.DEFAULT_BOLD);
                key.setGravity(Gravity.CENTER);
                key.setTextColor(Ui.color(a, R.color.ink));
                key.setBackgroundResource(R.drawable.key_bg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.keyHeight(a), 1f);
                lp.bottomMargin = Ui.dp(a, 3);
                key.setLayoutParams(lp);
                if (k.length() > 0) {
                    key.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View x) {
                            error.setVisibility(View.GONE);
                            if ("<".equals(k)) {
                                if (typed.length() > 0) typed.setLength(typed.length() - 1);
                            } else if (typed.length() < 4) {
                                typed.append(k);
                            }
                            paint.run();
                            if (typed.length() == 4) {
                                final String pin = typed.toString();
                                v.postDelayed(new Runnable() {
                                    public void run() {
                                        if (cb.got(pin)) {
                                            dlg.dismiss();
                                        } else {
                                            typed.setLength(0);
                                            paint.run();
                                            error.setText(R.string.pin_wrong);
                                            error.setVisibility(View.VISIBLE);
                                            shake(dots);
                                        }
                                    }
                                }, 90);
                            }
                        }
                    });
                }
                row.addView(key);
            }
        }
        paint.run();

        v.findViewById(R.id.pinCancel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) { dlg.dismiss(); }
        });
        dlg.show();
    }

    private static void shake(View v) {
        v.animate().translationX(Ui.dp(v.getContext(), 10)).setDuration(50)
                .withEndAction(new Runnable() {
                    public void run() {
                        v.animate().translationX(-Ui.dp(v.getContext(), 10)).setDuration(50)
                                .withEndAction(new Runnable() {
                                    public void run() {
                                        v.animate().translationX(0).setDuration(50).start();
                                    }
                                }).start();
                    }
                }).start();
    }
}
