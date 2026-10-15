package com.electrostore.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class RevenuePage extends Page {

    private final ListView list;
    private final View empty, monthArchiveBlock;
    private final LinearLayout monthArchive;
    private final TextView todayUsd, todayLbp, monthTotal, todayLabel;
    private final HistoryAdapter adapter;

    public RevenuePage(Context c) {
        super(c);
        LayoutInflater.from(c).inflate(R.layout.page_revenue, this, true);

        list = (ListView) findViewById(R.id.list);
        empty = findViewById(R.id.empty);
        todayUsd = (TextView) findViewById(R.id.todayUsd);
        todayLbp = (TextView) findViewById(R.id.todayLbp);
        todayLabel = (TextView) findViewById(R.id.todayLabel);
        monthTotal = (TextView) findViewById(R.id.monthTotal);
        monthArchiveBlock = findViewById(R.id.monthArchiveBlock);
        monthArchive = (LinearLayout) findViewById(R.id.monthArchive);

        findViewById(R.id.productProfit).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getContext().startActivity(new Intent(getContext(), ProductProfitActivity.class));
            }
        });

        adapter = new HistoryAdapter(c);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Intent i = new Intent(getContext(), DayDetailActivity.class);
                i.putExtra("day", adapter.getItem(pos).day);
                getContext().startActivity(i);
            }
        });
    }

    @Override
    public void refresh() {
        Db db = Db.get(getContext());
        DayRow t = db.dayTotals(Db.today());
        todayUsd.setText(Money.usd(t.revenue));
        todayLbp.setText(Money.lbp(getContext(), t.revenue));
        todayLabel.setText(t.servicePaid!=0 ? getContext().getString(R.string.record_revenue_detail,t.items,Money.usd(t.servicePaid)) : t.sales == 0
                ? getContext().getString(R.string.today_nothing)
                : getContext().getString(R.string.today_pieces, t.items));
        monthTotal.setText(Money.usd(db.monthTotals(Db.currentMonth()).revenue));
        drawPreviousMonths(db.previousMonths());

        List<DayRow> days = db.revenueHistory(365);
        adapter.setItems(days);
        Ui.show(empty, days.isEmpty());
        Ui.show(list, !days.isEmpty());
    }

    private void drawPreviousMonths(List<MonthRow> months) {
        monthArchive.removeAllViews();
        Ui.show(monthArchiveBlock, months != null && !months.isEmpty());
        if (months == null) return;
        for (MonthRow month : months) {
            View card = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_month_total, monthArchive, false);
            ((TextView) card.findViewById(R.id.monthName)).setText(Ui.prettyMonth(month.month));
            TextView amount = (TextView) card.findViewById(R.id.monthAmount);
            amount.setText(Money.usd(month.revenue));
            amount.setTextColor(Ui.color(getContext(), R.color.green));
            ((TextView) card.findViewById(R.id.monthDetail)).setText(getContext().getString(
                    R.string.month_revenue_detail, month.items, Money.usd(month.servicePaid)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dp(getContext(), 154), LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(getContext(), 10));
            monthArchive.addView(card, lp);
        }
    }

    private static class HistoryAdapter extends BaseAdapter {
        private final Context ctx;
        private List<DayRow> items = new ArrayList<DayRow>();

        HistoryAdapter(Context c) { ctx = c; }

        void setItems(List<DayRow> l) {
            items = l == null ? new ArrayList<DayRow>() : l;
            notifyDataSetChanged();
        }

        public int getCount() { return items.size(); }
        public DayRow getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int pos, View convert, ViewGroup parent) {
            View v = convert;
            if (v == null) {
                v = LayoutInflater.from(ctx).inflate(R.layout.item_day, parent, false);
            }
            DayRow r = items.get(pos);
            ((TextView) v.findViewById(R.id.dayNum)).setText(Ui.dayNumber(r.day));
            ((TextView) v.findViewById(R.id.monTxt)).setText(Ui.monthShort(r.day));
            ((TextView) v.findViewById(R.id.dateTxt)).setText(Ui.shortDay(ctx, r.day));
            ((TextView) v.findViewById(R.id.detail)).setText(
                    ctx.getString(R.string.record_revenue_detail,r.items,Money.usd(r.servicePaid)));
            ((TextView) v.findViewById(R.id.amountUsd)).setText(Money.usd(r.revenue));
            ((TextView) v.findViewById(R.id.amountLbp)).setText(Money.lbp(ctx, r.revenue));
            return v;
        }
    }
}
