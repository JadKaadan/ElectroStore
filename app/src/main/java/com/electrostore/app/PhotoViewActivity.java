package com.electrostore.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;


public class PhotoViewActivity extends Base {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_photo_view);
        String path = getIntent().getStringExtra("path");
        Bitmap bm = ImgUtil.thumb(path, 1600);
        if (bm == null) { finish(); return; }
        ((ImageView) findViewById(R.id.photo)).setImageBitmap(bm);
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        getWindow().setStatusBarColor(0xFF000000);
    }
}
