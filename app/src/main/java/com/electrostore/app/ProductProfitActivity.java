package com.electrostore.app;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class ProductProfitActivity extends Base {
    private static final int THIS_MONTH=0, ALL_TIME=1, DAY=2, THIS_YEAR=3, MONTH=4, YEAR=5, YEAR_MONTHS=6;
    private int mode=THIS_MONTH, calendarYear=ProfitDates.currentYear(), reportYear=ProfitDates.currentYear();
    private String selectedMonth=Db.currentMonth(), selectedDay=Db.today(), lastDay="";
    private TextView btnThisMonth, btnAllTime, btnChooseDay, btnThisYear, yearLabel;
    private TextView periodLabel, soldUsd, soldLbp, servicesUsd, servicesLbp, costUsd, costLbp, profitUsd, profitLbp;
    private TextView countLine, missingCost, profitHeading;
    private LinearLayout monthGrid;
    private final Handler clock=new Handler();
    private final Runnable checkDate=new Runnable() {
        @Override public void run() {
            if (!Db.today().equals(lastDay)) refreshReport();
            clock.postDelayed(this, 30000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_product_profit);
        btnThisMonth=findViewById(R.id.btnThisMonth);
        btnAllTime=findViewById(R.id.btnAllTime);
        btnChooseDay=findViewById(R.id.btnChooseDay);
        btnThisYear=findViewById(R.id.btnThisYear);
        yearLabel=findViewById(R.id.profitYearLabel);
        periodLabel=findViewById(R.id.periodLabel);
        soldUsd=findViewById(R.id.soldUsd); soldLbp=findViewById(R.id.soldLbp);
        servicesUsd=findViewById(R.id.servicesUsd); servicesLbp=findViewById(R.id.servicesLbp);
        costUsd=findViewById(R.id.costUsd); costLbp=findViewById(R.id.costLbp);
        profitUsd=findViewById(R.id.profitUsd); profitLbp=findViewById(R.id.profitLbp);
        countLine=findViewById(R.id.countLine); missingCost=findViewById(R.id.missingCost);
        profitHeading=findViewById(R.id.profitHeading);
        monthGrid=findViewById(R.id.profitMonthGrid);
        if (state!=null) {
            mode=state.getInt("mode",THIS_MONTH);
            reportYear=state.getInt("report_year",ProfitDates.currentYear());
            calendarYear=state.getInt("calendar_year",ProfitDates.currentYear());
            selectedMonth=state.getString("selected_month",Db.currentMonth());
            selectedDay=state.getString("selected_day",Db.today());
        } else {
            String month=getIntent().getStringExtra("month");
            if (month!=null && month.matches("[0-9]{4}-(0[1-9]|1[0-2])")) {
                selectedMonth=month; calendarYear=Integer.parseInt(month.substring(0,4)); mode=MONTH;
            }
        }
        findViewById(R.id.back).setOnClickListener(v -> finish());
        btnThisMonth.setOnClickListener(v -> { mode=THIS_MONTH; refreshReport(); });
        btnAllTime.setOnClickListener(v -> { mode=ALL_TIME; calendarYear=ProfitDates.currentYear(); refreshReport(); });
        btnChooseDay.setOnClickListener(v -> chooseDay());
        btnThisYear.setOnClickListener(v -> { mode=THIS_YEAR; refreshReport(); });
        yearLabel.setOnClickListener(v -> chooseYear());
        findViewById(R.id.profitPreviousYear).setOnClickListener(v -> browseYear(calendarYear-1));
        findViewById(R.id.profitNextYear).setOnClickListener(v -> browseYear(calendarYear+1));
        findViewById(R.id.profitYearTotal).setOnClickListener(v -> selectYear(calendarYear));
    }

    @Override protected void onResume() {
        super.onResume(); refreshReport(); clock.removeCallbacks(checkDate); clock.postDelayed(checkDate,30000);
    }
    @Override protected void onPause() { clock.removeCallbacks(checkDate); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("report_year",reportYear);
        out.putInt("mode",mode); out.putInt("calendar_year",calendarYear);
        out.putString("selected_month",selectedMonth); out.putString("selected_day",selectedDay);
        super.onSaveInstanceState(out);
    }

    private void browseYear(int year) {
        calendarYear=Math.max(1900,Math.min(year,ProfitDates.currentYear()+100));
        if(mode==ALL_TIME||mode==YEAR) mode=ALL_TIME;
        else mode=YEAR_MONTHS;
        refreshReport();
    }
    private void selectYear(int year) {
        calendarYear=Math.max(1900,Math.min(year,ProfitDates.currentYear()+100));
        mode=calendarYear==ProfitDates.currentYear()?THIS_YEAR:YEAR_MONTHS;
        refreshReport();
    }
    private void chooseYear() {
        CalendarCard.yearDialog(this,calendarYear,(days,year,month)->browseYear(year));
    }
    private void chooseDay() {
        CalendarCard.dateDialog(this,selectedDay,true,(days,year,month)->{
            if (days.isEmpty()) return;
            selectedDay=days.iterator().next();
            calendarYear=Integer.parseInt(selectedDay.substring(0,4));mode=DAY;refreshReport();
        });
    }
    private void refreshReport() {
        lastDay=Db.today();
        if (mode==THIS_MONTH) { selectedMonth=Db.currentMonth(); calendarYear=ProfitDates.currentYear(); }
        if (mode==THIS_YEAR) calendarYear=ProfitDates.currentYear();
        styleButton(btnThisMonth,mode==THIS_MONTH);
        styleButton(btnAllTime,mode==ALL_TIME||mode==YEAR);
        styleButton(btnChooseDay,mode==DAY);
        styleButton(btnThisYear,mode==THIS_YEAR||mode==YEAR_MONTHS||mode==MONTH);
        Db db=Db.get(this);
        ProfitTotals[] months=db.productProfitYear(calendarYear);
        double[] serviceMonths=db.servicePaymentsYear(calendarYear);
        ProfitTotals year=new ProfitTotals();
        for (ProfitTotals month:months) year.add(month);
        ProfitTotals totals;
        double servicePaid;
        String label;
        if (mode==ALL_TIME) {
            totals=db.productProfitTotals(null,null);
            servicePaid=db.servicePaymentsTotal(null,null);
            label=getString(R.string.profit_all_time_label);
        }
        else if (mode==DAY) {
            String nextDay=ProfitDates.nextDay(selectedDay);
            totals=db.productProfitTotals(selectedDay,nextDay);
            servicePaid=db.servicePaymentsTotal(selectedDay,nextDay);
            label=formatDay(selectedDay);
        }
        else if (mode==THIS_YEAR || mode==YEAR || mode==YEAR_MONTHS) {
            totals=year;
            int shownYear=calendarYear;
            if(mode==YEAR){shownYear=reportYear;totals=new ProfitTotals();for(ProfitTotals m:db.productProfitYear(reportYear))totals.add(m);}

            double[] selectedYearServices=shownYear==calendarYear?serviceMonths:db.servicePaymentsYear(shownYear);
            servicePaid=0;
            for(double amount:selectedYearServices) servicePaid+=amount;
            label=getString(shownYear==ProfitDates.currentYear()?R.string.profit_year_to_date:R.string.profit_full_year,shownYear);
        } else {
            int index=Integer.parseInt(selectedMonth.substring(5,7))-1;
            totals=months[index];
            servicePaid=serviceMonths[index];
            label=Ui.prettyMonth(selectedMonth);
        }
        periodLabel.setText(label);


        soldUsd.setText(Money.usd(totals.sold.doubleValue())); soldLbp.setText(Money.lbp(this,totals.sold.doubleValue()));
        servicesUsd.setText(Money.usd(servicePaid)); servicesLbp.setText(Money.lbp(this,servicePaid));
        costUsd.setText(Money.usd(totals.cost.doubleValue())); costLbp.setText(Money.lbp(this,totals.cost.doubleValue()));
        profitUsd.setText(Money.usd(totals.profit.doubleValue())); profitLbp.setText(Money.lbp(this,totals.profit.doubleValue()));
        profitUsd.setTextColor(Ui.color(this,totals.profit.signum()<0?R.color.red:totals.profit.signum()>0?R.color.green:R.color.ink));
        profitHeading.setText(totals.missingCostPieces>0?R.string.profit_known_cost_total:R.string.profit_product_profit);
        countLine.setText(totals.sales==0?getString(R.string.profit_no_recorded_sales):getResources().getQuantityString(R.plurals.profit_pieces_sold,totals.pieces,totals.pieces));
        missingCost.setVisibility(totals.missingCostPieces>0?View.VISIBLE:View.GONE);
        if (totals.missingCostPieces>0) missingCost.setText(getResources().getQuantityString(R.plurals.profit_missing_summary,totals.missingCostPieces,totals.missingCostPieces));
        yearLabel.setText(String.valueOf(calendarYear)+" ▾");
        yearLabel.setContentDescription(getString(R.string.profit_choose_year)+", "+calendarYear);
        setEnabled(findViewById(R.id.profitPreviousYear),calendarYear>1900);
        setEnabled(findViewById(R.id.profitNextYear),calendarYear<ProfitDates.currentYear()+100);
        TextView yearTotal=findViewById(R.id.profitYearTotal);
        yearTotal.setText(getString(R.string.profit_year_calendar_total,calendarYear,Money.usd(year.profit.doubleValue())));
        boolean summaryOnly=mode==THIS_MONTH||mode==DAY;
        boolean annual=mode==ALL_TIME||mode==YEAR;
        findViewById(R.id.profitArchiveSection).setVisibility(summaryOnly?View.GONE:View.VISIBLE);
        yearTotal.setVisibility(!summaryOnly&&!annual?View.VISIBLE:View.GONE);
        ((TextView)findViewById(R.id.profitCalendarTitle)).setText(annual?R.string.profit_by_year:R.string.profit_by_month);
        if(summaryOnly) monthGrid.removeAllViews();else if(annual) drawYears();else drawMonths(months);
    }
    private void setEnabled(View view,boolean enabled) { view.setEnabled(enabled); view.setAlpha(enabled?1f:.3f); }
    private void styleButton(TextView button,boolean selected) {
        button.setBackgroundResource(selected?R.drawable.chip_on:R.drawable.chip);
        button.setTextColor(Ui.color(this,selected?R.color.on_accent:R.color.ink));
        button.setSelected(selected);
    }
    private void drawMonths(ProfitTotals[] months) {
        monthGrid.removeAllViews();

        int columns=getResources().getConfiguration().fontScale>1.2f?2:3;
        LinearLayout row=null;
        int first=0;
        int last=12;
        for (int i=first;i<last;i++) {
            if ((i-first)%columns==0) { row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); monthGrid.addView(row); }
            final String month=ProfitDates.month(calendarYear,i);
            ProfitTotals total=months[i];
            boolean selected=(mode==MONTH||mode==THIS_MONTH)&&month.equals(selectedMonth);
            boolean future=month.compareTo(Db.currentMonth())>0;
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(Ui.dp(this,10),Ui.dp(this,12),Ui.dp(this,10),Ui.dp(this,12));
            card.setBackgroundResource(selected?R.drawable.chip_on:R.drawable.bg_card_line);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1);
            lp.setMargins(Ui.dp(this,3),Ui.dp(this,4),Ui.dp(this,3),Ui.dp(this,4));
            row.addView(card,lp);
            TextView name=cardText(Ui.prettyMonth(month),11,selected?R.color.on_accent:R.color.ink_soft);
            TextView amount=cardText(total.sales==0?"—":Money.usd(total.profit.doubleValue()),16,
                    selected?R.color.on_accent:total.profit.signum()<0?R.color.red:total.profit.signum()>0?R.color.green:R.color.ink);
            amount.setTypeface(null,android.graphics.Typeface.BOLD);
            amount.setPadding(0,Ui.dp(this,8),0,Ui.dp(this,4));
            String detail=future&&total.sales==0?getString(R.string.profit_upcoming):total.sales==0?
                    getString(R.string.profit_no_recorded_sales):total.missingCostPieces>0?
                    getString(R.string.profit_incomplete):getResources().getQuantityString(R.plurals.profit_pieces_sold,total.pieces,total.pieces);
            card.addView(name); card.addView(amount);
            card.addView(cardText(detail,11,selected?R.color.on_accent:R.color.ink_soft));
            card.setContentDescription(Ui.prettyMonth(month)+", "+amount.getText()+", "+detail);
            card.setEnabled(!future || total.sales>0); card.setAlpha(card.isEnabled()?1f:.45f);
            card.setClickable(card.isEnabled()); card.setFocusable(card.isEnabled()); card.setSelected(selected);
            card.setOnClickListener(v -> { selectedMonth=month; mode=MONTH; refreshReport(); });
        }
    }

    private void drawYears() {
        monthGrid.removeAllViews();
        String firstSale=Db.get(this).firstProductSaleDay();
        int firstRecorded=firstSale.isEmpty()?ProfitDates.currentYear():Integer.parseInt(firstSale.substring(0,4));
        int start=Math.max(1900,Math.max(calendarYear-5,Math.min(calendarYear,firstRecorded)));
        for(int y=calendarYear;y>=start;y--){
            final int picked=y;
            ProfitTotals total=new ProfitTotals();
            for(ProfitTotals m:Db.get(this).productProfitYear(y)) total.add(m);
            boolean selected=mode==YEAR&&y==reportYear;
            int ink=selected?R.color.on_accent:R.color.ink;
            int muted=selected?R.color.on_accent:R.color.ink_soft;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);
            card.setPadding(Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18));
            card.setBackgroundResource(selected?R.drawable.chip_on:R.drawable.bg_card_line);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=Ui.dp(this,10);monthGrid.addView(card,lp);
            LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);
            card.addView(left,new LinearLayout.LayoutParams(0,-2,1));
            TextView yearName=cardText(String.valueOf(y),21,ink);yearName.setTypeface(null,android.graphics.Typeface.BOLD);left.addView(yearName);
            String detail=getString(total.sales==0?R.string.profit_no_recorded_sales:total.missingCostPieces>0?R.string.profit_incomplete:y==ProfitDates.currentYear()?R.string.profit_year_progress:R.string.profit_year_complete);
            TextView caption=cardText(detail,12,muted);caption.setPadding(0,Ui.dp(this,5),0,0);left.addView(caption);
            LinearLayout right=new LinearLayout(this);right.setOrientation(LinearLayout.VERTICAL);right.setGravity(android.view.Gravity.END);
            right.setPadding(Ui.dp(this,12),0,0,0);card.addView(right,new LinearLayout.LayoutParams(0,-2,1));
            TextView amount=cardText(Money.usd(total.profit.doubleValue()),21,selected?R.color.on_accent:total.profit.signum()<0?R.color.red:total.profit.signum()>0?R.color.green:R.color.ink);
            amount.setTypeface(null,android.graphics.Typeface.BOLD);right.addView(amount);
            TextView profitLabel=cardText(getString(R.string.profit_product_profit),10,muted);profitLabel.setPadding(0,Ui.dp(this,5),0,0);right.addView(profitLabel);
            card.setContentDescription(y+", "+amount.getText()+", "+detail);
            card.setSelected(selected);card.setFocusable(true);card.setOnClickListener(v->{reportYear=picked;mode=YEAR;refreshReport();});
        }
    }
    private TextView cardText(String text,int size,int color) {
        TextView view=new TextView(this); view.setText(text); view.setTextSize(size);
        view.setTextColor(Ui.color(this,color)); return view;
    }
    private String formatDay(String iso) {
        try { Date date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(iso);
            return new SimpleDateFormat("d MMMM yyyy",Locale.getDefault()).format(date); }
        catch (Exception ignored) { return iso; }
    }
}
