package com.electrostore.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.*;
import java.text.*;
import java.util.*;


public final class CalendarCard extends LinearLayout {
    public interface Applied { void apply(Set<String> days, int year, int month); }
    public interface ActivityDays { Set<String> get(String month); }
    private static final int PAPER=0xFFFFFCF6, INK=0xFF4D341F, OLIVE=0xFF708044,
            GREEN=0xFF8B9F59, ORANGE=0xFFE9852D, MUTED=0xFFB6AEA4;
    private final Calendar visible=Calendar.getInstance();
    private final LinkedHashSet<String> selected=new LinkedHashSet<String>();
    private final LinearLayout grid;
    private final TextView heading, prev, next;
    private final Button apply;
    private final boolean multi, pastOnly;
    private boolean years=false, replaceDefault=false;
    private Applied callback;
    private ActivityDays activityDays;
    public CalendarCard(Context c,boolean multiple,boolean historical,Applied listener) {
        super(c); multi=multiple; pastOnly=historical; callback=listener;
        visible.set(Calendar.DAY_OF_MONTH,1);
        setOrientation(VERTICAL); setPadding(dp(12),dp(12),dp(12),dp(10));
        setBackground(shape(PAPER,26,0xFFDDD5C9));setElevation(dp(3));

        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        LinearLayout nav=new LinearLayout(c);nav.setGravity(Gravity.CENTER_VERTICAL);
        prev=button("‹",22);next=button("›",22);heading=button("",16);
        prev.setContentDescription(c.getString(R.string.simple_prev));next.setContentDescription(c.getString(R.string.simple_next));
        prev.setBackground(shape(0xFFF3EFE8,24,0));next.setBackground(shape(0xFFF3EFE8,24,0));
        nav.addView(prev,new LayoutParams(dp(44),dp(44)));nav.addView(heading,new LayoutParams(0,dp(48),1));nav.addView(next,new LayoutParams(dp(44),dp(44)));addView(nav);
        grid=new LinearLayout(c);grid.setOrientation(VERTICAL);addView(grid);
        apply=new Button(c);apply.setAllCaps(false);apply.setText(R.string.calendar_apply);apply.setTextColor(Color.WHITE);apply.setTextSize(14);apply.setBackground(shape(INK,24,0));
        LayoutParams ap=new LayoutParams(-1,dp(48));ap.topMargin=dp(10);addView(apply,ap);
        prev.setOnClickListener(v->move(-1));next.setOnClickListener(v->move(1));
        heading.setOnClickListener(v->{years=!years;draw();});
        apply.setOnClickListener(v->{if(callback!=null)callback.apply(new LinkedHashSet<String>(selected),visible.get(Calendar.YEAR),visible.get(Calendar.MONTH));});
        if(multi){
            TextView clear=button(c.getString(R.string.cal_clear_selection),12);
            addView(clear,new LayoutParams(-1,dp(44)));
            clear.setOnClickListener(v->{selected.clear();replaceDefault=false;draw();});
        }
        draw();
    }
    public void setActivityDays(ActivityDays provider){activityDays=provider;draw();}
    public long visibleTime(){return visible.getTimeInMillis();}
    public void setSelection(Collection<String> days,long month,boolean defaultSelection){selected.clear();selected.addAll(days);visible.setTimeInMillis(month);visible.set(Calendar.DAY_OF_MONTH,1);replaceDefault=defaultSelection;draw();}
    public void showYears(){years=true;draw();}
    private int dp(int n){return Ui.dp(getContext(),n);}
    private GradientDrawable shape(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private TextView button(String text,int size){TextView t=new TextView(getContext());t.setText(text);t.setTextColor(INK);t.setTextSize(size);t.setGravity(Gravity.CENTER);t.setTypeface(null,Typeface.BOLD);t.setFocusable(true);return t;}
    private String iso(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    private void move(int delta){visible.set(Calendar.DAY_OF_MONTH,1);visible.add(years?Calendar.YEAR:Calendar.MONTH,delta*(years?12:1));int y=visible.get(Calendar.YEAR);if(y<1900)visible.set(1900,0,1);if(y>ProfitDates.currentYear()+100)visible.set(ProfitDates.currentYear()+100,11,1);draw();}
    private void draw(){
        grid.removeAllViews();
        int year=visible.get(Calendar.YEAR);int base=year-year%12;
        boolean canPrev=years?base>1900:year>1900||visible.get(Calendar.MONTH)>0;
        boolean canNext=years?base+11<ProfitDates.currentYear()+100:year<ProfitDates.currentYear()+100||visible.get(Calendar.MONTH)<11;
        prev.setEnabled(canPrev);prev.setAlpha(canPrev?1f:.3f);next.setEnabled(canNext);next.setAlpha(canNext?1f:.3f);
        heading.setText(years?base+" – "+(base+11):new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(visible.getTime())+" ▾");
        heading.setContentDescription(getContext().getString(R.string.profit_choose_year)+", "+heading.getText());
        if(years){
            for(int r=0;r<4;r++){LinearLayout row=new LinearLayout(getContext());grid.addView(row);
                for(int col=0;col<3;col++){final int y=base+r*3+col;TextView cell=button(String.valueOf(y),16);LayoutParams p=new LayoutParams(0,dp(52),1);p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(cell,p);
                    if(y==year){cell.setBackground(shape(GREEN,24,0));cell.setTextColor(Color.WHITE);}
                    boolean ok=y>=1900&&y<=ProfitDates.currentYear()+100;cell.setEnabled(ok);cell.setAlpha(ok?1f:.3f);
                    cell.setOnClickListener(v->{visible.set(Calendar.DAY_OF_MONTH,1);visible.set(Calendar.YEAR,y);years=false;draw();});
                }
            }return;
        }
        LinearLayout week=new LinearLayout(getContext());grid.addView(week);String[] names=new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();
        int first=visible.getFirstDayOfWeek();for(int i=0;i<7;i++){TextView label=button(names[(first-1+i)%7+1],11);label.setTextColor(OLIVE);week.addView(label,new LayoutParams(0,dp(36),1));}
        Calendar cursor=(Calendar)visible.clone();cursor.set(Calendar.DAY_OF_MONTH,1);
        int lead=(cursor.get(Calendar.DAY_OF_WEEK)-first+7)%7;int count=((lead+cursor.getActualMaximum(Calendar.DAY_OF_MONTH)+6)/7)*7;cursor.add(Calendar.DATE,-lead);
        Set<String> active=activityDays==null?Collections.<String>emptySet():activityDays.get(new SimpleDateFormat("yyyy-MM",Locale.US).format(visible.getTime()));
        LinearLayout row=null;
        for(int i=0;i<count;i++){
            if(i%7==0){row=new LinearLayout(getContext());grid.addView(row);}
            final Calendar day=(Calendar)cursor.clone();final String date=iso(day);boolean inMonth=day.get(Calendar.MONTH)==visible.get(Calendar.MONTH);boolean chosen=selected.contains(date);boolean today=date.equals(Db.today());
            FrameLayout cell=new FrameLayout(getContext());row.addView(cell,new LayoutParams(0,dp(46),1));
            TextView num=button(String.valueOf(day.get(Calendar.DAY_OF_MONTH)),13);num.setTypeface(null,Typeface.NORMAL);
            FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(dp(34),dp(34),Gravity.CENTER);cell.addView(num,np);
            if(chosen||today){num.setBackground(shape(chosen?GREEN:ORANGE,20,chosen?0xFFBCCBA0:0xFFF6C38F));num.setTextColor(Color.WHITE);}else num.setTextColor(inMonth?INK:MUTED);
            if(active!=null&&active.contains(date)&&!chosen){View dot=new View(getContext());dot.setBackground(shape(OLIVE,3,0));FrameLayout.LayoutParams dotp=new FrameLayout.LayoutParams(dp(4),dp(4),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);dotp.bottomMargin=dp(2);cell.addView(dot,dotp);}
            boolean allowed=!pastOnly||date.compareTo(Db.today())<=0;cell.setEnabled(allowed);cell.setAlpha(allowed?1f:.3f);cell.setFocusable(allowed);cell.setContentDescription(Ui.prettyDay(date)+(chosen?", "+getContext().getString(R.string.calendar_selected):""));cell.setSelected(chosen);
            cell.setOnClickListener(v->{if(!multi||replaceDefault){selected.clear();replaceDefault=false;}if(multi&&selected.contains(date))selected.remove(date);else selected.add(date);if(day.get(Calendar.MONTH)!=visible.get(Calendar.MONTH)){visible.setTimeInMillis(day.getTimeInMillis());visible.set(Calendar.DAY_OF_MONTH,1);}draw();});
            cursor.add(Calendar.DATE,1);
        }
    }
    public static void dateDialog(Context c,String day,boolean historical,Applied listener){show(c,day,historical,false,listener);}
    public static void yearDialog(Context c,int year,Applied listener){show(c,String.format(Locale.US,"%04d-%02d-01",year,Calendar.getInstance().get(Calendar.MONTH)+1),false,true,listener);}
    private static void show(Context c,String initial,boolean pastOnly,boolean yearMode,Applied listener){
        Dialog dialog=new Dialog(c);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Calendar at=Calendar.getInstance();try{at.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(initial));}catch(Exception ignored){}
        CalendarCard card=new CalendarCard(c,false,pastOnly,(days,y,m)->{listener.apply(days,y,m);dialog.dismiss();});
        card.setSelection(Collections.singleton(initial),at.getTimeInMillis(),false);if(yearMode)card.showYears();
        ScrollView scroll=new ScrollView(c);scroll.addView(card);dialog.setContentView(scroll);dialog.show();
        Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(Math.min(Ui.dp(c,360),c.getResources().getDisplayMetrics().widthPixels-Ui.dp(c,32)),android.view.ViewGroup.LayoutParams.WRAP_CONTENT);}
    }
}
