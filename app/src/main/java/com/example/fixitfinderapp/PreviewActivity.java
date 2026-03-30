package com.example.fixitfinderapp;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.LayoutRes;

public class PreviewActivity extends BaseSwipeActivity {

    public static final String EXTRA_LAYOUT_RES_ID = "layoutResId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        @LayoutRes int layoutResId = getIntent().getIntExtra(EXTRA_LAYOUT_RES_ID, 0);
        if (layoutResId == 0) {
            Toast.makeText(this, "Missing layout for preview", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(layoutResId);
    }
}
