package com.electrostore.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class ProductDetailActivity extends Base {

    private long id;
    private Product p;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_product_detail);
        id = getIntent().getLongExtra("id", 0);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        findViewById(R.id.edit).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Lock.require(ProductDetailActivity.this, new Runnable() {
                    public void run() {
                        Intent i = new Intent(ProductDetailActivity.this,
                                EditProductActivity.class);
                        i.putExtra("id", id);
                        startActivityForResult(i, 1);
                    }
                });
            }
        });
        findViewById(R.id.btnSell).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(ProductDetailActivity.this, SellActivity.class);
                i.putExtra("id", id);
                startActivity(i);
            }
        });
        findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Lock.require(ProductDetailActivity.this, new Runnable() {
                    public void run() { askDelete(); }
                });
            }
        });
        findViewById(R.id.photo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (p == null || p.imagePath.length() == 0) return;
                Intent i = new Intent(ProductDetailActivity.this, PhotoViewActivity.class);
                i.putExtra("path", p.imagePath);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res == RESULT_FIRST_USER) finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        draw();
    }

    private void draw() {
        p = Db.get(this).product(id);
        if (p == null) { finish(); return; }

        ((TextView) findViewById(R.id.name)).setText(p.displayName(this));
        ((TextView) findViewById(R.id.idBadge)).setText(
                getString(R.string.product_position, p.displayNumber(this), Db.get(this).productCount()));

        ImageView photo = (ImageView) findViewById(R.id.photo);
        Bitmap bm = ImgUtil.thumb(p.imagePath, 1200);
        if (bm != null) photo.setImageBitmap(bm); else photo.setImageDrawable(null);

        LinearLayout tags = (LinearLayout) findViewById(R.id.tagRow);
        tags.removeAllViews();
        if (p.category.length() > 0) addTag(tags, p.category, R.drawable.pill_brand, R.color.brand);
        if (p.color.length() > 0) addTag(tags, p.color, R.drawable.pill_cyan, R.color.cyan);

        ((TextView) findViewById(R.id.unitUsd)).setText(
                p.unitPrice > 0 ? Money.usd(p.unitPrice) : "—");
        ((TextView) findViewById(R.id.unitLbp)).setText(
                p.unitPrice > 0 ? Money.lbp(this, p.unitPrice) : getString(R.string.no_price_set));


        ((TextView) findViewById(R.id.retailUsd)).setText(
                p.costPrice > 0 ? Money.usd(p.costPrice) : "—");
        ((TextView) findViewById(R.id.retailLbp)).setText(
                p.costPrice > 0 ? Money.lbp(this, p.costPrice) : "");

        double paidForAll = p.costPrice * p.quantity;
        ((TextView) findViewById(R.id.stockPaidUsd)).setText(
                paidForAll > 0 ? Money.usd(paidForAll) : "—");
        ((TextView) findViewById(R.id.stockPaidLbp)).setText(
                paidForAll > 0
                        ? getString(R.string.wholesale_total,
                                Money.lbp(this, paidForAll), String.valueOf(p.quantity))
                        : "");

        TextView profit = (TextView) findViewById(R.id.profit);
        double margin = p.profitPerUnit();
        if (p.bestPrice() > 0 && p.costPrice > 0) {
            profit.setVisibility(View.VISIBLE);
            if (margin >= 0) {
                profit.setBackgroundResource(R.drawable.pill_green);
                profit.setTextColor(Ui.color(this, R.color.green));
                profit.setText(getString(R.string.earn_total, Money.usd(margin),
                        Money.usd(margin * p.quantity), p.quantity));
            } else {
                profit.setBackgroundResource(R.drawable.pill_red);
                profit.setTextColor(Ui.color(this, R.color.red));
                profit.setText(getString(R.string.lose_each, Money.usd(-margin)));
            }
        } else {
            profit.setVisibility(View.GONE);
        }

        TextView qty = (TextView) findViewById(R.id.qty);
        qty.setText(String.valueOf(p.quantity));
        int lowStock = 3;
        try { lowStock = Integer.parseInt(Db.get(this).setting("low_stock", "3")); }
        catch (Exception ignored) { }
        if (p.quantity <= 0) qty.setTextColor(Ui.color(this, R.color.red));
        else if (p.quantity <= lowStock) qty.setTextColor(Ui.color(this, R.color.amber));
        else qty.setTextColor(Ui.color(this, R.color.ink));

        boolean hasDesc = p.description.trim().length() > 0;
        findViewById(R.id.descBox).setVisibility(hasDesc ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.desc)).setText(p.description);
    }

    private void addTag(LinearLayout row, String text, int bg, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setBackgroundResource(bg);
        t.setTextColor(Ui.color(this, color));
        int px = Ui.dp(this, 12);
        t.setPadding(px, Ui.dp(this, 7), px, Ui.dp(this, 7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(this, 8);
        t.setLayoutParams(lp);
        row.addView(t);
    }

    private void askDelete() {
        if (p == null) return;
        Ui.confirm(this, getString(R.string.confirm_delete_title),
                getString(R.string.confirm_delete_msg, p.displayName(this)),
                getString(R.string.delete), new Runnable() {
                    public void run() {
                        ImgUtil.delete(p.imagePath);
                        Db.get(ProductDetailActivity.this).deleteProduct(id);
                        Ui.toast(ProductDetailActivity.this, getString(R.string.deleted));
                        finish();
                    }
                });
    }
}
