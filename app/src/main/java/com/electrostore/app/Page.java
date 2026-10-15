package com.electrostore.app;

import android.content.Context;
import android.content.Intent;
import android.widget.FrameLayout;


public abstract class Page extends FrameLayout {
    public Page(Context c) { super(c); }


    public abstract void refresh();


    public boolean onResult(int requestCode, int resultCode, Intent data) { return false; }
}
