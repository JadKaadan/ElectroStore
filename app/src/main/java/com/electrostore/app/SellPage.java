package com.electrostore.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;


public class SellPage extends Page {

    private final ProductAdapter adapter;
    private final EditText search;
    private final ListView list;
    private final View empty;
    private final TextView todayLine;

    public SellPage(Context c) {
        super(c);
        LayoutInflater.from(c).inflate(R.layout.page_sell, this, true);

        search = (EditText) findViewById(R.id.search);
        list = (ListView) findViewById(R.id.list);
        empty = findViewById(R.id.empty);
        todayLine = (TextView) findViewById(R.id.todayLine);

        adapter = new ProductAdapter(c);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Intent i = new Intent(getContext(), SellActivity.class);
                i.putExtra("id", adapter.getItem(pos).id);
                getContext().startActivity(i);
            }
        });

        Ui.onType(search, new Ui.Watch() {
            public void onChanged(String s) { reload(); }
        });
    }

    @Override
    public void refresh() { reload(); }

    private void reload() {
        Db db = Db.get(getContext());
        List<Product> items = db.products(search.getText().toString(), "");
        adapter.setItems(items);
        boolean none = items.isEmpty();
        Ui.show(empty, none);
        Ui.show(list, !none);

        DayRow today = db.dayTotals(Db.today());
        todayLine.setText(getContext().getString(R.string.today_so_far,
                Money.usd(today.revenue) + "   ·   " + Money.lbp(getContext(), today.revenue)));
    }
}
