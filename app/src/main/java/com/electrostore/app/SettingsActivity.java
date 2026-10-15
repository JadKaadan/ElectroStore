package com.electrostore.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class SettingsActivity extends Base {

    private EditText fShop, fShopInfo, fRate, fLow;
    private TextView rateEcho;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        fShop = (EditText) findViewById(R.id.fShop);
        fShopInfo = (EditText) findViewById(R.id.fShopInfo);
        fRate = (EditText) findViewById(R.id.fRate);
        fLow = (EditText) findViewById(R.id.fLow);
        rateEcho = (TextView) findViewById(R.id.rateEcho);

        Db db = Db.get(this);
        fShop.setText(db.setting("shop_name", ""));
        fShopInfo.setText(db.setting("shop_info", ""));
        fRate.setText(Money.plain(Money.rate(this)));
        fLow.setText(db.setting("low_stock", "3"));

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        paintLanguage();
        findViewById(R.id.langEn).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { chooseLanguage(Lang.EN); }
        });
        findViewById(R.id.langAr).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { chooseLanguage(Lang.AR); }
        });
        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { save(); }
        });
        findViewById(R.id.btnBackup).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Backup.send(SettingsActivity.this); }
        });
        findViewById(R.id.btnRestore).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickRestore(); }
        });
        findViewById(R.id.btnPinSet).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { choosePin(); }
        });
        findViewById(R.id.btnPinRemove).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { removePin(); }
        });
        paintPin();

        findViewById(R.id.btnPickPrinter).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickPrinter(); }
        });
        findViewById(R.id.btnTestPrint).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { testPrint(); }
        });
        findViewById(R.id.btnForgetPrinter).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BtPrinter.forget(SettingsActivity.this);
                paintPrinter();
            }
        });
        findViewById(R.id.btnCategories).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Categories.manage(SettingsActivity.this, null); }
        });
        findViewById(R.id.paper58).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { BtPrinter.setPaperMm(SettingsActivity.this, 58); paintPrinter(); }
        });
        findViewById(R.id.paper80).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { BtPrinter.setPaperMm(SettingsActivity.this, 80); paintPrinter(); }
        });
        paintPrinter();
        Ui.onType(fRate, new Ui.Watch() {
            public void onChanged(String s) { echoRate(); }
        });
        Keypad.attach(this, fRate, getString(R.string.lbl_rate), true,
                new Keypad.Echo() {
                    public String of(double typed) {
                        return typed > 0 ? getString(R.string.rate_echo,
                                new java.text.DecimalFormat("#,##0").format(typed)) : "";
                    }
                }, new Runnable() { public void run() { echoRate(); } });
        Keypad.attachWhole(this, fLow, getString(R.string.lbl_low_stock), null);

        echoRate();
        ((TextView) findViewById(R.id.stats)).setText(
                getString(R.string.stats_line, db.productCount(), db.stockCount())
                        + "\n" + getString(R.string.version_line));
    }


    private void paintLanguage() {
        boolean ar = Lang.isArabic(this);
        TextView en = (TextView) findViewById(R.id.langEn);
        TextView arab = (TextView) findViewById(R.id.langAr);
        TextView on = ar ? arab : en;
        TextView off = ar ? en : arab;
        on.setBackgroundResource(R.drawable.btn_brand);
        on.setTextColor(Ui.color(this, R.color.on_accent));
        off.setBackgroundResource(R.drawable.btn_outline);
        off.setTextColor(Ui.color(this, R.color.ink));
    }

    private void chooseLanguage(String lang) {
        if (lang.equals(Lang.current(this))) return;
        saveQuietly();
        Lang.set(this, lang);
        restartApp();
    }


    private void saveQuietly() {
        Db db = Db.get(this);
        db.setSetting("shop_name", fShop.getText().toString().trim());
        db.setSetting("shop_info", fShopInfo.getText().toString().trim());
        double r = Money.parse(fRate.getText());
        if (r > 0) Money.setRate(this, r);
        db.setSetting("low_stock", String.valueOf(Money.parseInt(fLow.getText())));
    }


    private void paintPrinter() {
        boolean set = BtPrinter.isConfigured(this);
        ((TextView) findViewById(R.id.printerStatus)).setText(
                set ? BtPrinter.savedName(this) : getString(R.string.printer_none));
        ((Button) findViewById(R.id.btnPickPrinter)).setText(
                set ? R.string.printer_change : R.string.printer_pick);
        findViewById(R.id.btnForgetPrinter).setVisibility(set ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnTestPrint).setEnabled(set);

        boolean wide = BtPrinter.paperMm(this) == 80;
        TextView p58 = (TextView) findViewById(R.id.paper58);
        TextView p80 = (TextView) findViewById(R.id.paper80);
        TextView on = wide ? p80 : p58;
        TextView off = wide ? p58 : p80;
        on.setBackgroundResource(R.drawable.btn_brand);
        on.setTextColor(Ui.color(this, R.color.on_accent));
        off.setBackgroundResource(R.drawable.btn_outline);
        off.setTextColor(Ui.color(this, R.color.ink));
    }

    private void pickPrinter() {
        if (!BtPrinter.hasPermission(this)) {
            BtPrinter.askPermission(this);
            return;
        }
        if (!BtPrinter.bluetoothOn(this)) {
            Ui.toast(this, getString(R.string.printer_bt_off));
            return;
        }
        final java.util.List<BtPrinter.Item> items = BtPrinter.paired(this);
        if (items.isEmpty()) {
            Ui.toast(this, getString(R.string.printer_none_paired));
            return;
        }
        String[] names = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            names[i] = items.get(i).name + "\n" + items.get(i).mac;
        }
        new android.app.AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(R.string.printer_pick)
                .setItems(names, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int which) {
                        BtPrinter.Item it = items.get(which);
                        BtPrinter.save(SettingsActivity.this, it.mac, it.name);
                        paintPrinter();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void testPrint() {
        if (!BtPrinter.isConfigured(this)) return;
        if (!BtPrinter.hasPermission(this)) { BtPrinter.askPermission(this); return; }
        Ui.toast(this, getString(R.string.printer_sending));
        byte[] page = EscPos.testPage(this, BtPrinter.dotsWide(this));
        BtPrinter.send(this, page, new BtPrinter.Result() {
            public void done(boolean ok, String message) {
                Ui.toast(SettingsActivity.this, ok
                        ? getString(R.string.printer_ok)
                        : getString(R.string.printer_failed, String.valueOf(message)));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] granted) {
        super.onRequestPermissionsResult(req, perms, granted);
        if (req == BtPrinter.REQ_PERMISSION) {
            if (granted.length > 0 && granted[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pickPrinter();
            } else {
                Ui.toast(this, getString(R.string.printer_need_permission));
            }
        }
    }


    private void paintPin() {
        boolean on = Lock.isSet(this);
        ((TextView) findViewById(R.id.pinStatus)).setText(on ? R.string.pin_on : R.string.pin_off);
        ((Button) findViewById(R.id.btnPinSet)).setText(
                on ? R.string.pin_change_btn : R.string.pin_set_btn);
        findViewById(R.id.btnPinRemove).setVisibility(on ? View.VISIBLE : View.GONE);
    }


    private void choosePin() {
        Lock.ask(this, getString(R.string.pin_title_set), getString(R.string.pin_sub_set),
                new Lock.OnPin() {
                    public boolean got(final String first) {
                        findViewById(R.id.btnPinSet).post(new Runnable() {
                            public void run() { confirmPin(first); }
                        });
                        return true;
                    }
                });
    }

    private void confirmPin(final String first) {
        Lock.ask(this, getString(R.string.pin_title_confirm), getString(R.string.pin_sub_set),
                new Lock.OnPin() {
                    public boolean got(String again) {
                        if (!again.equals(first)) {
                            Ui.toast(SettingsActivity.this, getString(R.string.pin_mismatch));
                            return false;
                        }
                        Lock.save(SettingsActivity.this, again);
                        Ui.toast(SettingsActivity.this, getString(R.string.pin_set_ok));
                        paintPin();
                        return true;
                    }
                });
    }

    private void removePin() {
        Lock.require(this, new Runnable() {
            public void run() {
                Lock.clear(SettingsActivity.this);
                Ui.toast(SettingsActivity.this, getString(R.string.pin_removed));
                paintPin();
            }
        });
    }

    private static final int REQ_RESTORE = 7701;


    private void pickRestore() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");


        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/octet-stream", "application/x-sqlite3",
                "application/vnd.sqlite3", "application/db", "*/*"});
        try {
            startActivityForResult(
                    Intent.createChooser(i, getString(R.string.restore_pick)), REQ_RESTORE);
            return;
        } catch (Throwable ignored) { }
        try {
            Intent g = new Intent(Intent.ACTION_GET_CONTENT);
            g.addCategory(Intent.CATEGORY_OPENABLE);
            g.setType("*/*");
            startActivityForResult(
                    Intent.createChooser(g, getString(R.string.restore_pick)), REQ_RESTORE);
        } catch (Throwable t) {
            Ui.toast(this, getString(R.string.restore_no_picker));
        }
    }

    @Override
    protected void onActivityResult(int req, int res, final Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_RESTORE || res != RESULT_OK || data == null || data.getData() == null) return;
        inspectThenRestore(data.getData());
    }


    private void inspectThenRestore(android.net.Uri uri) {
        final java.io.File staged;
        try {
            staged = Backup.stage(this, uri);
        } catch (Throwable t) {
            Ui.toast(this, getString(R.string.restore_unreadable));
            return;
        }

        final java.io.File work = new java.io.File(getCacheDir(), "restore_work");
        final Backup.Info info = Backup.inspect(this, staged, work);
        staged.delete();

        if (!info.valid) {
            Backup.wipe(work);
            Ui.toast(this, getString(R.string.restore_failed));
            return;
        }
        if (info.isEmpty()) {
            Backup.wipe(work);
            Ui.toast(this, getString(R.string.restore_empty));
            return;
        }

        String body = info.summary(this);
        if (!info.hasPhotoFolder) {


            body += "\n\n" + getString(R.string.restore_no_photos);
        }
        body += "\n\n" + getString(R.string.restore_msg);

        Ui.confirm(this, getString(R.string.restore_title), body,
                getString(R.string.restore_btn), new Runnable() {
                    public void run() {
                        try {
                            Backup.install(SettingsActivity.this, work);
                            Ui.toast(SettingsActivity.this, getString(R.string.restore_done));
                            restartApp();
                        } catch (Throwable t) {
                            Backup.wipe(work);
                            Ui.toast(SettingsActivity.this, getString(R.string.restore_failed));
                        }
                    }
                });
    }

    private void echoRate() {
        double r = Money.parse(fRate.getText());
        rateEcho.setText(r > 0
                ? getString(R.string.rate_echo, new java.text.DecimalFormat("#,##0").format(r))
                : getString(R.string.rate_prompt));
    }

    private void save() {
        saveQuietly();
        Ui.hideKeyboard(this);
        Ui.toast(this, getString(R.string.saved));
        finish();
    }

}
