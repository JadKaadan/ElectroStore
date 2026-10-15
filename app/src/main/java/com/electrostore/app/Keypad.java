package com.electrostore.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


public class Keypad {

    public interface OnValue { void set(double value); }


    public interface Echo { String of(double typed); }

    private static final String[][] KEYS = {
            {"1", "2", "3"},
            {"4", "5", "6"},
            {"7", "8", "9"},
            {".", "0", "<"}
    };

    public static void open(final Activity a, final String label, double start,
                            final boolean decimals, final Echo echoer, final OnValue cb) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_keypad, null);

        final TextView value = (TextView) v.findViewById(R.id.kpValue);
        final TextView echo = (TextView) v.findViewById(R.id.kpEcho);
        ((TextView) v.findViewById(R.id.kpLabel)).setText(label);
        v.findViewById(R.id.kpUsd).setVisibility(View.GONE);
        v.findViewById(R.id.kpLbp).setVisibility(View.GONE);

        final String[] typed = {start > 0 ? Money.plain(start) : ""};

        final Runnable paint = new Runnable() {
            public void run() {
                value.setText(typed[0].length() == 0 ? "0" : typed[0]);
                echo.setText(echoer == null ? "" : echoer.of(Money.parse(typed[0])));
            }
        };

        LinearLayout grid = (LinearLayout) v.findViewById(R.id.kpGrid);
        for (String[] rowKeys : KEYS) {
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            grid.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (final String k : rowKeys) {
                final boolean isBack = "<".equals(k);
                TextView key = new TextView(a);
                key.setText(isBack ? "⌫" : k);
                key.setTextSize(isBack ? 20 : 23);
                key.setTypeface(Typeface.DEFAULT_BOLD);
                key.setGravity(Gravity.CENTER);
                key.setTextColor(Ui.color(a, R.color.ink));
                key.setBackgroundResource(R.drawable.key_bg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.keyHeight(a), 1f);
                lp.bottomMargin = Ui.dp(a, 3);
                key.setLayoutParams(lp);

                if (".".equals(k) && !decimals) {
                    key.setText("");
                    key.setClickable(false);
                } else {
                    key.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View x) {
                            typed[0] = press(typed[0], k, decimals);
                            paint.run();
                        }
                    });
                    if (isBack) {
                        key.setOnLongClickListener(new View.OnLongClickListener() {
                            public boolean onLongClick(View x) {
                                typed[0] = "";
                                paint.run();
                                return true;
                            }
                        });
                    }
                }
                row.addView(key);
            }
        }

        paint.run();

        final Dialog dlg = new Dialog(a, R.style.SheetDialog);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setContentView(v);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
        }

        v.findViewById(R.id.kpCancel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) { dlg.dismiss(); }
        });
        v.findViewById(R.id.kpDone).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                double n = Money.parse(typed[0]);
                dlg.dismiss();
                cb.set(n);
            }
        });
        dlg.show();
    }


    static String press(String cur, String k, boolean decimals) {
        if ("<".equals(k)) {
            return cur.length() == 0 ? cur : cur.substring(0, cur.length() - 1);
        }
        if (".".equals(k)) {
            if (!decimals || cur.indexOf('.') >= 0) return cur;
            return cur.length() == 0 ? "0." : cur + ".";
        }
        if (cur.equals("0")) cur = "";
        int dot = cur.indexOf('.');
        if (dot >= 0 && cur.length() - dot > 2) return cur;
        if (cur.length() >= 12) return cur;
        return cur + k;
    }


    public static void attach(final Activity a, final EditText box, final String label,
                              final boolean decimals, final Echo echoer,
                              final Runnable afterChange) {
        box.setFocusable(false);
        box.setFocusableInTouchMode(false);
        box.setCursorVisible(false);
        box.setLongClickable(false);
        box.setKeyListener(null);
        box.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                open(a, label, Money.parse(box.getText()), decimals, echoer, new OnValue() {
                    public void set(double value) {
                        box.setText(value <= 0 ? "" : Money.plain(value));
                        if (afterChange != null) afterChange.run();
                    }
                });
            }
        });
    }


    public static void attachWhole(Activity a, EditText box, String label, Runnable afterChange) {
        attach(a, box, label, false, null, afterChange);
    }
}
