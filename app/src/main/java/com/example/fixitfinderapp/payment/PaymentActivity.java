package com.example.fixitfinderapp.payment;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fixitfinderapp.R;

public class PaymentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_payment);

        RadioGroup group = findViewById(R.id.radioGroup);
        Button btnPay = findViewById(R.id.btnPay);

        btnPay.setOnClickListener(v -> {
            int id = group.getCheckedRadioButtonId();
            String method = (id == R.id.radioCash) ? "Cash" : "GCash";
            Toast.makeText(this,
                    "Payment Selected: " + method,
                    Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
