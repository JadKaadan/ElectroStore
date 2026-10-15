package com.electrostore.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.List;


public class ProductFormView extends FrameLayout {

    public static final int REQ_CAMERA = 9101;
    public static final int REQ_GALLERY = 9102;

    public interface Listener {
        void onSaved(Product p, boolean isNew);
        void onDeleted(Product p);
    }

    private EditText fName, fUnit, fCost, fQty, fDesc;
    private AutoCompleteTextView fCategory, fColor;
    private TextView echoUnit, echoCost, profitHint, curUsd, curLbp;
    private ImageView photo, photoRemove;
    private View photoHint;
    private Button btnSave, btnDelete;

    private Product product = new Product();
    private String photoPath = "";
    private File pendingCapture;
    private boolean typingInLbp = false;
    private Listener listener;

    public ProductFormView(Context c) { this(c, null); }

    public ProductFormView(Context c, AttributeSet a) {
        super(c, a);
        LayoutInflater.from(c).inflate(R.layout.view_product_form, this, true);
        bind();
    }

    public void setListener(Listener l) { this.listener = l; }

    private void bind() {
        fName = (EditText) findViewById(R.id.fName);
        fUnit = (EditText) findViewById(R.id.fUnit);
        fCost = (EditText) findViewById(R.id.fCost);
        fQty = (EditText) findViewById(R.id.fQty);
        fDesc = (EditText) findViewById(R.id.fDesc);
        fCategory = (AutoCompleteTextView) findViewById(R.id.fCategory);
        fColor = (AutoCompleteTextView) findViewById(R.id.fColor);
        echoUnit = (TextView) findViewById(R.id.echoUnit);
        echoCost = (TextView) findViewById(R.id.echoCost);
        profitHint = (TextView) findViewById(R.id.profitHint);
        curUsd = (TextView) findViewById(R.id.curUsd);
        curLbp = (TextView) findViewById(R.id.curLbp);
        photo = (ImageView) findViewById(R.id.photo);
        photoRemove = (ImageView) findViewById(R.id.photoRemove);
        photoHint = findViewById(R.id.photoHint);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnDelete = (Button) findViewById(R.id.btnDelete);


        Ui.Watch recompute = new Ui.Watch() {
            public void onChanged(String s) { refreshEchoes(); }
        };
        Ui.onType(fUnit, recompute);
        Ui.onType(fCost, recompute);
        Ui.onType(fQty, recompute);

        findViewById(R.id.btnCamera).setOnClickListener(new OnClickListener() {
            public void onClick(View v) { openCamera(); }
        });
        findViewById(R.id.btnGallery).setOnClickListener(new OnClickListener() {
            public void onClick(View v) { openGallery(); }
        });
        photoRemove.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { setPhoto(""); }
        });

        curUsd.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { switchCurrency(false); }
        });
        curLbp.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { switchCurrency(true); }
        });

        findViewById(R.id.qtyMinus).setOnClickListener(new OnClickListener() {
            public void onClick(View v) { bumpQty(-1); }
        });
        findViewById(R.id.qtyPlus).setOnClickListener(new OnClickListener() {
            public void onClick(View v) { bumpQty(1); }
        });

        findViewById(R.id.btnAddCategory).setOnClickListener(new OnClickListener() {
            public void onClick(View v) { addCategoryDialog(); }
        });

        btnSave.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { save(); }
        });
        btnDelete.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Activity host = activity();
                if (host == null) { askDelete(); return; }
                Lock.require(host, new Runnable() {
                    public void run() { askDelete(); }
                });
            }
        });

        wireKeypad();
        loadSuggestions();
        refreshEchoes();
    }


    private void wireKeypad() {
        Activity a = activity();
        if (a == null) return;
        Keypad.Echo money = new Keypad.Echo() {
            public String of(double typed) { return padEcho(typed); }
        };
        Runnable after = new Runnable() {
            public void run() { refreshEchoes(); }
        };
        Keypad.attach(a, fUnit, getContext().getString(R.string.lbl_unit_price), true, money, after);
        Keypad.attach(a, fCost, getContext().getString(R.string.lbl_cost_price), true, money, after);
        Keypad.attachWhole(a, fQty, getContext().getString(R.string.lbl_quantity), after);
    }


    private String padEcho(double typed) {
        if (typed <= 0) return "";
        double usd = typingInLbp ? typed / Money.rate(getContext()) : typed;
        return typingInLbp ? "= " + Money.usd(usd) : "= " + Money.lbp(getContext(), usd);
    }

    private void loadSuggestions() {
        List<String> cats = Db.get(getContext()).categories();
        fCategory.setAdapter(new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_dropdown_item_1line, cats));
        String[] colors = getResources().getStringArray(R.array.color_suggestions);
        fColor.setAdapter(new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_dropdown_item_1line, Arrays.asList(colors)));
    }

    private void addCategoryDialog() {
        final EditText input = new EditText(getContext());
        input.setSingleLine(true);
        input.setHint(R.string.hint_new_category);
        int pad = Ui.dp(getContext(), 20);
        FrameLayout box = new FrameLayout(getContext());
        box.setPadding(pad, 0, pad, 0);
        box.addView(input, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.add_category_title)
                .setView(box)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        String name = input.getText().toString().trim();
                        if (name.length() == 0) return;
                        Db.get(getContext()).addCategory(name);
                        loadSuggestions();
                        fCategory.setText(name);
                        fCategory.setSelection(name.length());
                        Ui.toast(getContext(), getContext().getString(R.string.category_added, name));
                    }
                }).show();
    }


    private void switchCurrency(boolean toLbp) {
        if (toLbp == typingInLbp) return;
        double rate = Money.rate(getContext());

        double u = Money.parse(fUnit.getText());
        double r = Money.parse(fCost.getText());
        if (toLbp) {
            if (u > 0) fUnit.setText(Money.plain(Math.round(u * rate)));
            if (r > 0) fCost.setText(Money.plain(Math.round(r * rate)));
        } else {
            if (u > 0) fUnit.setText(Money.plain(round2(u / rate)));
            if (r > 0) fCost.setText(Money.plain(round2(r / rate)));
        }
        typingInLbp = toLbp;
        paintCurrencyChips();
        refreshEchoes();
    }

    private void paintCurrencyChips() {
        TextView on = typingInLbp ? curLbp : curUsd;
        TextView off = typingInLbp ? curUsd : curLbp;
        on.setBackgroundResource(R.drawable.chip_on);
        on.setTextColor(Ui.color(getContext(), R.color.on_accent));
        off.setBackgroundResource(R.drawable.chip);
        off.setTextColor(Ui.color(getContext(), R.color.ink_soft));
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }


    private double asUsd(EditText box) {
        double v = Money.parse(box.getText());
        if (v == 0) return 0;
        return typingInLbp ? v / Money.rate(getContext()) : v;
    }

    private void refreshEchoes() {
        double unit = asUsd(fUnit), cost = asUsd(fCost);
        echo(echoUnit, unit);


        if (cost <= 0) {
            echoCost.setText("");
        } else {
            StringBuilder sb = new StringBuilder(
                    typingInLbp ? "= " + Money.usd(cost) : "= " + Money.lbp(getContext(), cost));
            int qty = Money.parseInt(fQty.getText());
            if (qty > 1) {
                sb.append("   ·   ").append(getContext().getString(
                        R.string.wholesale_total, Money.usd(cost * qty), String.valueOf(qty)));
            }
            echoCost.setText(sb.toString());
        }

        if (unit > 0 && cost > 0) {
            double profit = unit - cost;
            String pct = String.format(java.util.Locale.US, "  (%.0f%%)", profit / cost * 100.0);
            profitHint.setVisibility(VISIBLE);
            if (profit >= 0) {
                profitHint.setBackgroundResource(R.drawable.pill_green);
                profitHint.setTextColor(Ui.color(getContext(), R.color.green));
                profitHint.setText(getContext().getString(
                        R.string.earn_per_unit, Money.usd(profit), pct));
            } else {
                profitHint.setBackgroundResource(R.drawable.pill_red);
                profitHint.setTextColor(Ui.color(getContext(), R.color.red));
                profitHint.setText(getContext().getString(
                        R.string.lose_per_unit, Money.usd(-profit)));
            }
        } else {
            profitHint.setVisibility(GONE);
        }
    }

    private void echo(TextView tv, double usd) {
        if (usd <= 0) { tv.setText(""); return; }
        tv.setText(typingInLbp ? "= " + Money.usd(usd) : "= " + Money.lbp(getContext(), usd));
    }

    private void bumpQty(int delta) {
        int q = Money.parseInt(fQty.getText()) + delta;
        if (q < 0) q = 0;
        fQty.setText(String.valueOf(q));
    }


    private Activity activity() {
        Context c = getContext();
        while (c instanceof android.content.ContextWrapper) {
            if (c instanceof Activity) return (Activity) c;
            c = ((android.content.ContextWrapper) c).getBaseContext();
        }
        return null;
    }

    private void openCamera() {
        Activity a = activity();
        if (a == null) return;
        try {
            pendingCapture = ImgUtil.newPhotoFile(getContext());
            Uri out = PhotoProvider.uriFor(pendingCapture);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, out);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                a.startActivityForResult(i, REQ_CAMERA);
            } catch (android.content.ActivityNotFoundException nf) {
                Ui.toast(getContext(), getContext().getString(R.string.no_camera));
            }
        } catch (Throwable t) {
            Ui.toast(getContext(), getContext().getString(R.string.camera_failed));
        }
    }

    private void openGallery() {
        Activity a = activity();
        if (a == null) return;
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            a.startActivityForResult(Intent.createChooser(
                    i, getContext().getString(R.string.from_gallery)), REQ_GALLERY);
        } catch (Throwable t) {
            Ui.toast(getContext(), getContext().getString(R.string.gallery_failed));
        }
    }


    public boolean onPhotoResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return false;
        if (requestCode == REQ_CAMERA) {
            if (pendingCapture != null && pendingCapture.exists() && pendingCapture.length() > 0) {
                String small = ImgUtil.shrinkInto(getContext(), pendingCapture.getAbsolutePath());
                ImgUtil.delete(pendingCapture.getAbsolutePath());
                if (small.length() > 0) setPhoto(small);
            } else if (data != null && data.getExtras() != null
                    && data.getExtras().get("data") instanceof Bitmap) {
                setPhoto(savePreview((Bitmap) data.getExtras().get("data")));
            }
            return true;
        }
        if (requestCode == REQ_GALLERY && data != null && data.getData() != null) {
            String p = ImgUtil.importFrom(getContext(), data.getData());
            if (p.length() > 0) setPhoto(p);
            else Ui.toast(getContext(), getContext().getString(R.string.picture_failed));
            return true;
        }
        return false;
    }

    private String savePreview(Bitmap bm) {
        try {
            File f = ImgUtil.newPhotoFile(getContext());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return f.getAbsolutePath();
        } catch (Throwable t) { return ""; }
    }

    private void setPhoto(String path) {
        photoPath = path == null ? "" : path;
        if (photoPath.length() == 0) {
            photo.setVisibility(GONE);
            photoHint.setVisibility(VISIBLE);
            photoRemove.setVisibility(GONE);
            photo.setImageDrawable(null);
        } else {
            Bitmap bm = ImgUtil.thumb(photoPath, 900);
            if (bm == null) { setPhoto(""); return; }
            photo.setImageBitmap(bm);
            photo.setVisibility(VISIBLE);
            photoHint.setVisibility(GONE);
            photoRemove.setVisibility(VISIBLE);
        }
    }


    public void loadNew() {
        product = new Product();
        fName.setText(""); fCategory.setText(""); fColor.setText(""); fDesc.setText("");
        fUnit.setText(""); fCost.setText(""); fQty.setText("");
        typingInLbp = false;
        paintCurrencyChips();
        setPhoto("");
        btnSave.setText(R.string.save_product);
        btnDelete.setVisibility(GONE);
        loadSuggestions();
        refreshEchoes();
        fullScrollTop();
    }

    public void load(Product p) {
        product = p == null ? new Product() : p;
        typingInLbp = false;
        paintCurrencyChips();
        fName.setText(product.name);
        fCategory.setText(product.category);
        fColor.setText(product.color);
        fDesc.setText(product.description);
        fUnit.setText(Money.plain(product.unitPrice));
        fCost.setText(Money.plain(product.costPrice));
        fQty.setText(product.quantity == 0 ? "" : String.valueOf(product.quantity));
        setPhoto(product.imagePath);
        btnSave.setText(product.id > 0 ? R.string.save_changes : R.string.save_product);
        btnDelete.setVisibility(product.id > 0 ? VISIBLE : GONE);
        loadSuggestions();
        refreshEchoes();
    }

    private void fullScrollTop() {
        View v = getChildAt(0);
        if (v instanceof android.widget.ScrollView) ((android.widget.ScrollView) v).scrollTo(0, 0);
    }

    private void save() {
        boolean isNew = product.id == 0;
        product.name = fName.getText().toString();
        product.category = fCategory.getText().toString();
        product.color = fColor.getText().toString();
        product.description = fDesc.getText().toString();
        product.unitPrice = asUsd(fUnit);
        product.costPrice = asUsd(fCost);
        product.quantity = Money.parseInt(fQty.getText());
        product.imagePath = photoPath;

        Db.get(getContext()).saveProduct(product);
        Activity a = activity();
        if (a != null) Ui.hideKeyboard(a);
        Ui.toast(getContext(), getContext().getString(
                isNew ? R.string.saved_as : R.string.updated_product, product.displayNumber(getContext())));
        if (listener != null) listener.onSaved(product, isNew);
    }

    private void askDelete() {
        if (product.id == 0) return;
        Ui.confirm(getContext(),
                getContext().getString(R.string.confirm_delete_title),
                getContext().getString(R.string.confirm_delete_msg,
                        product.displayName(getContext())),
                getContext().getString(R.string.delete), new Runnable() {
                    public void run() {
                        ImgUtil.delete(product.imagePath);
                        Db.get(getContext()).deleteProduct(product.id);
                        Ui.toast(getContext(), getContext().getString(R.string.deleted));
                        if (listener != null) listener.onDeleted(product);
                    }
                });
    }
}
