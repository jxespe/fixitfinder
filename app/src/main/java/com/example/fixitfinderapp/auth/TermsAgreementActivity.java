package com.example.fixitfinderapp.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.example.fixitfinderapp.BaseSwipeActivity;

import com.example.fixitfinderapp.NavigationHelper;
import com.example.fixitfinderapp.R;

public class TermsAgreementActivity extends BaseSwipeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_agreement);

        ScrollView scrollView = findViewById(R.id.scrollTerms);
        Button btnAccept = findViewById(R.id.btnAccept);
        Button btnDecline = findViewById(R.id.btnDecline);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnAccept.setEnabled(false);

        btnBack.setOnClickListener(v -> finishWithResult(false));
        btnDecline.setOnClickListener(v -> finishWithResult(false));
        btnAccept.setOnClickListener(v -> finishWithResult(true));
        if (scrollView != null) {
            scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                if (isScrolledToBottom(scrollView)) {
                    btnAccept.setEnabled(true);
                }
            });

            scrollView.post(() -> {
                if (isScrolledToBottom(scrollView)) {
                    btnAccept.setEnabled(true);
                }
            });
        }
    }

    private boolean isScrolledToBottom(ScrollView scrollView) {
        if (scrollView.getChildCount() == 0) {
            return false;
        }
        int contentBottom = scrollView.getChildAt(0).getBottom();
        int scrollBottom = scrollView.getHeight() + scrollView.getScrollY();
        return contentBottom <= scrollBottom + 8;
    }

    private void finishWithResult(boolean accepted) {
        setResult(accepted ? RESULT_OK : RESULT_CANCELED);
        finish();
    }
}
