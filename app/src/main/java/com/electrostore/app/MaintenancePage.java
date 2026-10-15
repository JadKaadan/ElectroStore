package com.electrostore.app;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.text.*;


public class MaintenancePage extends Page {
    private final Calendar month=Calendar.getInstance();
    private String selected=Db.today();
    private final LinearLayout grid, records;
    private final TextView monthLabel,dayLabel,dayTotal,dayTotalLbp;
    public MaintenancePage(Context c) {
        super(c);setBackgroundColor(Ui.color(c,R.color.bg));
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);addView(scroll);
        LinearLayout body=ServiceUi.column(c);ServiceUi.pad(body,20);scroll.addView(body);
        ServiceUi.header(body,c.getString(R.string.tab_maintenance),"",false,v->((MainActivity)c).openDrawer());
        Button add=action(c.getString(R.string.simple_add),true,v->{selected=Db.today();month.setTime(new Date());Intent i=new Intent(c,ServiceJobEditActivity.class);c.startActivity(i);});add.setId(R.id.deskAdd);body.addView(add,ServiceUi.match());
        ServiceUi.gap(body,24);
        LinearLayout nav=ServiceUi.row(c);
        nav.addView(ServiceUi.icon(c,R.drawable.ic_back,c.getString(R.string.simple_prev),v->move(-1)),new LinearLayout.LayoutParams(Ui.dp(c,48),Ui.dp(c,48)));
        monthLabel=ServiceUi.text(c,"",18,R.color.ink,true);monthLabel.setGravity(Gravity.CENTER);monthLabel.setOnClickListener(v->CalendarCard.dateDialog(c,selected,false,(days,year,m)->{
            if(!days.isEmpty())selected=days.iterator().next();month.set(year,m,1);refresh();
        }));nav.addView(monthLabel,ServiceUi.weight());
        View next=ServiceUi.icon(c,R.drawable.ic_back,c.getString(R.string.simple_next),v->move(1));next.setRotation(180);nav.addView(next,new LinearLayout.LayoutParams(Ui.dp(c,48),Ui.dp(c,48)));body.addView(nav);
        LinearLayout weekdays=ServiceUi.row(c);String[] names=new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();
        int first=month.getFirstDayOfWeek();for(int n=0;n<7;n++){TextView label=ServiceUi.text(c,names[(first-1+n)%7+1],11,R.color.ink_soft,false);label.setGravity(Gravity.CENTER);weekdays.addView(label,ServiceUi.weight());}body.addView(weekdays);
        grid=ServiceUi.column(c);body.addView(grid,ServiceUi.match());ServiceUi.gap(body,16);
        dayLabel=ServiceUi.text(c,"",19,R.color.ink,true);body.addView(dayLabel,ServiceUi.match());ServiceUi.gap(body,10);
        LinearLayout totalCard=ServiceUi.column(c);totalCard.setBackgroundResource(R.drawable.bg_card);ServiceUi.pad(totalCard,18);
        totalCard.addView(ServiceUi.text(c,c.getString(R.string.simple_day_total),12,R.color.ink_soft,true),ServiceUi.match());
        dayTotal=ServiceUi.text(c,"",28,R.color.green,true);totalCard.addView(dayTotal,ServiceUi.match());
        dayTotalLbp=ServiceUi.text(c,"",13,R.color.ink_soft,false);totalCard.addView(dayTotalLbp,ServiceUi.match());
        body.addView(totalCard,ServiceUi.match());ServiceUi.gap(body,16);
        records=ServiceUi.column(c);body.addView(records,ServiceUi.match());refresh();
    }
    private Button action(String title,boolean primary,View.OnClickListener listener){
        Button b=new Button(getContext());b.setText(title);b.setAllCaps(false);b.setTextSize(16);b.setMinHeight(Ui.dp(getContext(),52));b.setStateListAnimator(null);
        b.setTextColor(Ui.color(getContext(),primary?R.color.on_accent:R.color.ink));b.setBackgroundResource(primary?R.drawable.btn_cyan:R.drawable.btn_outline);b.setOnClickListener(listener);return b;
    }
    public void saveState(android.os.Bundle state){state.putString("service_day",selected);state.putLong("service_month",month.getTimeInMillis());}
    public void restoreState(android.os.Bundle state){selected=state.getString("service_day",Db.today());month.setTimeInMillis(state.getLong("service_month",System.currentTimeMillis()));refresh();}
    private void move(int amount){month.set(Calendar.DAY_OF_MONTH,1);month.add(Calendar.MONTH,amount);selected=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(month.getTime());refresh();}
    @Override public void refresh(){
        Context c=getContext();MaintenanceStore store=new MaintenanceStore(c);Calendar start=(Calendar)month.clone();start.set(Calendar.DAY_OF_MONTH,1);
        monthLabel.setText(new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(start.getTime()));
        Set<String> active=store.entryDays(new SimpleDateFormat("yyyy-MM",Locale.US).format(start.getTime()));
        int lead=(start.get(Calendar.DAY_OF_WEEK)-start.getFirstDayOfWeek()+7)%7,days=start.getActualMaximum(Calendar.DAY_OF_MONTH);
        grid.removeAllViews();LinearLayout row=null;
        for(int n=0;n<((lead+days+6)/7)*7;n++){
            if(n%7==0){row=ServiceUi.row(c);grid.addView(row,ServiceUi.match());}
            View cell=LayoutInflater.from(c).inflate(R.layout.item_cal_day,row,false);row.addView(cell);
            int d=n-lead+1;if(d<1||d>days){cell.setVisibility(View.INVISIBLE);continue;}
            Calendar date=(Calendar)start.clone();date.set(Calendar.DAY_OF_MONTH,d);final String iso=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(date.getTime());
            TextView num=(TextView)cell.findViewById(R.id.num);num.setText(String.valueOf(d));
            if(iso.equals(selected)){cell.findViewById(R.id.bubble).setBackgroundResource(R.drawable.cal_selected);num.setTextColor(Ui.color(c,R.color.on_accent));}
            cell.findViewById(R.id.dot).setVisibility(active.contains(iso)&&!iso.equals(selected)?View.VISIBLE:View.INVISIBLE);
            cell.setContentDescription(Ui.prettyDay(iso));cell.setOnClickListener(v->{selected=iso;refresh();});
        }
        dayLabel.setText(Ui.prettyDay(selected));records.removeAllViews();List<ServiceJob> entries=store.entriesOn(selected);
        double paid=0;for(ServiceJob entry:entries)paid+=entry.paid;
        dayTotal.setText(Money.usd(paid));dayTotalLbp.setText(Money.lbp(c,paid));
        if(entries.isEmpty())ServiceUi.body(records,c.getString(R.string.simple_empty));
        for(ServiceJob entry:entries){
            LinearLayout card=ServiceUi.column(c);card.setBackgroundResource(R.drawable.bg_card);ServiceUi.pad(card,18);
            card.addView(ServiceUi.text(c,entry.title(c),19,R.color.ink,true),ServiceUi.match());ServiceUi.gap(card,8);
            ServiceUi.body(card,entry.device);ServiceUi.body(card,entry.customer);if(!entry.phone.isEmpty())ServiceUi.body(card,entry.phone);
            ServiceUi.gap(card,12);card.addView(ServiceUi.text(c,c.getString(R.string.simple_paid_value,Money.usd(entry.paid)),17,R.color.ink,true));ServiceUi.gap(card,16);
            LinearLayout actions=ServiceUi.row(c);
            Button edit=action(c.getString(R.string.desk_edit),false,v->Lock.require((MainActivity)c,()->{Intent i=new Intent(c,ServiceJobEditActivity.class);i.putExtra("id",entry.id);c.startActivity(i);}));edit.setId(R.id.deskEditJob);actions.addView(edit,ServiceUi.weight());
            LinearLayout.LayoutParams gap=ServiceUi.weight();gap.setMarginStart(Ui.dp(c,12));
            Button delete=action(c.getString(R.string.desk_delete),false,v->Lock.require((MainActivity)c,()->Ui.confirm(c,c.getString(R.string.desk_delete),c.getString(R.string.simple_delete_message),c.getString(R.string.desk_delete),()->{try{new MaintenanceStore(c).deleteJob(entry.id);refresh();}catch(RuntimeException e){Ui.toast(c,c.getString(R.string.svc_save_failed));}})));delete.setId(R.id.deskDeleteJob);delete.setTextColor(Ui.color(c,R.color.red));actions.addView(delete,gap);card.addView(actions);
            records.addView(card,ServiceUi.match());ServiceUi.gap(records,16);
        }
    }
}
