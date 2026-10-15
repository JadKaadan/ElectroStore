package com.electrostore.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ProductsPage extends Page {

    private static final int PREVIEW_PER_SECTION = 4;
    private static final int PAGE_SIZE = 30;

    private final EditText search;
    private final LinearLayout catStrip, sections;
    private final View empty, catScroll;
    private final TextView emptyText, subCount, catLabel;
    private final ImageView clearSearch;

    private String category = "";
    private int filteredLimit = PAGE_SIZE;

    public ProductsPage(Context c) {
        super(c);
        LayoutInflater.from(c).inflate(R.layout.page_products, this, true);

        search = (EditText) findViewById(R.id.search);
        catStrip = (LinearLayout) findViewById(R.id.catStrip);
        catScroll = findViewById(R.id.catScroll);
        catLabel = (TextView) findViewById(R.id.catLabel);
        sections = (LinearLayout) findViewById(R.id.sections);
        empty = findViewById(R.id.empty);
        emptyText = (TextView) findViewById(R.id.emptyText);
        subCount = (TextView) findViewById(R.id.subCount);
        clearSearch = (ImageView) findViewById(R.id.clearSearch);

        Ui.onType(search, new Ui.Watch() {
            public void onChanged(String s) {
                Ui.show(clearSearch, s.length() > 0);
                filteredLimit = PAGE_SIZE;
                reload();
            }
        });
        clearSearch.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { search.setText(""); }
        });
        findViewById(R.id.btnMenu).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).openDrawer();
                }
            }
        });
        Ui.tint(clearSearch, Ui.color(c, R.color.ink_faint));
    }

    @Override
    public void refresh() {
        buildCategoryStrip();
        reload();
    }


    private void buildCategoryStrip() {
        catStrip.removeAllViews();
        List<String> cats = Db.get(getContext()).categories();
        boolean any = !cats.isEmpty();
        Ui.show(catScroll, any);
        Ui.show(catLabel, any);
        if (!any) return;

        addCategoryBadge(getContext().getString(R.string.chip_all), "", true);
        for (String c : cats) addCategoryBadge(c, c, false);
    }

    private void addCategoryBadge(String label, final String value, boolean isAll) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.item_category, catStrip, false);
        boolean on = value.equals(category);

        TextView letter = (TextView) v.findViewById(R.id.letter);
        ImageView glyph = (ImageView) v.findViewById(R.id.glyph);
        View circle = v.findViewById(R.id.circle);
        TextView text = (TextView) v.findViewById(R.id.label);

        int accent = isAll ? Ui.color(getContext(), R.color.brand)
                           : Cat.color(getContext(), value);
        int soft = isAll ? Ui.color(getContext(), R.color.brand_soft)
                         : Cat.soft(getContext(), value);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(on ? accent : soft);
        circle.setBackground(bg);

        if (isAll) {
            letter.setVisibility(GONE);
            glyph.setVisibility(VISIBLE);
            Ui.tint(glyph, on ? Ui.color(getContext(), R.color.on_accent) : accent);
        } else {
            glyph.setVisibility(GONE);
            letter.setVisibility(VISIBLE);
            letter.setText(Cat.initial(value));
            letter.setTextColor(on ? Ui.color(getContext(), R.color.on_accent) : accent);
        }

        text.setText(label);
        text.setTextColor(on ? accent : Ui.color(getContext(), R.color.ink_soft));

        v.setOnClickListener(new OnClickListener() {
            public void onClick(View x) {
                category = category.equals(value) ? "" : value;
                filteredLimit = PAGE_SIZE;
                buildCategoryStrip();
                reload();
            }
        });
        catStrip.addView(v);
    }


    private void reload() {
        Db db = Db.get(getContext());
        String q = search.getText().toString();
        sections.removeAllViews();

        boolean filtering = q.trim().length() > 0 || category.length() > 0;
        int filteredCount = filtering ? db.productCount(q, category) : 0;
        List<Product> all = filtering
                ? db.products(q, category, filteredLimit, 0)
                : db.products(q, category);

        if (all.isEmpty()) {
            Ui.show(empty, true);
            emptyText.setText(filtering
                    ? getContext().getString(R.string.empty_search)
                    : getContext().getString(R.string.empty_products));
        } else {
            Ui.show(empty, false);
            if (filtering) {
                String title = category.length() > 0
                        ? category : getContext().getString(R.string.all_products);
                sections.addView(sectionHeader(title, null));
                addGrid(all);
                if (all.size() < filteredCount) addLoadMore(filteredCount - all.size());
            } else {

                Map<String, List<Product>> groups = new LinkedHashMap<String, List<Product>>();
                for (Product p : all) {
                    String key = p.category == null || p.category.trim().length() == 0
                            ? "" : p.category.trim();
                    List<Product> list = groups.get(key);
                    if (list == null) { list = new ArrayList<Product>(); groups.put(key, list); }
                    list.add(p);
                }
                for (Map.Entry<String, List<Product>> e : groups.entrySet()) {
                    final String key = e.getKey();
                    List<Product> list = e.getValue();
                    String title = key.length() > 0
                            ? key : getContext().getString(R.string.uncategorised);
                    boolean more = list.size() > PREVIEW_PER_SECTION;
                    sections.addView(sectionHeader(title, more && key.length() > 0
                            ? new Runnable() {
                                public void run() {
                                    category = key;
                                    filteredLimit = PAGE_SIZE;
                                    buildCategoryStrip();
                                    reload();
                                }
                            } : null));
                    addGrid(more ? list.subList(0, PREVIEW_PER_SECTION) : list);
                }
            }
        }

        subCount.setText(getContext().getString(
                R.string.products_sub, db.productCount(), db.stockCount()));
    }


    private void addLoadMore(int remaining) {
        Button more = new Button(getContext());
        more.setAllCaps(false);
        more.setText(getContext().getString(R.string.load_more_products, remaining));
        more.setTextSize(16);
        more.setTypeface(more.getTypeface(), Typeface.BOLD);
        more.setTextColor(Ui.color(getContext(), R.color.ink));
        more.setBackgroundResource(R.drawable.btn_outline);
        int side = Ui.dp(getContext(), 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(getContext(), 54));
        lp.setMargins(side, Ui.dp(getContext(), 4), side, Ui.dp(getContext(), 18));
        more.setLayoutParams(lp);
        more.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                filteredLimit += PAGE_SIZE;
                reload();
            }
        });
        sections.addView(more);
    }

    private View sectionHeader(String title, final Runnable seeAll) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = Ui.dp(getContext(), 20);
        row.setPadding(pad, Ui.dp(getContext(), 20), pad, Ui.dp(getContext(), 10));

        TextView t = new TextView(getContext());
        t.setText(title);
        t.setTextSize(20);
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setTextColor(Ui.color(getContext(), R.color.ink));
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(t, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (seeAll != null) {
            TextView link = new TextView(getContext());
            link.setText(R.string.see_all);
            link.setTextSize(14);
            link.setTypeface(link.getTypeface(), Typeface.BOLD);
            link.setTextColor(Ui.color(getContext(), R.color.brand));
            link.setPadding(Ui.dp(getContext(), 8), Ui.dp(getContext(), 6),
                    Ui.dp(getContext(), 4), Ui.dp(getContext(), 6));
            link.setOnClickListener(new OnClickListener() {
                public void onClick(View v) { seeAll.run(); }
            });
            row.addView(link);
        }
        return row;
    }


    private void addGrid(List<Product> items) {
        int gap = Ui.dp(getContext(), 12);
        int side = Ui.dp(getContext(), 16);
        for (int i = 0; i < items.size(); i += 2) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.leftMargin = side; rp.rightMargin = side; rp.bottomMargin = gap;
            row.setLayoutParams(rp);

            row.addView(card(items.get(i), row), cell(1f, 0, gap / 2));
            if (i + 1 < items.size()) {
                row.addView(card(items.get(i + 1), row), cell(1f, gap / 2, 0));
            } else {
                View filler = new View(getContext());
                row.addView(filler, cell(1f, gap / 2, 0));
            }
            sections.addView(row);
        }
    }

    private LinearLayout.LayoutParams cell(float weight, int left, int right) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        lp.leftMargin = left;
        lp.rightMargin = right;
        return lp;
    }

    private View card(final Product p, ViewGroup parent) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.item_product_card, parent, false);

        ((TextView) v.findViewById(R.id.name)).setText(p.displayName(getContext()));

        TextView usd = (TextView) v.findViewById(R.id.priceUsd);
        TextView lbp = (TextView) v.findViewById(R.id.priceLbp);
        double price = p.bestPrice();
        if (price > 0) {
            usd.setText(Money.usd(price));
            usd.setTextColor(Ui.color(getContext(), R.color.brand));
            lbp.setVisibility(VISIBLE);
            lbp.setText(Money.lbp(getContext(), price));
        } else {
            usd.setText(R.string.no_price_yet);
            usd.setTextColor(Ui.color(getContext(), R.color.ink_faint));
            lbp.setVisibility(GONE);
        }

        int lowStock = 3;
        try {
            lowStock = Integer.parseInt(Db.get(getContext()).setting("low_stock", "3"));
        } catch (Exception ignored) { }
        TextView qty = (TextView) v.findViewById(R.id.qty);
        qty.setText(getContext().getString(R.string.stock_count_short,p.quantity));
        if (p.quantity <= 0) {
            qty.setBackgroundResource(R.drawable.pill_red);
            qty.setTextColor(Ui.color(getContext(), R.color.red));
        } else if (p.quantity <= lowStock) {
            qty.setBackgroundResource(R.drawable.pill_amber);
            qty.setTextColor(Ui.color(getContext(), R.color.amber));
        } else {
            qty.setBackgroundResource(R.drawable.pill_green);
            qty.setTextColor(Ui.color(getContext(), R.color.green));
        }

        ImageView photo = (ImageView) v.findViewById(R.id.photo);
        View holder = v.findViewById(R.id.photoPlaceholder);
        ImgUtil.loadThumb(photo, holder, p.imagePath, 360);

        v.setOnClickListener(new OnClickListener() {
            public void onClick(View x) {
                Intent i = new Intent(getContext(), ProductDetailActivity.class);
                i.putExtra("id", p.id);
                getContext().startActivity(i);
            }
        });
        v.findViewById(R.id.btnSell).setOnClickListener(new OnClickListener() {
            public void onClick(View x) {
                Intent i = new Intent(getContext(), SellActivity.class);
                i.putExtra("id", p.id);
                getContext().startActivity(i);
            }
        });
        return v;
    }
}
