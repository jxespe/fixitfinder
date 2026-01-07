package com.example.fixitfinderapp.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        EditText email = findViewById(R.id.edtEmail);
        EditText password = findViewById(R.id.edtPassword);
        Switch switchProvider = findViewById(R.id.switchProvider);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            auth.createUserWithEmailAndPassword(
                    email.getText().toString(),
                    password.getText().toString()
            ).addOnSuccessListener(result -> {
                String role = switchProvider.isChecked() ? "provider" : "user";

                HashMap<String, Object> data = new HashMap<>();
                data.put("email", email.getText().toString());
                data.put("role", role);

                db.collection("users")
                        .document(result.getUser().getUid())
                        .set(data);

                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
