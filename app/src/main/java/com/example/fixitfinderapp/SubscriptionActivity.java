package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.fixitfinderapp.payment.PaymentActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class SubscriptionActivity extends BaseSwipeActivity {

    private MaterialCardView cardMonthly;
    private MaterialCardView cardAnnual;
    private android.widget.ImageView ivMonthly;
    private android.widget.ImageView ivAnnual;
    private String selectedPlan = PaymentActivity.PLAN_MONTHLY;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        density = getResources().getDisplayMetrics().density;

        TextView tvBack = findViewById(R.id.tvBack);
        MaterialButton btnSubscribe = findViewById(R.id.btnSubscribe);
        cardMonthly = findViewById(R.id.cardPlanMonthly);
        cardAnnual = findViewById(R.id.cardPlanAnnual);
        ivMonthly = findViewById(R.id.ivPlanMonthly);
        ivAnnual = findViewById(R.id.ivPlanAnnual);

        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }

        if (cardMonthly != null) {
            cardMonthly.setOnClickListener(v -> {
                selectedPlan = PaymentActivity.PLAN_MONTHLY;
                updatePlanSelectionUi();
            });
        }
        if (cardAnnual != null) {
            cardAnnual.setOnClickListener(v -> {
                selectedPlan = PaymentActivity.PLAN_ANNUAL;
                updatePlanSelectionUi();
            });
        }

        updatePlanSelectionUi();

        if (btnSubscribe != null) {
            btnSubscribe.setOnClickListener(v -> openPaymentForPlan());
        }
    }

    private void updatePlanSelectionUi() {
        boolean monthly = PaymentActivity.PLAN_MONTHLY.equals(selectedPlan);
        if (ivMonthly != null) {
            ivMonthly.setImageResource(monthly
                    ? R.drawable.ic_subscription_plan_selected
                    : R.drawable.ic_subscription_plan_unselected);
        }
        if (ivAnnual != null) {
            ivAnnual.setImageResource(!monthly
                    ? R.drawable.ic_subscription_plan_selected
                    : R.drawable.ic_subscription_plan_unselected);
        }
        int strokeSelected = (int) (2f * density + 0.5f);
        int strokeNormal = (int) (1f * density + 0.5f);
        if (cardMonthly != null) {
            cardMonthly.setStrokeWidth(monthly ? strokeSelected : strokeNormal);
        }
        if (cardAnnual != null) {
            cardAnnual.setStrokeWidth(!monthly ? strokeSelected : strokeNormal);
        }
    }

    private void openPaymentForPlan() {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_SUBSCRIPTION_FLOW, true);
        intent.putExtra(PaymentActivity.EXTRA_SUBSCRIPTION_PLAN, selectedPlan);
        startActivity(intent);
    }
}
