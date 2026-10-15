package com.electrostore.app;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends Base {

    public static final int TAB_PRODUCTS = 0, TAB_ADD = 1, TAB_SELL = 2,
            TAB_REVENUE = 3, TAB_ORDERS = 4, TAB_MAINTENANCE = 5;

    private final int[] cellIds = {R.id.nav0, R.id.nav1, R.id.nav2, R.id.nav3, R.id.nav4, R.id.nav5};
    private final int[] iconIds = {R.id.nav0i, R.id.nav1i, R.id.nav2i, R.id.nav3i, R.id.nav4i, R.id.nav5i};
    private final int[] textIds = {R.id.nav0t, R.id.nav1t, R.id.nav2t, R.id.nav3t, R.id.nav4t, R.id.nav5t};
    private int[] tabColors;

    private FrameLayout host;
    private View drawerRoot, drawerScrim, drawerPanel;
    private Page[] pages = new Page[6];
    private int current = -1;
    private boolean drawerOpen = false;
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        StockAlerts.requestPermission(this);

        tabColors = new int[]{
                Ui.color(this, R.color.tab_products), Ui.color(this, R.color.tab_add),
                Ui.color(this, R.color.tab_sell), Ui.color(this, R.color.tab_revenue),
                Ui.color(this, R.color.tab_orders), Ui.color(this, R.color.ink)};

        host = (FrameLayout) findViewById(R.id.host);
        drawerRoot = findViewById(R.id.drawerRoot);
        drawerScrim = findViewById(R.id.drawerScrim);
        drawerPanel = findViewById(R.id.drawerPanel);

        for (int i = 0; i < cellIds.length; i++) {
            final int index = i;
            findViewById(cellIds[i]).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { select(index); }
            });
        }
        wireDrawer();
        select(state == null ? TAB_PRODUCTS : state.getInt("selected_tab", TAB_PRODUCTS));
        if (state != null && pages[TAB_MAINTENANCE] instanceof MaintenancePage)
            ((MaintenancePage) pages[TAB_MAINTENANCE]).restoreState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (current >= 0 && pages[current] != null) pages[current].refresh();
        paintDrawerHeader();
    }


    public void select(int index) {
        closeDrawer();
        if (index == current) {
            pages[index].refresh();
            return;
        }
        current = index;
        if (pages[index] == null) pages[index] = build(index);
        host.removeAllViews();
        host.addView(pages[index], new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        pages[index].refresh();
        paintNav(index);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("selected_tab", current);
        if (pages[TAB_MAINTENANCE] instanceof MaintenancePage)
            ((MaintenancePage) pages[TAB_MAINTENANCE]).saveState(state);
        super.onSaveInstanceState(state);
    }

    private Page build(int index) {
        switch (index) {
            case TAB_ADD: return new AddPage(this);
            case TAB_SELL: return new SellPage(this);
            case TAB_REVENUE: return new RevenuePage(this);
            case TAB_ORDERS: return new OrdersPage(this);
            case TAB_MAINTENANCE: return new MaintenancePage(this);
            default: return new ProductsPage(this);
        }
    }

    private void paintNav(int active) {
        for (int i = 0; i < cellIds.length; i++) {
            boolean on = i == active;
            int color = on ? tabColors[i] : Ui.color(this, R.color.ink_faint);
            ImageView icon = (ImageView) findViewById(iconIds[i]);
            TextView label = (TextView) findViewById(textIds[i]);
            Ui.tint(icon, color);
            label.setTextColor(color);
            icon.setBackground(on ? pill(color) : null);
        }
    }


    private Drawable pill(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(Ui.dp(this, 15));
        d.setColor((color & 0x00FFFFFF) | 0x26000000);
        return d;
    }

    private static int darker(int color) {
        int r = (int) (((color >> 16) & 0xFF) * 0.78f);
        int g = (int) (((color >> 8) & 0xFF) * 0.78f);
        int b = (int) ((color & 0xFF) * 0.78f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }


    private void wireDrawer() {
        drawerScrim.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { closeDrawer(); }
        });
        findViewById(R.id.mCalendar).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeDrawer();
                startActivity(new Intent(MainActivity.this, CalendarActivity.class));
            }
        });
        findViewById(R.id.mSettings).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeDrawer();
                Lock.require(MainActivity.this, new Runnable() {
                    public void run() {
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    }
                });
            }
        });
        findViewById(R.id.mLangEn).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { chooseLanguage(Lang.EN); }
        });
        findViewById(R.id.mLangAr).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { chooseLanguage(Lang.AR); }
        });
        paintDrawerHeader();
    }

    private void chooseLanguage(String lang) {
        if (lang.equals(Lang.current(this))) { closeDrawer(); return; }
        Lang.set(this, lang);
        restartApp();
    }

    private void paintDrawerHeader() {
        Db db = Db.get(this);
        String shop = db.setting("shop_name", "");
        if (shop.trim().length() == 0) shop = getString(R.string.app_name);
        ((TextView) findViewById(R.id.drawerShop)).setText(shop);
        ((TextView) findViewById(R.id.drawerStats)).setText(
                getString(R.string.stats_line, db.productCount(), db.stockCount()));


        double selling = db.stockSellingValue();
        double paid = db.stockValue();
        ((TextView) findViewById(R.id.assetSellUsd)).setText(Money.usd(selling));
        ((TextView) findViewById(R.id.assetSellLbp)).setText(Money.lbp(this, selling));
        ((TextView) findViewById(R.id.assetCostUsd)).setText(Money.usd(paid));
        ((TextView) findViewById(R.id.assetCostLbp)).setText(Money.lbp(this, paid));

        boolean ar = Lang.isArabic(this);
        TextView en = (TextView) findViewById(R.id.mLangEn);
        TextView arb = (TextView) findViewById(R.id.mLangAr);
        TextView on = ar ? arb : en;
        TextView off = ar ? en : arb;
        on.setBackgroundResource(R.drawable.btn_brand);
        on.setTextColor(Ui.color(this, R.color.on_accent));
        off.setBackgroundResource(R.drawable.btn_outline);
        off.setTextColor(Ui.color(this, R.color.ink));
    }

    public void openDrawer() {
        if (drawerOpen) return;
        drawerOpen = true;
        paintDrawerHeader();
        drawerRoot.setVisibility(View.VISIBLE);
        drawerScrim.setAlpha(0f);
        drawerScrim.animate().alpha(1f).setDuration(180).start();

        final float hidden = offscreenX();
        drawerPanel.setTranslationX(hidden);
        drawerPanel.animate().translationX(0f).setDuration(220).start();
    }

    public void closeDrawer() {
        if (!drawerOpen) return;
        drawerOpen = false;
        drawerScrim.animate().alpha(0f).setDuration(160).start();
        drawerPanel.animate().translationX(offscreenX()).setDuration(180)
                .withEndAction(new Runnable() {
                    public void run() { drawerRoot.setVisibility(View.GONE); }
                }).start();
    }


    private float offscreenX() {
        int w = drawerPanel.getWidth();
        if (w == 0) w = Ui.dp(this, 300);
        boolean rtl = getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
        return rtl ? w : -w;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (Page p : pages) {
            if (p != null && p.onResult(requestCode, resultCode, data)) return;
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) closeDrawer();
        else if (current != TAB_PRODUCTS) select(TAB_PRODUCTS);
        else super.onBackPressed();
    }
}
