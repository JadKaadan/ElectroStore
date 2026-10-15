package com.electrostore.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class DayDetailActivity extends Base {

    private String day;
    private SaleAdapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_day_detail);
        day = getIntent().getStringExtra("day");
        if (day == null) day = Db.today();

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        adapter = new SaleAdapter();
        ((ListView) findViewById(R.id.list)).setAdapter(adapter);
        draw();
    }

    private void draw() {
        Db db = Db.get(this);
        DayRow r = db.dayTotals(day);
        ((TextView) findViewById(R.id.dateTxt)).setText(Ui.prettyDay(day));
        ((TextView) findViewById(R.id.revUsd)).setText(Money.usd(r.revenue));
        ((TextView) findViewById(R.id.revLbp)).setText(Money.lbp(this, r.revenue));
        ((TextView) findViewById(R.id.profitTxt)).setText(
                getString(R.string.profit_pieces, Money.usd(r.profit), r.items)+"\n"+getString(R.string.record_service_revenue,Money.usd(r.servicePaid)));
        adapter.setItems(db.salesOn(day));
    }

    private class SaleAdapter extends BaseAdapter {
        private List<Sale> items = new ArrayList<Sale>();

        void setItems(List<Sale> l) {
            items = l == null ? new ArrayList<Sale>() : l;
            notifyDataSetChanged();
        }

        public int getCount() { return items.size(); }
        public Sale getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return items.get(i).id; }

        public View getView(int pos, View convert, ViewGroup parent) {
            View v = convert;
            if (v == null) {
                v = LayoutInflater.from(DayDetailActivity.this)
                        .inflate(R.layout.item_sale, parent, false);
            }
            final Sale s = items.get(pos);
            ((TextView) v.findViewById(R.id.name)).setText(s.productName);
            ((TextView) v.findViewById(R.id.detail)).setText(
                    getString(R.string.qty_times_price, s.qty,
                            Money.usd(s.unitPrice), Ui.clock(s.ts)));
            ((TextView) v.findViewById(R.id.amount)).setText(Money.usd(s.total));
            ((TextView) v.findViewById(R.id.amountLbp)).setText(
                    Money.lbp(DayDetailActivity.this, s.total));

            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) {
                    if (s.productId <= 0 || Db.get(DayDetailActivity.this).product(s.productId) == null) {
                        Ui.toast(DayDetailActivity.this, getString(R.string.sale_product_unavailable));
                        return;
                    }
                    Intent i = new Intent(DayDetailActivity.this, ProductDetailActivity.class);
                    i.putExtra("id", s.productId);
                    startActivity(i);
                }
            });

            v.findViewById(R.id.del).setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) {
                  Lock.require(DayDetailActivity.this, new Runnable() { public void run() {
                    Ui.confirm(DayDetailActivity.this,
                            getString(R.string.undo_sale_title),
                            getString(R.string.undo_sale_msg, s.qty, s.productName),
                            getString(R.string.undo), new Runnable() {
                                public void run() {
                                    Db.get(DayDetailActivity.this).deleteSale(s.id);
                                    draw();
                                }
                            });
                  }});
                }
            });
            return v;
        }
    }
}
