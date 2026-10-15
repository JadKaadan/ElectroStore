package com.electrostore.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


final class DeskUi {
    static GradientDrawable shape(Context c,int color,int radius) {return ServiceUi.background(c,color,radius,false);}
    static TextView text(Context c,String value,int size,int color,boolean bold) {
        TextView t=ServiceUi.text(c,value,size,color,bold);t.setFontFeatureSettings("kern");
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        t.setIncludeFontPadding(false);t.setLineSpacing(Ui.dp(c,3),1);return t;
    }
    static Button action(Context c,String label,int color,int fill,View.OnClickListener click) {
        Button b=ServiceUi.button(c,label,false,click);b.setTextSize(13);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setTextColor(Ui.color(c,color));b.setBackground(shape(c,fill,12));b.setMinHeight(Ui.dp(c,48));return b;
    }
    static void gap(LinearLayout row,int dp) {row.addView(new View(row.getContext()),new LinearLayout.LayoutParams(Ui.dp(row.getContext(),dp),1));}
    static void line(LinearLayout root) {View v=new View(root.getContext());v.setBackgroundColor(Ui.color(root.getContext(),R.color.desk_line));root.addView(v,new LinearLayout.LayoutParams(-1,Ui.dp(root.getContext(),1)));}
    static ImageView icon(Context c,int drawable,int ink,int fill,int size) {
        ImageView v=new ImageView(c);v.setImageResource(drawable);Ui.tint(v,Ui.color(c,ink));ServiceUi.pad(v,size/4);
        v.setBackground(shape(c,fill,16));return v;
    }
    static TextView badge(Context c,String label,int ink,int fill) {
        TextView t=text(c,label,11,ink,true);t.setPadding(Ui.dp(c,10),Ui.dp(c,6),Ui.dp(c,10),Ui.dp(c,6));
        t.setBackground(shape(c,fill,8));return t;
    }
    static LinearLayout card(Context c) {
        LinearLayout card=ServiceUi.column(c);ServiceUi.pad(card,20);card.setBackground(shape(c,R.color.desk_surface,22));return card;
    }
    static TextView avatar(Context c,String name,int size) {
        String[] words=name.trim().split("\\s+");String value=words[0].isEmpty()?"?":words[0].substring(0,1);
        if(words.length>1)value+=words[words.length-1].substring(0,1);
        TextView t=text(c,value.toUpperCase(java.util.Locale.ROOT),size/3,R.color.desk_accent,true);t.setGravity(Gravity.CENTER);
        t.setBackground(shape(c,R.color.desk_tint,size/2));return t;
    }
}
