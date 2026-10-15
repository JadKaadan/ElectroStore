package com.electrostore.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


final class ServiceUi {
    static LinearLayout column(Context c) {
        LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v;
    }
    static LinearLayout row(Context c) {
        LinearLayout v = new LinearLayout(c); v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    static LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1, -2); }
    static LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1f); }
    static void pad(View v, int n) {
        int p = Ui.dp(v.getContext(), n); v.setPadding(p, p, p, p);
    }
    static GradientDrawable background(Context c, int color, int radius, boolean stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Ui.color(c, color)); d.setCornerRadius(Ui.dp(c, radius));
        if (stroke) d.setStroke(Ui.dp(c, 1), Ui.color(c, R.color.line));
        return d;
    }
    static TextView text(Context c, String value, int size, int color, boolean bold) {
        TextView t = new TextView(c); t.setText(value); t.setTextSize(size);
        t.setTextColor(Ui.color(c, color));
        if (bold) t.setTypeface(t.getTypeface(), Typeface.BOLD);
        return t;
    }
    static TextView label(LinearLayout parent, String value) {
        TextView t = text(parent.getContext(), value, 13, R.color.desk_soft, true);
        LinearLayout.LayoutParams lp = match(); lp.topMargin = Ui.dp(parent.getContext(), 18);
        lp.bottomMargin = Ui.dp(parent.getContext(), 7); parent.addView(t, lp); return t;
    }
    static TextView body(LinearLayout parent, String value) {
        TextView t = text(parent.getContext(), value, 14, R.color.desk_soft, false);
        t.setLineSpacing(Ui.dp(parent.getContext(), 3), 1f); parent.addView(t, match()); return t;
    }
    static void gap(LinearLayout parent, int height) {
        parent.addView(new View(parent.getContext()), new LinearLayout.LayoutParams(1, Ui.dp(parent.getContext(), height)));
    }
    static Button button(Context c, String value, boolean primary, View.OnClickListener click) {
        Button b = new Button(c); b.setText(value); b.setAllCaps(false); b.setTextSize(14);
        b.setTypeface(b.getTypeface(), Typeface.BOLD); b.setMinHeight(Ui.dp(c, 48));
        b.setMinimumWidth(0); b.setMinWidth(0); b.setStateListAnimator(null);
        b.setTextColor(Ui.color(c, primary ? R.color.desk_surface : R.color.desk_ink));
        b.setBackground(background(c, primary ? R.color.desk_accent : R.color.desk_surface, 14, false));
        b.setPadding(Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
        b.setOnClickListener(click); return b;
    }
    static ImageView icon(Context c, int drawable, String description, View.OnClickListener click) {
        ImageView v = new ImageView(c); v.setImageResource(drawable);
        Ui.tint(v, Ui.color(c, R.color.ink)); pad(v, 12);
        v.setContentDescription(description); v.setOnClickListener(click);
        v.setFocusable(click != null); return v;
    }
    static void header(LinearLayout root, String title, String subtitle, boolean back, View.OnClickListener click) {
        Context c = root.getContext();
        LinearLayout row = row(c); row.addView(icon(c, back ? R.drawable.ic_back : R.drawable.ic_menu,
                c.getString(back ? R.string.svc_back : R.string.svc_menu), click), new LinearLayout.LayoutParams(Ui.dp(c,48), Ui.dp(c,48)));
        LinearLayout titles = column(c);
        titles.addView(text(c, title, 24, R.color.desk_ink, true), match());
        if (!subtitle.isEmpty()) body(titles, subtitle);
        row.addView(titles, weight()); root.addView(row, match()); gap(root, 14);
    }
    static EditText field(LinearLayout parent, int label, int hint, int id, int inputType) {
        Context c = parent.getContext(); label(parent, c.getString(label));
        EditText e = new EditText(c); e.setId(id); e.setInputType(inputType);
        e.setTextSize(16); e.setTextColor(Ui.color(c, R.color.desk_ink));
        e.setHintTextColor(Ui.color(c, R.color.desk_soft));
        e.setHint(hint); e.setMinHeight(Ui.dp(c, 52)); pad(e, 14);
        e.setBackground(background(c, R.color.desk_surface, 14, false));
        e.setGravity(Gravity.TOP | Gravity.START); parent.addView(e, match()); return e;
    }
    static LinearLayout form(Base a, String title, String sub, View.OnClickListener back) {
        LinearLayout root = column(a); root.setBackgroundColor(Ui.color(a, R.color.desk_bg)); pad(root, 20);
        header(root, title, sub, true, back);
        ScrollView scroll = new ScrollView(a); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        LinearLayout body = column(a); body.setPadding(0,0,0,Ui.dp(a,24));
        scroll.addView(body, new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1)); a.setContentView(root); return body;
    }
    static TextView pill(Context c, String value, int color) {
        TextView t = text(c, value, 12, color, true);
        t.setBackground(background(c, R.color.field, 9, false));
        t.setPadding(Ui.dp(c,10),Ui.dp(c,6),Ui.dp(c,10),Ui.dp(c,6)); return t;
    }
}
