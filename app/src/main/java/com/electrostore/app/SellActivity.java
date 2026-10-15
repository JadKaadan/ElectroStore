package com.electrostore.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class SellActivity extends Base {

    private Product p;
    private EditText fQty, fPrice;
    private TextView totalUsd, totalLbp, profitLine, curUsd, curLbp;
    private LinearLayout priceChoices;
    private boolean typingInLbp = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_sell);

        long id = getIntent().getLongExtra("id", 0);
        p = Db.get(this).product(id);
        if (p == null) { finish(); return; }

        fQty = (EditText) findViewById(R.id.fQty);
        fPrice = (EditText) findViewById(R.id.fPrice);
        totalUsd = (TextView) findViewById(R.id.totalUsd);
        totalLbp = (TextView) findViewById(R.id.totalLbp);
        profitLine = (TextView) findViewById(R.id.profitLine);
        curUsd = (TextView) findViewById(R.id.curUsd);
        curLbp = (TextView) findViewById(R.id.curLbp);
        priceChoices = (LinearLayout) findViewById(R.id.priceChoices);

        ((TextView) findViewById(R.id.sub)).setText(
                getString(R.string.product_number, p.displayNumber(this)));
        ((TextView) findViewById(R.id.name)).setText(p.displayName(this));
        ((TextView) findViewById(R.id.stock)).setText(
                getString(R.string.available_stock, p.quantity));

        ImageView photo = (ImageView) findViewById(R.id.photo);
        Bitmap bm = ImgUtil.thumb(p.imagePath, 220);
        if (bm != null) {
            photo.setImageBitmap(bm);
            findViewById(R.id.photoPlaceholder).setVisibility(View.GONE);
        }

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        findViewById(R.id.qtyMinus).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { bump(-1); }
        });
        findViewById(R.id.qtyPlus).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { bump(1); }
        });
        curUsd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchCurrency(false); }
        });
        curLbp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchCurrency(true); }
        });
        findViewById(R.id.btnSell).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirm(); }
        });

        Ui.Watch recompute = new Ui.Watch() {
            public void onChanged(String s) { draw(); }
        };
        Ui.onType(fQty, recompute);
        Ui.onType(fPrice, recompute);

        Runnable redraw = new Runnable() {
            public void run() { draw(); }
        };
        Keypad.attachWhole(this, fQty, getString(R.string.lbl_how_many_buying), redraw);
        Keypad.attach(this, fPrice, getString(R.string.lbl_price_each), true,
                new Keypad.Echo() {
                    public String of(double typed) {
                        if (typed <= 0) return "";
                        double usd = typingInLbp ? typed / Money.rate(SellActivity.this) : typed;
                        return typingInLbp ? "= " + Money.usd(usd)
                                : "= " + Money.lbp(SellActivity.this, usd);
                    }
                }, redraw);

        fPrice.setText(Money.plain(p.bestPrice()));
        buildChoices();
        draw();
    }

    private void buildChoices() {
        priceChoices.removeAllViews();
        addChoice(getString(R.string.choice_unit), p.unitPrice);
        addChoice(getString(R.string.choice_cost), p.costPrice);
    }

    private void addChoice(String label, final double usd) {
        if (usd <= 0) return;
        TextView t = new TextView(this);
        t.setText(label + "  " + Money.usd(usd));
        t.setTextSize(14);
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setBackgroundResource(R.drawable.chip);
        t.setTextColor(Ui.color(this, R.color.ink_soft));
        int px = Ui.dp(this, 14);
        t.setPadding(px, Ui.dp(this, 9), px, Ui.dp(this, 9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(this, 8);
        t.setLayoutParams(lp);
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                double shown = typingInLbp ? Math.round(usd * Money.rate(SellActivity.this)) : usd;
                fPrice.setText(Money.plain(shown));
            }
        });
        priceChoices.addView(t);
    }

    private void bump(int delta) {
        int q = Money.parseInt(fQty.getText()) + delta;
        if (q < 1) q = 1;
        fQty.setText(String.valueOf(q));
    }

    private void switchCurrency(boolean toLbp) {
        if (toLbp == typingInLbp) return;
        double rate = Money.rate(this);
        double v = Money.parse(fPrice.getText());
        if (v > 0) {
            fPrice.setText(Money.plain(toLbp
                    ? Math.round(v * rate)
                    : Math.round(v / rate * 100.0) / 100.0));
        }
        typingInLbp = toLbp;
        TextView on = toLbp ? curLbp : curUsd;
        TextView off = toLbp ? curUsd : curLbp;
        on.setBackgroundResource(R.drawable.chip_on);
        on.setTextColor(Ui.color(this, R.color.on_accent));
        off.setBackgroundResource(R.drawable.chip);
        off.setTextColor(Ui.color(this, R.color.ink_soft));
        draw();
    }

    private double priceUsd() {
        double v = Money.parse(fPrice.getText());
        return typingInLbp ? v / Money.rate(this) : v;
    }

    private void draw() {
        int qty = Math.max(1, Money.parseInt(fQty.getText()));
        double unit = priceUsd();
        double total = unit * qty;
        totalUsd.setText(Money.usd(total));
        totalLbp.setText(Money.lbp(this, total));

        if (p.costPrice > 0 && unit > 0) {
            double profit = (unit - p.costPrice) * qty;
            profitLine.setVisibility(View.VISIBLE);
            if (profit >= 0) {
                profitLine.setBackgroundResource(R.drawable.pill_green);
                profitLine.setTextColor(Ui.color(this, R.color.green));
                profitLine.setText(getString(R.string.profit_on_sale, Money.usd(profit)));
            } else {
                profitLine.setBackgroundResource(R.drawable.pill_red);
                profitLine.setTextColor(Ui.color(this, R.color.red));
                profitLine.setText(getString(R.string.losing_on_sale, Money.usd(-profit)));
            }
        } else {
            profitLine.setVisibility(View.GONE);
        }
    }

    private void confirm() {
        final int qty = Math.max(1, Money.parseInt(fQty.getText()));
        final double unit = priceUsd();

        if (qty > p.quantity) {
            Ui.confirm(this, getString(R.string.not_enough_title),
                    getString(R.string.not_enough_msg, p.quantity, p.displayName(this), qty),
                    getString(R.string.sell_anyway), new Runnable() {
                        public void run() { doSell(qty, unit); }
                    });
            return;
        }
        doSell(qty, unit);
    }

    private void doSell(int qty, double unit) {
        try { Db.get(this).sell(p, qty, unit, ""); }
        catch (RuntimeException e) {
            Ui.toast(this, getString(R.string.stock_save_failed));
            return;
        }
        Ui.toast(this, getString(R.string.sold_toast, qty, p.displayName(this),
                Money.usd(unit * qty)));
        finish();
    }
}
