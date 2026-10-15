package com.electrostore.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class OrdersPage extends Page {

    private final ListView list;
    private final TextView empty;
    private final TextView sub;
    private final TextView filterLabel;
    private final Button btnAll, btnToday, btnCalendar;
    private final OrderAdapter adapter;


    private String selectedDay = "";

    public OrdersPage(Context c) {
        super(c);
        LayoutInflater.from(c).inflate(R.layout.page_orders, this, true);

        list = (ListView) findViewById(R.id.list);
        empty = (TextView) findViewById(R.id.empty);
        sub = (TextView) findViewById(R.id.sub);
        filterLabel = (TextView) findViewById(R.id.filterLabel);
        btnAll = (Button) findViewById(R.id.btnAllDates);
        btnToday = (Button) findViewById(R.id.btnToday);
        btnCalendar = (Button) findViewById(R.id.btnCalendar);

        adapter = new OrderAdapter(c);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Intent i = new Intent(getContext(), ReceiptActivity.class);
                i.putExtra("id", adapter.getItem(pos).id);
                getContext().startActivity(i);
            }
        });

        findViewById(R.id.btnNew).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                getContext().startActivity(new Intent(getContext(), NewOrderActivity.class));
            }
        });

        btnAll.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                selectedDay = "";
                refresh();
            }
        });
        btnToday.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                selectedDay = Db.today();
                refresh();
            }
        });
        btnCalendar.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { chooseDate(); }
        });
    }

    private void chooseDate() {
        CalendarCard.dateDialog(getContext(),selectedDay.isEmpty()?Db.today():selectedDay,true,(days,year,month)->{
            if(!days.isEmpty()){selectedDay=days.iterator().next();refresh();}
        });
    }

    @Override
    public void refresh() {
        Db db = Db.get(getContext());
        List<Order> all = db.orders(null, selectedDay);
        adapter.setItems(all);
        Ui.show(empty, all.isEmpty());
        Ui.show(list, !all.isEmpty());

        int open = db.openOrderCount();
        sub.setText(open == 0
                ? getContext().getString(R.string.orders_none_waiting)
                : getContext().getString(R.string.orders_waiting, open));

        boolean allDates = selectedDay.length() == 0;
        String dayText = allDates ? getContext().getString(R.string.all_order_history)
                : Ui.shortDay(getContext(), selectedDay);
        filterLabel.setText(getContext().getString(R.string.showing_orders_for, dayText));
        empty.setText(allDates ? R.string.empty_orders : R.string.empty_orders_day);

        paintFilterButton(btnAll, allDates);
        paintFilterButton(btnToday, Db.today().equals(selectedDay));
        paintFilterButton(btnCalendar, !allDates && !Db.today().equals(selectedDay));
        btnCalendar.setText(!allDates && !Db.today().equals(selectedDay)
                ? Ui.shortDay(getContext(), selectedDay)
                : getContext().getString(R.string.choose_date));
    }

    private void paintFilterButton(Button b, boolean on) {
        b.setBackgroundResource(on ? R.drawable.chip_on : R.drawable.chip);
        b.setTextColor(Ui.color(getContext(), on ? R.color.on_accent : R.color.ink_soft));
    }

    private static class OrderAdapter extends BaseAdapter {
        private final Context ctx;
        private List<Order> items = new ArrayList<Order>();

        OrderAdapter(Context c) { ctx = c; }

        void setItems(List<Order> l) {
            items = l == null ? new ArrayList<Order>() : l;
            notifyDataSetChanged();
        }

        public int getCount() { return items.size(); }
        public Order getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return items.get(i).id; }

        public View getView(int pos, View convert, ViewGroup parent) {
            View v = convert;
            if (v == null) {
                v = LayoutInflater.from(ctx).inflate(R.layout.item_order, parent, false);
            }
            Order o = items.get(pos);
            ((TextView) v.findViewById(R.id.receipt)).setText(o.receiptNo);

            TextView status = (TextView) v.findViewById(R.id.status);
            boolean done = "DONE".equals(o.status);
            status.setText(ctx.getString(done ? R.string.status_collected : R.string.status_waiting));
            status.setBackgroundResource(done ? R.drawable.pill_green : R.drawable.pill_amber);
            status.setTextColor(Ui.color(ctx, done ? R.color.green : R.color.amber));

            String who = o.customer == null || o.customer.trim().length() == 0
                    ? ctx.getString(R.string.walk_in) : o.customer;
            ((TextView) v.findViewById(R.id.customer)).setText(who);

            StringBuilder d = new StringBuilder(Ui.shortDay(ctx, o.day));
            if (o.ts > 0) d.append("  ·  ").append(Ui.clock(o.ts));
            if (o.phone.length() > 0) d.append("  ·  ").append(Money.ltr(o.phone));
            double bal = o.balance();
            if (bal > 0.005) {
                d.append("  ·  ").append(ctx.getString(R.string.still_owes, Money.usd(bal)));
            }
            ((TextView) v.findViewById(R.id.detail)).setText(d.toString());

            ((TextView) v.findViewById(R.id.total)).setText(
                    Money.usd(o.total) + "   ·   " + Money.lbp(ctx, o.total));
            return v;
        }
    }
}
