package com.electrostore.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ReceiptActivity extends Base {

    private Order order;
    private WebView web;
    private boolean ready = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_receipt);

        long id = getIntent().getLongExtra("id", 0);
        order = Db.get(this).order(id);
        if (order == null) { finish(); return; }

        web = (WebView) findViewById(R.id.web);
        web.getSettings().setJavaScriptEnabled(false);
        web.setBackgroundColor(Ui.color(this, R.color.bg));
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) { ready = true; }
        });

        ((TextView) findViewById(R.id.sub)).setText(order.receiptNo);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        findViewById(R.id.btnPrint).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { print(); }
        });
        findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { share(); }
        });
        findViewById(R.id.btnStatus).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { toggleStatus(); }
        });
        findViewById(R.id.btnDeleteOrder).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { askDeleteOrder(); }
        });
        render();
    }

    private void render() {
        ready = false;
        web.loadDataWithBaseURL(null, buildHtml(), "text/html", "UTF-8", null);
        Button status = (Button) findViewById(R.id.btnStatus);
        boolean done = "DONE".equals(order.status);
        status.setText(done ? R.string.reopen_order : R.string.mark_collected);
        status.setTextColor(Ui.color(this, done ? R.color.amber : R.color.ink));
    }

    private void toggleStatus() {
        final boolean done = "DONE".equals(order.status);
        if (done) {
            Ui.confirm(this, getString(R.string.reopen_title),
                    getString(R.string.reopen_msg, Money.usd(order.total)),
                    getString(R.string.reopen_yes), new Runnable() {
                        public void run() { applyStatus(false); }
                    });
        } else {
            Ui.confirm(this, getString(R.string.collected_title),
                    getString(R.string.collected_msg, Money.usd(order.total)),
                    getString(R.string.collected_yes), new Runnable() {
                        public void run() { applyStatus(true); }
                    });
        }
    }

    private void applyStatus(boolean collected) {
        Db db = Db.get(this);
        if (collected) {
            db.bookOrderAsSales(order);
            order.status = "DONE";
        } else {
            db.unbookOrder(order.receiptNo);
            order.status = "OPEN";
        }

        Ui.toast(this, collected
                ? getString(R.string.collected_toast, Money.usd(order.total))
                : getString(R.string.reopened_toast));
        render();
    }

    private void askDeleteOrder() {
        Lock.require(this, new Runnable() {
            public void run() { confirmDeleteOrder(); }
        });
    }

    private void confirmDeleteOrder() {
        final boolean collected = "DONE".equals(order.status);
        String msg = collected
                ? getString(R.string.delete_collected_order_msg, order.receiptNo)
                : getString(R.string.delete_order_msg, order.receiptNo);
        Ui.confirm(this, getString(R.string.delete_order_title), msg,
                getString(R.string.delete), new Runnable() {
                    public void run() {
                        Db db = Db.get(ReceiptActivity.this);


                        db.deleteOrder(order.id);
                        Ui.toast(ReceiptActivity.this, getString(R.string.order_deleted));
                        finish();
                    }
                });
    }

    private String shopName() {
        return Db.get(this).setting("shop_name", getString(R.string.app_name));
    }

    private String shopInfo() {
        return Db.get(this).setting("shop_info", "");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String buildHtml() {
        String when = new SimpleDateFormat("d MMMM yyyy  ·  h:mm a", Locale.US)
                .format(new Date(order.ts == 0 ? System.currentTimeMillis() : order.ts));

        StringBuilder rows = new StringBuilder();
        for (OrderItem it : order.items) {
            rows.append("<tr><td class='n'>").append(esc(it.productName))
                    .append("<div class='sub'>").append(it.qty).append(" × ")
                    .append(Money.usd(it.unitPrice)).append("</div></td>")
                    .append("<td class='q'>").append(it.qty).append("</td>")
                    .append("<td class='a'>").append(Money.usd(it.lineTotal))
                    .append("</td></tr>");
        }

        double bal = order.balance();
        StringBuilder pay = new StringBuilder();
        pay.append("<div class='row big'><span>").append(getString(R.string.rc_total))
                .append("</span><span>")
                .append(Money.usd(order.total)).append("</span></div>");
        pay.append("<div class='row lira'><span></span><span>")
                .append(Money.lbp(this, order.total)).append("</span></div>");
        if (order.paid > 0.005) {
            pay.append("<div class='row'><span>").append(getString(R.string.rc_deposit))
                    .append("</span><span>- ")
                    .append(Money.usd(order.paid)).append("</span></div>");
        }
        if (bal > 0.005) {
            pay.append("<div class='row due'><span>").append(getString(R.string.rc_to_pay))
                    .append("</span><span>")
                    .append(Money.usd(bal)).append("</span></div>");
            pay.append("<div class='row lira'><span></span><span>")
                    .append(Money.lbp(this, bal)).append("</span></div>");
        } else {
            pay.append("<div class='row paid'><span>").append(getString(R.string.rc_paid_full))
                    .append("</span><span>✓</span></div>");
        }

        String customer = order.customer == null || order.customer.trim().length() == 0
                ? getString(R.string.walk_in) : esc(order.customer);

        boolean rtl = getResources().getConfiguration().getLayoutDirection()
                == android.view.View.LAYOUT_DIRECTION_RTL;
        return "<!doctype html><html dir='" + (rtl ? "rtl" : "ltr") + "'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "*{box-sizing:border-box}"
                + "body{margin:0;padding:14px;background:transparent;"
                + "font-family:-apple-system,Roboto,'Helvetica Neue',Arial,sans-serif;color:#14162B}"
                + ".paper{background:#fff;border-radius:14px;padding:24px 22px;"
                + "box-shadow:0 2px 10px rgba(0,0,0,.07);max-width:620px;margin:0 auto}"
                + "h1{margin:0;font-size:22px;letter-spacing:.5px}"
                + ".info{color:#5C6180;font-size:13px;margin-top:3px}"
                + ".rn{margin-top:18px;background:#FDE9F5;color:#C81D86;border-radius:10px;"
                + "padding:12px 14px;font-weight:700;font-size:17px;letter-spacing:1px;"
                + "display:flex;justify-content:space-between;align-items:center}"
                + ".rn small{font-weight:600;font-size:11px;letter-spacing:.4px;opacity:.75}"
                + ".meta{margin-top:16px;font-size:14px;line-height:1.7;color:#5C6180}"
                + ".meta b{color:#14162B}"
                + "table{width:100%;border-collapse:collapse;margin-top:18px}"
                + "th{text-align:left;font-size:11px;letter-spacing:.6px;color:#9AA0BD;"
                + "border-bottom:2px solid #E3E7F3;padding:0 0 7px}"
                + "th.q,td.q{text-align:center;width:44px}"
                + "th.a,td.a{text-align:right;width:92px}"
                + "td{padding:11px 0;border-bottom:1px solid #EEF1F8;font-size:15px;"
                + "vertical-align:top}"
                + "td.n{font-weight:600}"
                + ".sub{font-weight:400;font-size:12px;color:#9AA0BD;margin-top:2px}"
                + "td.a{font-weight:700}"
                + ".row{display:flex;justify-content:space-between;font-size:14px;"
                + "padding:7px 0;color:#5C6180}"
                + ".row.big{font-size:20px;font-weight:800;color:#14162B;padding-top:14px}"
                + ".row.lira{font-size:13px;color:#9AA0BD;padding-top:0}"
                + ".row.due{margin-top:10px;background:#FDE9F5;color:#C81D86;font-weight:800;"
                + "padding:12px 14px;border-radius:10px;font-size:15px}"
                + ".row.paid{margin-top:10px;background:#E1F7F0;color:#00996B;font-weight:800;"
                + "padding:12px 14px;border-radius:10px;font-size:15px}"
                + ".note{margin-top:16px;background:#FFF4E0;color:#8A5A00;border-radius:10px;"
                + "padding:12px 14px;font-size:14px;line-height:1.55}"
                + ".foot{margin-top:22px;text-align:center;color:#9AA0BD;font-size:12px;"
                + "border-top:1px dashed #D8DDEC;padding-top:16px;line-height:1.7}"
                + "@media print{body{background:#fff;padding:0}"
                + ".paper{box-shadow:none;border-radius:0;padding:0;max-width:none}}"
                + "</style></head><body><div class='paper'>"
                + "<h1>" + esc(shopName()) + "</h1>"
                + (shopInfo().length() > 0 ? "<div class='info'>" + esc(shopInfo()) + "</div>" : "")
                + "<div class='rn'><span dir='ltr'>" + esc(order.receiptNo) + "</span>"
                + "<small>" + getString(R.string.rc_order_receipt) + "</small></div>"
                + "<div class='meta'><b>" + customer + "</b>"
                + (order.phone.length() > 0
                    ? " &nbsp;·&nbsp; <span dir='ltr'>" + esc(order.phone) + "</span>" : "")
                + "<br>" + when + "</div>"
                + "<table><tr><th>" + getString(R.string.rc_item) + "</th>"
                + "<th class='q'>" + getString(R.string.rc_qty) + "</th>"
                + "<th class='a'>" + getString(R.string.rc_amount) + "</th></tr>"
                + rows + "</table>"
                + pay
                + (order.note.trim().length() > 0
                    ? "<div class='note'>" + esc(order.note).replace("\n", "<br>") + "</div>" : "")
                + "<div class='foot'>" + getString(R.string.rc_thanks) + "<br>"
                + getString(R.string.rc_keep)
                + "</div></div></body></html>";
    }


    private void print() {
        if (BtPrinter.isConfigured(this)) {
            printThermal();
            return;
        }
        printViaSystem();
    }

    private void printThermal() {
        if (!BtPrinter.hasPermission(this)) {
            BtPrinter.askPermission(this);
            return;
        }
        if (!BtPrinter.bluetoothOn(this)) {
            Ui.toast(this, getString(R.string.printer_bt_off));
            return;
        }
        Ui.toast(this, getString(R.string.printer_sending));
        try {
            android.graphics.Bitmap bm =
                    EscPos.render(this, order, BtPrinter.dotsWide(this));
            byte[] payload = EscPos.bytes(bm);
            bm.recycle();
            BtPrinter.send(this, payload, new BtPrinter.Result() {
                public void done(boolean ok, String message) {
                    Ui.toast(ReceiptActivity.this, ok
                            ? getString(R.string.printer_ok)
                            : getString(R.string.printer_failed, String.valueOf(message)));
                }
            });
        } catch (Throwable t) {
            Ui.toast(this, getString(R.string.printer_failed, String.valueOf(t.getMessage())));
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] granted) {
        super.onRequestPermissionsResult(req, perms, granted);
        if (req == BtPrinter.REQ_PERMISSION) {
            if (granted.length > 0
                    && granted[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                printThermal();
            } else {
                Ui.toast(this, getString(R.string.printer_need_permission));
            }
        }
    }

    private void printViaSystem() {
        if (!ready) { Ui.toast(this, getString(R.string.receipt_drawing)); return; }
        try {
            PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
            if (pm == null) { Ui.toast(this, getString(R.string.print_unavailable)); return; }
            PrintDocumentAdapter adapter =
                    web.createPrintDocumentAdapter(
                            getString(R.string.receipt_no_label, order.receiptNo));
            pm.print(getString(R.string.receipt_no_label, order.receiptNo), adapter,
                    new PrintAttributes.Builder().build());
        } catch (Throwable t) {
            Ui.toast(this, getString(R.string.print_failed));
        }
    }

    private void share() {
        StringBuilder sb = new StringBuilder();
        sb.append(shopName()).append("\n");
        if (shopInfo().length() > 0) sb.append(shopInfo()).append("\n");
        sb.append("\n").append(getString(R.string.receipt_no_label, order.receiptNo)).append("\n");
        String who = order.customer == null || order.customer.trim().length() == 0
                ? getString(R.string.walk_in) : order.customer;
        sb.append(who);
        if (order.phone.length() > 0) sb.append(" · ").append(order.phone);
        sb.append("\n\n");
        for (OrderItem it : order.items) {
            sb.append(it.qty).append(" × ").append(it.productName)
                    .append("  ").append(Money.usd(it.lineTotal)).append("\n");
        }
        sb.append("\n").append(getString(R.string.rc_total)).append(": ")
                .append(Money.usd(order.total))
                .append("  (").append(Money.lbp(this, order.total)).append(")\n");
        if (order.paid > 0.005) {
            sb.append(getString(R.string.rc_deposit)).append(": ")
                    .append(Money.usd(order.paid)).append("\n");
        }
        double bal = order.balance();
        if (bal > 0.005) {
            sb.append(getString(R.string.rc_to_pay)).append(": ")
                    .append(Money.usd(bal)).append("\n");
        }
        if (order.note.trim().length() > 0) sb.append("\n").append(order.note).append("\n");

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.receipt_no_label, order.receiptNo));
        i.putExtra(Intent.EXTRA_TEXT, sb.toString());
        try {
            startActivity(Intent.createChooser(i, getString(R.string.send_receipt)));
        } catch (Throwable t) {
            Ui.toast(this, getString(R.string.nothing_can_send));
        }
    }
}
