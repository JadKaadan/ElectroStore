package com.electrostore.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.widget.FrameLayout;


public class AddPage extends Page implements ProductFormView.Listener {

    private final ProductFormView form;

    public AddPage(Context c) {
        super(c);
        LayoutInflater.from(c).inflate(R.layout.page_add, this, true);
        FrameLayout host = (FrameLayout) findViewById(R.id.formHost);
        form = new ProductFormView(c);
        form.setListener(this);
        host.addView(form, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        form.loadNew();
    }

    @Override
    public void refresh() { }

    @Override
    public boolean onResult(int requestCode, int resultCode, Intent data) {
        return form.onPhotoResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaved(Product p, boolean isNew) {
        form.loadNew();
        if (getContext() instanceof MainActivity) {
            ((MainActivity) getContext()).select(MainActivity.TAB_PRODUCTS);
        }
    }

    @Override
    public void onDeleted(Product p) { form.loadNew(); }
}
