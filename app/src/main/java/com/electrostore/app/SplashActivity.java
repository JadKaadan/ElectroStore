package com.electrostore.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


public class SplashActivity extends Base {

    private static final long SHOW_FOR_MS = 1300;
    private boolean timeReady, leaving;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_splash);

        int cream = Ui.color(this, R.color.splash_bg);
        getWindow().setStatusBarColor(cream);
        getWindow().setNavigationBarColor(cream);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View root = getWindow().getDecorView();
            root.setSystemUiVisibility(
                    root.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }


        final ImageView logo = (ImageView) findViewById(R.id.logo);
        int width = getResources().getDisplayMetrics().widthPixels;
        ViewGroup.LayoutParams lp = logo.getLayoutParams();
        lp.width = Math.round(width * 0.68f);
        logo.setLayoutParams(lp);

        logo.setAlpha(0f);
        logo.animate().alpha(1f).setDuration(420).start();

        new Handler().postDelayed(new Runnable() {
            public void run() { timeReady = true; openAppWhenReady(); }
        }, SHOW_FOR_MS);
    }

    private void openAppWhenReady() {
        if (!timeReady || leaving || isFinishing()) return;
        leaving = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }


    @Override
    public void onBackPressed() { finish(); }
}
