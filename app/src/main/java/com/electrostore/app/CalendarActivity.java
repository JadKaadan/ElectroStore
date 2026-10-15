package com.electrostore.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class CalendarActivity extends Base {

    private final Calendar month = Calendar.getInstance();
    private final Set<String> selectedDays = new LinkedHashSet<String>();
    private boolean manualSelectionStarted = false;
    private Set<String> active;

    private LinearLayout grid, weekdays, salesBox, ordersBox;
    private TextView monthLabel, dayLabel, dayRevenue, dayRevenueLbp, dayCounts,
            totalLabel, nothing, salesLabel, ordersLabel, btnClear;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_calendar);

        grid = (LinearLayout) findViewById(R.id.grid);
        weekdays = (LinearLayout) findViewById(R.id.weekdays);
        salesBox = (LinearLayout) findViewById(R.id.salesBox);
        ordersBox = (LinearLayout) findViewById(R.id.ordersBox);
        monthLabel = (TextView) findViewById(R.id.monthLabel);
        dayLabel = (TextView) findViewById(R.id.dayLabel);
        dayRevenue = (TextView) findViewById(R.id.dayRevenue);
        dayRevenueLbp = (TextView) findViewById(R.id.dayRevenueLbp);
        dayCounts = (TextView) findViewById(R.id.dayCounts);
        totalLabel = (TextView) findViewById(R.id.totalLabel);
        nothing = (TextView) findViewById(R.id.nothing);
        salesLabel = (TextView) findViewById(R.id.salesLabel);
        ordersLabel = (TextView) findViewById(R.id.ordersLabel);
        btnClear = (TextView) findViewById(R.id.btnClear);

        if (b != null) {
            ArrayList<String> saved = b.getStringArrayList("selected_days");
            if (saved != null) selectedDays.addAll(saved);
            manualSelectionStarted = b.getBoolean("manual_selection", false);
            month.setTimeInMillis(b.getLong("visible_month", System.currentTimeMillis()));
        } else {
            selectedDays.add(Db.today());
            month.setTime(new java.util.Date());
        }

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        findViewById(R.id.prevMonth).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { month.add(Calendar.MONTH, -1); drawMonth(); }
        });
        findViewById(R.id.nextMonth).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { month.add(Calendar.MONTH, 1); drawMonth(); }
        });
        findViewById(R.id.btnToday).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectedDays.clear();
                selectedDays.add(Db.today());
                manualSelectionStarted = false;
                month.setTime(new java.util.Date());
                drawMonth();
                drawDay();
            }
        });
        btnClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectedDays.clear();
                manualSelectionStarted = true;
                drawMonth();
                drawDay();
            }
        });


        boolean rtl = getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
        Ui.tint((ImageView) findViewById(R.id.prevMonth), Ui.color(this, R.color.ink_soft));
        Ui.tint((ImageView) findViewById(R.id.nextMonth), Ui.color(this, R.color.ink_soft));
        if (rtl) {
            findViewById(R.id.prevMonth).setRotation(180);
            findViewById(R.id.nextMonth).setRotation(180);
        }

        monthLabel.setOnClickListener(v -> CalendarCard.dateDialog(this,
                new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(month.getTime()),false,(days,year,m)->{
                    selectedDays.clear();selectedDays.addAll(days);manualSelectionStarted=true;
                    month.set(year,m,1);drawMonth();drawDay();
                }));
        buildWeekdayRow();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putStringArrayList("selected_days", new ArrayList<String>(selectedDays));
        out.putBoolean("manual_selection", manualSelectionStarted);
        out.putLong("visible_month", month.getTimeInMillis());
        super.onSaveInstanceState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        drawMonth();
        drawDay();
    }


    private void buildWeekdayRow() {
        weekdays.removeAllViews();
        String[] names = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();
        int first = Calendar.getInstance().getFirstDayOfWeek();
        for (int i = 0; i < 7; i++) {
            int dow = ((first - 1 + i) % 7) + 1;
            TextView t = new TextView(this);
            t.setText(names[dow]);
            t.setTextSize(11);
            t.setGravity(android.view.Gravity.CENTER);
            t.setTextColor(Ui.color(this, R.color.ink_faint));
            t.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            weekdays.addView(t);
        }
    }

    private void drawMonth() {
        Calendar c = (Calendar) month.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);

        String prefix = new SimpleDateFormat("yyyy-MM", Locale.US).format(c.getTime());
        active = Db.get(this).activeDays(prefix);
        monthLabel.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(c.getTime()));

        int first = c.getFirstDayOfWeek();
        int lead = (c.get(Calendar.DAY_OF_WEEK) - first + 7) % 7;
        int days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        int cells = (int) (Math.ceil((lead + days) / 7.0) * 7);

        grid.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < cells; i++) {
            if (i % 7 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            int dayNum = i - lead + 1;
            row.addView(dayCell(c, dayNum, dayNum >= 1 && dayNum <= days));
        }
    }

    private View dayCell(Calendar monthStart, int dayNum, boolean real) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_cal_day, null);
        TextView num = (TextView) v.findViewById(R.id.num);
        View bubble = v.findViewById(R.id.bubble);
        View dot = v.findViewById(R.id.dot);

        if (!real) {
            num.setText("");
            bubble.setBackground(null);
            dot.setVisibility(View.INVISIBLE);
            v.setClickable(false);
            return v;
        }

        Calendar c = (Calendar) monthStart.clone();
        c.set(Calendar.DAY_OF_MONTH, dayNum);
        final String iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());

        num.setText(String.valueOf(dayNum));
        boolean isSelected = selectedDays.contains(iso);
        boolean isToday = iso.equals(Db.today());

        if (isSelected) {
            bubble.setBackgroundResource(R.drawable.cal_selected);
            num.setTextColor(Ui.color(this, R.color.on_accent));
        } else if (isToday) {
            bubble.setBackgroundResource(R.drawable.cal_today);
            num.setTextColor(Ui.color(this, R.color.cyan));
        } else {
            bubble.setBackground(null);
            num.setTextColor(Ui.color(this, R.color.ink));
        }

        boolean busy = active != null && active.contains(iso);
        dot.setVisibility(busy && !isSelected ? View.VISIBLE : View.INVISIBLE);

        v.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {


                if (!manualSelectionStarted) {
                    selectedDays.clear();
                    manualSelectionStarted = true;
                }
                if (selectedDays.contains(iso)) selectedDays.remove(iso);
                else selectedDays.add(iso);
                drawMonth();
                drawDay();
            }
        });
        return v;
    }


    private void drawDay() {
        Db db = Db.get(this);
        DayRow r = new DayRow();
        List<Sale> sales = new ArrayList<Sale>();
        List<Order> orders = new ArrayList<Order>();
        List<String> days = new ArrayList<String>(selectedDays);
        Collections.sort(days);

        for (String day : days) {
            DayRow one = db.dayTotals(day);
            r.revenue += one.revenue;
            r.profit += one.profit;
            r.servicePaid += one.servicePaid;
            r.items += one.items;
            r.sales += one.sales;
            sales.addAll(db.salesOn(day));
            orders.addAll(db.orders(null, day));
        }

        Collections.sort(sales, new Comparator<Sale>() {
            public int compare(Sale a, Sale b) {
                return a.ts == b.ts ? 0 : (a.ts < b.ts ? 1 : -1);
            }
        });
        Collections.sort(orders, new Comparator<Order>() {
            public int compare(Order a, Order b) {
                return a.ts == b.ts ? 0 : (a.ts < b.ts ? 1 : -1);
            }
        });

        if (days.isEmpty()) {
            dayLabel.setText(R.string.cal_no_days_selected);
            totalLabel.setText(R.string.cal_selected_total);
            dayRevenue.setText(Money.usd(0));
            dayRevenueLbp.setText(Money.lbp(this, 0));
            dayCounts.setText(R.string.cal_tap_days);
            btnClear.setVisibility(View.GONE);
            nothing.setText(R.string.cal_choose_days);
            Ui.show(nothing, true);
            Ui.show(salesLabel, false);
            Ui.show(salesBox, false);
            Ui.show(ordersLabel, false);
            Ui.show(ordersBox, false);
            salesBox.removeAllViews();
            ordersBox.removeAllViews();
            return;
        }

        final boolean several = days.size() > 1;
        dayLabel.setText(several
                ? getString(R.string.cal_days_selected, days.size())
                : Ui.prettyDay(days.get(0)));
        totalLabel.setText(several ? R.string.cal_selected_total : R.string.cal_day_total);
        dayRevenue.setText(Money.usd(r.revenue));
        dayRevenueLbp.setText(Money.lbp(this, r.revenue));
        dayCounts.setText(getString(R.string.profit_pieces, Money.usd(r.profit), r.items)
                + "   ·   " + getString(R.string.cal_service_paid, Money.usd(r.servicePaid))
                + "   ·   " + getString(R.string.cal_orders_count, String.valueOf(orders.size())));
        btnClear.setVisibility(View.VISIBLE);

        boolean hasDetails = !sales.isEmpty() || !orders.isEmpty();
        if (!hasDetails) {
            nothing.setText(r.servicePaid != 0
                    ? R.string.cal_service_payments_only
                    : (several ? R.string.cal_nothing_selected : R.string.cal_nothing));
        }
        Ui.show(nothing, !hasDetails);

        Ui.show(salesLabel, !sales.isEmpty());
        Ui.show(salesBox, !sales.isEmpty());
        salesLabel.setText(several ? R.string.cal_sales_selected : R.string.cal_sales);
        salesBox.removeAllViews();
        for (final Sale s : sales) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_sale, salesBox, false);
            ((TextView) row.findViewById(R.id.name)).setText(s.productName);
            String saleDetail = getString(R.string.qty_times_price, s.qty,
                    Money.usd(s.unitPrice), Ui.clock(s.ts));
            ((TextView) row.findViewById(R.id.detail)).setText(several
                    ? Ui.shortDay(this, s.day) + "   ·   " + saleDetail : saleDetail);
            ((TextView) row.findViewById(R.id.amount)).setText(Money.usd(s.total));
            ((TextView) row.findViewById(R.id.amountLbp)).setText(Money.lbp(this, s.total));
            View del = row.findViewById(R.id.del);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) {
                  Lock.require(CalendarActivity.this, new Runnable() { public void run() {
                    Ui.confirm(CalendarActivity.this,
                            getString(R.string.undo_sale_title),
                            getString(R.string.undo_sale_msg, s.qty, s.productName),
                            getString(R.string.undo), new Runnable() {
                                public void run() {
                                    Db.get(CalendarActivity.this).deleteSale(s.id);
                                    drawMonth();
                                    drawDay();
                                }
                            });
                  }});
                }
            });
            salesBox.addView(row);
        }

        Ui.show(ordersLabel, !orders.isEmpty());
        Ui.show(ordersBox, !orders.isEmpty());
        ordersLabel.setText(several ? R.string.cal_orders_selected : R.string.cal_orders);
        ordersBox.removeAllViews();
        for (final Order o : orders) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_sale, ordersBox, false);
            boolean done = "DONE".equals(o.status);
            String who = o.customer == null || o.customer.trim().length() == 0
                    ? getString(R.string.walk_in) : o.customer;

            TextView name = (TextView) row.findViewById(R.id.name);
            name.setText(o.receiptNo);
            name.setTextColor(Ui.color(this, R.color.pink));

            String orderDetail = who + "   ·   "
                    + getString(done ? R.string.status_collected : R.string.status_waiting);
            ((TextView) row.findViewById(R.id.detail)).setText(several
                    ? Ui.shortDay(this, o.day) + "   ·   " + orderDetail : orderDetail);

            TextView amount = (TextView) row.findViewById(R.id.amount);
            amount.setText(Money.usd(o.total));
            amount.setTextColor(Ui.color(this, done ? R.color.green : R.color.amber));
            ((TextView) row.findViewById(R.id.amountLbp)).setText(Money.lbp(this, o.total));

            ImageView chev = (ImageView) row.findViewById(R.id.del);
            chev.setImageResource(R.drawable.ic_chevron);
            chev.setOnClickListener(null);
            chev.setClickable(false);

            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) {
                    Intent i = new Intent(CalendarActivity.this, ReceiptActivity.class);
                    i.putExtra("id", o.id);
                    startActivity(i);
                }
            });
            ordersBox.addView(row);
        }
    }
}
