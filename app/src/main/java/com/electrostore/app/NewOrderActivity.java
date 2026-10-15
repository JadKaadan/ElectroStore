package com.electrostore.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class NewOrderActivity extends Base {

    private final List<OrderItem> lines = new ArrayList<OrderItem>();
    private EditText fCustomer, fPhone, fPaid, fNote;
    private LinearLayout linesBox;
    private TextView noLines, totalUsd, totalLbp, balance, curUsd, curLbp;
    private String receiptNo;
    private boolean paidInLbp = false;
    private long editingId = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_new_order);

        fCustomer = (EditText) findViewById(R.id.fCustomer);
        fPhone = (EditText) findViewById(R.id.fPhone);
        fPaid = (EditText) findViewById(R.id.fPaid);
        fNote = (EditText) findViewById(R.id.fNote);
        linesBox = (LinearLayout) findViewById(R.id.lines);
        noLines = (TextView) findViewById(R.id.noLines);
        totalUsd = (TextView) findViewById(R.id.totalUsd);
        totalLbp = (TextView) findViewById(R.id.totalLbp);
        balance = (TextView) findViewById(R.id.balance);
        curUsd = (TextView) findViewById(R.id.curUsd);
        curLbp = (TextView) findViewById(R.id.curLbp);

        editingId = getIntent().getLongExtra("id", 0);
        if (editingId > 0) {
            Order o = Db.get(this).order(editingId);
            if (o != null) {
                receiptNo = o.receiptNo;
                fCustomer.setText(o.customer);
                fPhone.setText(o.phone);
                fPaid.setText(Money.plain(o.paid));
                fNote.setText(o.note);
                lines.addAll(o.items);
            }
        }
        if (receiptNo == null) receiptNo = Db.get(this).nextReceiptNo();
        ((TextView) findViewById(R.id.receiptNo)).setText(
                getString(R.string.receipt_no_label, receiptNo));

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        findViewById(R.id.btnAddItem).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addItem(); }
        });
        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { save(); }
        });
        curUsd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchPaidCurrency(false); }
        });
        curLbp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchPaidCurrency(true); }
        });
        Ui.onType(fPaid, new Ui.Watch() {
            public void onChanged(String s) { draw(); }
        });
        Keypad.attach(this, fPaid, getString(R.string.lbl_deposit), true,
                new Keypad.Echo() {
                    public String of(double typed) {
                        if (typed <= 0) return "";
                        double usd = paidInLbp ? typed / Money.rate(NewOrderActivity.this) : typed;
                        return paidInLbp ? "= " + Money.usd(usd)
                                : "= " + Money.lbp(NewOrderActivity.this, usd);
                    }
                }, new Runnable() { public void run() { draw(); } });
        draw();
    }

    private void addItem() {
        if (Db.get(this).productCount() == 0) {
            Ui.toast(this, getString(R.string.add_product_first));
            return;
        }
        Picker.chooseProduct(this, new Picker.OnProduct() {
            public void picked(final Product p) {
                Picker.chooseLine(NewOrderActivity.this, p, new Picker.OnLine() {
                    public void picked(Product prod, int qty, double unit) {
                        OrderItem it = new OrderItem();
                        it.productId = prod.id;
                        it.productName = prod.displayName(NewOrderActivity.this);
                        it.qty = qty;
                        it.unitPrice = unit;
                        it.lineTotal = unit * qty;
                        lines.add(it);
                        draw();
                    }
                });
            }
        });
    }

    private void switchPaidCurrency(boolean toLbp) {
        if (toLbp == paidInLbp) return;
        double rate = Money.rate(this);
        double v = Money.parse(fPaid.getText());
        if (v > 0) {
            fPaid.setText(Money.plain(toLbp
                    ? Math.round(v * rate)
                    : Math.round(v / rate * 100.0) / 100.0));
        }
        paidInLbp = toLbp;
        TextView on = toLbp ? curLbp : curUsd;
        TextView off = toLbp ? curUsd : curLbp;
        on.setBackgroundResource(R.drawable.chip_on);
        on.setTextColor(Ui.color(this, R.color.on_accent));
        off.setBackgroundResource(R.drawable.chip);
        off.setTextColor(Ui.color(this, R.color.ink_soft));
        draw();
    }

    private double paidUsd() {
        double v = Money.parse(fPaid.getText());
        return paidInLbp ? v / Money.rate(this) : v;
    }

    private double total() {
        double t = 0;
        for (OrderItem it : lines) t += it.lineTotal;
        return t;
    }

    private void draw() {
        linesBox.removeAllViews();
        for (int i = 0; i < lines.size(); i++) {
            final int index = i;
            final OrderItem it = lines.get(i);
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_order_line, linesBox, false);
            ((TextView) row.findViewById(R.id.name)).setText(it.productName);
            ((TextView) row.findViewById(R.id.detail)).setText(
                    it.qty + " × " + Money.usd(it.unitPrice));
            ((TextView) row.findViewById(R.id.total)).setText(Money.usd(it.lineTotal));
            row.findViewById(R.id.del).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    lines.remove(index);
                    draw();
                }
            });
            linesBox.addView(row);
        }
        Ui.show(noLines, lines.isEmpty());

        double t = total();
        totalUsd.setText(Money.usd(t));
        totalLbp.setText(Money.lbp(this, t));

        double bal = t - paidUsd();
        if (bal <= 0.005) {
            balance.setBackgroundResource(R.drawable.pill_green);
            balance.setTextColor(Ui.color(this, R.color.green));
            balance.setText(R.string.fully_paid);
        } else {
            balance.setBackgroundResource(R.drawable.pill_pink);
            balance.setTextColor(Ui.color(this, R.color.pink));
            balance.setText(getString(R.string.to_pay_on_collection,
                    Money.usd(bal), Money.lbp(this, bal)));
        }
    }

    private void save() {
        if (lines.isEmpty()) {
            Ui.toast(this, getString(R.string.add_one_item));
            return;
        }
        Order o = new Order();
        o.id = editingId;
        o.receiptNo = receiptNo;
        o.customer = fCustomer.getText().toString();
        o.phone = fPhone.getText().toString();
        o.note = fNote.getText().toString();
        o.total = total();
        o.paid = paidUsd();
        o.status = "OPEN";
        o.day = Db.today();
        o.ts = System.currentTimeMillis();
        o.items = lines;

        long id = Db.get(this).saveOrder(o);
        Ui.hideKeyboard(this);
        Intent i = new Intent(this, ReceiptActivity.class);
        i.putExtra("id", id);
        startActivity(i);
        finish();
    }
}
