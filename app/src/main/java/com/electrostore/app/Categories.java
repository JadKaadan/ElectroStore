package com.electrostore.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;


public class Categories {

    public static void manage(final Activity a, final Runnable onChange) {
        final Dialog[] holder = new Dialog[1];

        ScrollView scroll = new ScrollView(a);
        final LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(a, 20);
        list.setPadding(pad, Ui.dp(a, 4), pad, Ui.dp(a, 4));
        scroll.addView(list);

        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = new Runnable() {
            public void run() {
                list.removeAllViews();
                List<String> cats = Db.get(a).categories();

                if (cats.isEmpty()) {
                    TextView empty = new TextView(a);
                    empty.setText(R.string.categories_none);
                    empty.setTextSize(16);
                    empty.setTextColor(Ui.color(a, R.color.ink_faint));
                    empty.setPadding(0, Ui.dp(a, 24), 0, Ui.dp(a, 24));
                    empty.setGravity(Gravity.CENTER);
                    list.addView(empty);
                    return;
                }

                for (final String name : cats) {
                    final int used = Db.get(a).productsInCategory(name);

                    LinearLayout row = new LinearLayout(a);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 10));

                    LinearLayout text = new LinearLayout(a);
                    text.setOrientation(LinearLayout.VERTICAL);

                    TextView title = new TextView(a);
                    title.setText(name);
                    title.setTextSize(17);
                    title.setTypeface(Typeface.DEFAULT_BOLD);
                    title.setTextColor(Ui.color(a, R.color.ink));
                    title.setSingleLine(true);
                    title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    text.addView(title);

                    TextView sub = new TextView(a);
                    sub.setText(a.getString(R.string.category_products_count, String.valueOf(used)));
                    sub.setTextSize(13);
                    sub.setTextColor(Ui.color(a, R.color.ink_faint));
                    text.addView(sub);

                    row.addView(text, new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    ImageView edit = new ImageView(a);
                    edit.setImageResource(R.drawable.ic_edit);
                    Ui.tint(edit, Ui.color(a, R.color.ink_soft));
                    int ep = Ui.dp(a, 11);
                    edit.setPadding(ep, ep, ep, ep);
                    edit.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 42), Ui.dp(a, 42)));
                    edit.setBackgroundResource(R.drawable.key_bg);
                    edit.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            rename(a, name, used, new Runnable() {
                                public void run() {
                                    rebuild[0].run();
                                    if (onChange != null) onChange.run();
                                }
                            });
                        }
                    });
                    row.addView(edit);

                    ImageView del = new ImageView(a);
                    del.setImageResource(R.drawable.ic_trash);
                    Ui.tint(del, Ui.color(a, R.color.red));
                    int p = Ui.dp(a, 10);
                    del.setPadding(p, p, p, p);
                    del.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 42), Ui.dp(a, 42)));
                    del.setBackgroundResource(R.drawable.key_bg);
                    del.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            confirm(a, name, used, new Runnable() {
                                public void run() {
                                    rebuild[0].run();
                                    if (onChange != null) onChange.run();
                                }
                            });
                        }
                    });
                    row.addView(del);
                    list.addView(row);

                    View line = new View(a);
                    line.setBackgroundColor(Ui.color(a, R.color.line));
                    list.addView(line, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                }
            }
        };
        rebuild[0].run();

        holder[0] = new AlertDialog.Builder(a, R.style.AppTheme_Dialog)
                .setTitle(R.string.categories_manage)
                .setView(scroll)
                .setPositiveButton(R.string.kp_done, null)
                .create();
        holder[0].show();
    }


    private static void rename(final Activity a, final String from, final int used,
                               final Runnable after) {
        final EditText input = new EditText(a);
        input.setSingleLine(true);
        input.setText(from);
        input.setSelection(from.length());
        input.setInputType(android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
                | android.text.InputType.TYPE_CLASS_TEXT);
        int pad = Ui.dp(a, 22);
        FrameLayout box = new FrameLayout(a);
        box.setPadding(pad, Ui.dp(a, 10), pad, 0);
        box.addView(input, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(a, R.style.AppTheme_Dialog)
                .setTitle(R.string.category_rename_title)
                .setView(box)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.kp_done, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        String to = input.getText().toString().trim();
                        if (to.length() == 0 || to.equals(from)) return;

                        if (Db.get(a).categoryExists(to)) {


                            Ui.confirm(a, a.getString(R.string.category_merge_title, to),
                                    a.getString(R.string.category_merge_msg,
                                            to, String.valueOf(used), from),
                                    a.getString(R.string.category_merge_yes), new Runnable() {
                                        public void run() { apply(a, from, to, after); }
                                    });
                        } else {
                            apply(a, from, to, after);
                        }
                    }
                })
                .show();
    }

    private static void apply(Activity a, String from, String to, Runnable after) {
        Db.get(a).renameCategory(from, to);
        Ui.toast(a, a.getString(R.string.category_renamed));
        if (after != null) after.run();
    }

    private static void confirm(final Activity a, final String name, int used,
                                final Runnable after) {
        String msg = used > 0
                ? a.getString(R.string.category_delete_used, name, String.valueOf(used))
                : a.getString(R.string.category_delete_empty, name);
        Ui.confirm(a, a.getString(R.string.category_delete_title), msg,
                a.getString(R.string.delete), new Runnable() {
                    public void run() {
                        Db.get(a).deleteCategory(name);
                        Ui.toast(a, a.getString(R.string.category_removed));
                        if (after != null) after.run();
                    }
                });
    }
}
