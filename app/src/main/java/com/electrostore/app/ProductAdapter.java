package com.electrostore.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class ProductAdapter extends BaseAdapter {

    private final Context ctx;
    private List<Product> items = new ArrayList<Product>();
    private int lowStock = 3;

    public ProductAdapter(Context c) {
        this.ctx = c;
        try {
            lowStock = Integer.parseInt(Db.get(c).setting("low_stock", "3"));
        } catch (Exception ignored) { }
    }

    public void setItems(List<Product> list) {
        items = list == null ? new ArrayList<Product>() : list;
        try {
            lowStock = Integer.parseInt(Db.get(ctx).setting("low_stock", "3"));
        } catch (Exception ignored) { }
        notifyDataSetChanged();
    }

    public int getCount() { return items.size(); }
    public Product getItem(int i) { return items.get(i); }
    public long getItemId(int i) { return items.get(i).id; }

    public View getView(int pos, View convert, ViewGroup parent) {
        View v = convert;
        if (v == null) v = LayoutInflater.from(ctx).inflate(R.layout.item_product, parent, false);

        Product p = items.get(pos);

        TextView name = (TextView) v.findViewById(R.id.name);
        TextView meta = (TextView) v.findViewById(R.id.meta);
        TextView usd = (TextView) v.findViewById(R.id.priceUsd);
        TextView lbp = (TextView) v.findViewById(R.id.priceLbp);
        TextView qty = (TextView) v.findViewById(R.id.qty);
        ImageView photo = (ImageView) v.findViewById(R.id.photo);
        ImageView holder = (ImageView) v.findViewById(R.id.photoPlaceholder);

        name.setText(p.displayName(ctx));

        StringBuilder m = new StringBuilder("#" + p.displayNumber(ctx));
        if (p.category.length() > 0) m.append("  ·  ").append(p.category);
        if (p.color.length() > 0) m.append("  ·  ").append(p.color);
        meta.setText(m.toString());

        double price = p.bestPrice();
        usd.setText(price > 0 ? Money.usd(price) : ctx.getString(R.string.no_price_yet));
        usd.setTextColor(price > 0 ? Ui.color(ctx, R.color.brand) : Ui.color(ctx, R.color.ink_faint));
        if (price > 0) {
            lbp.setVisibility(View.VISIBLE);
            lbp.setText(Money.lbp(ctx, price));
        } else {
            lbp.setVisibility(View.GONE);
        }

        qty.setText(String.valueOf(p.quantity));
        if (p.quantity <= 0) {
            qty.setBackgroundResource(R.drawable.pill_red);
            qty.setTextColor(Ui.color(ctx, R.color.red));
        } else if (p.quantity <= lowStock) {
            qty.setBackgroundResource(R.drawable.pill_amber);
            qty.setTextColor(Ui.color(ctx, R.color.amber));
        } else {
            qty.setBackgroundResource(R.drawable.pill_green);
            qty.setTextColor(Ui.color(ctx, R.color.green));
        }

        ImgUtil.loadThumb(photo, holder, p.imagePath, 220);
        return v;
    }
}
