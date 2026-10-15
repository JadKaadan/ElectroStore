package com.electrostore.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;


public class EditProductActivity extends Base implements ProductFormView.Listener {

    private ProductFormView form;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_edit_product);

        long id = getIntent().getLongExtra("id", 0);
        Product p = Db.get(this).product(id);
        if (p == null) { finish(); return; }

        ((TextView) findViewById(R.id.sub)).setText(
                getString(R.string.product_number, p.displayNumber(this)));
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        form = new ProductFormView(this);
        form.setListener(this);
        ((FrameLayout) findViewById(R.id.formHost)).addView(form,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        form.load(p);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (form != null) form.onPhotoResult(req, res, data);
    }

    @Override public void onSaved(Product p, boolean isNew) { finish(); }

    @Override
    public void onDeleted(Product p) {
        setResult(RESULT_FIRST_USER);
        finish();
    }
}
