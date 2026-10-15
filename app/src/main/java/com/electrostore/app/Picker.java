package com.electrostore.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class Picker {

    public interface OnProduct { void picked(Product p); }
    public interface OnLine { void picked(Product p, int qty, double unitPriceUsd); }

    public static void chooseProduct(final Activity a, final OnProduct cb) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_pick, null);
        final EditText search = (EditText) v.findViewById(R.id.search);
        final ListView list = (ListView) v.findViewById(R.id.list);

        final SimpleAdapter adapter = new SimpleAdapter(a);
        adapter.setItems(Db.get(a).products("", ""));
        list.setAdapter(adapter);

        final AlertDialog dlg = new AlertDialog.Builder(a, R.style.AppTheme_Dialog)
                .setTitle(a.getString(R.string.choose_product))
                .setView(v)
                .setNegativeButton(a.getString(R.string.cancel), null)
                .create();

        Ui.onType(search, new Ui.Watch() {
            public void onChanged(String s) {
                adapter.setItems(Db.get(a).products(s, ""));
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View x, int pos, long id) {
                dlg.dismiss();
                cb.picked(adapter.getItem(pos));
            }
        });
        dlg.show();
    }

    public static void chooseLine(final Activity a, final Product p, final OnLine cb) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_line, null);
        final EditText fQty = (EditText) v.findViewById(R.id.fQty);
        final EditText fPrice = (EditText) v.findViewById(R.id.fPrice);
        final TextView echo = (TextView) v.findViewById(R.id.echo);
        ((TextView) v.findViewById(R.id.title)).setText(p.displayName(a));
        fPrice.setText(Money.plain(p.bestPrice()));

        Ui.onType(fPrice, new Ui.Watch() {
            public void onChanged(String s) {
                double u = Money.parse(s);
                echo.setText(u > 0 ? "= " + Money.lbp(a, u) : "");
            }
        });
        v.findViewById(R.id.qtyMinus).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                int q = Math.max(1, Money.parseInt(fQty.getText()) - 1);
                fQty.setText(String.valueOf(q));
            }
        });
        v.findViewById(R.id.qtyPlus).setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                fQty.setText(String.valueOf(Money.parseInt(fQty.getText()) + 1));
            }
        });

        new AlertDialog.Builder(a, R.style.AppTheme_Dialog)
                .setView(v)
                .setNegativeButton(a.getString(R.string.cancel), null)
                .setPositiveButton(a.getString(R.string.add), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        int q = Math.max(1, Money.parseInt(fQty.getText()));
                        cb.picked(p, q, Money.parse(fPrice.getText()));
                    }
                })
                .show();
    }

    private static class SimpleAdapter extends BaseAdapter {
        private final Context ctx;
        private List<Product> items = new ArrayList<Product>();

        SimpleAdapter(Context c) { ctx = c; }

        void setItems(List<Product> l) {
            items = l == null ? new ArrayList<Product>() : l;
            notifyDataSetChanged();
        }

        public int getCount() { return items.size(); }
        public Product getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return items.get(i).id; }

        public View getView(int pos, View convert, ViewGroup parent) {
            View v = convert;
            if (v == null) {
                v = LayoutInflater.from(ctx).inflate(R.layout.item_pick, parent, false);
            }
            Product p = items.get(pos);
            ((TextView) v.findViewById(R.id.name)).setText(p.displayName(ctx));
            StringBuilder d = new StringBuilder("#" + p.displayNumber(ctx));
            if (p.category.length() > 0) d.append("  ·  ").append(p.category);
            d.append("  ·  ").append(ctx.getString(R.string.stock_count_short, p.quantity));
            ((TextView) v.findViewById(R.id.detail)).setText(d.toString());
            ((TextView) v.findViewById(R.id.price)).setText(
                    p.bestPrice() > 0 ? Money.usd(p.bestPrice()) : "—");
            return v;
        }
    }
}
