package com.example.classmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText adminNameEdit, classNameEdit, emailEdit, passwordEdit, confirmPasswordEdit;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        adminNameEdit = findViewById(R.id.admin_name_edit_text);
        classNameEdit = findViewById(R.id.class_name_edit_text);
        emailEdit = findViewById(R.id.register_email_edit_text);
        passwordEdit = findViewById(R.id.register_password_edit_text);
        confirmPasswordEdit = findViewById(R.id.confirm_password_edit_text);
        progressBar = findViewById(R.id.register_progress);

        Button registerButton = findViewById(R.id.register_button);
        registerButton.setOnClickListener(v -> handleRegister());

        TextView backToLoginButton = findViewById(R.id.back_to_login_button);
        backToLoginButton.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String adminName = getText(adminNameEdit);
        String className = getText(classNameEdit);
        String email = getText(emailEdit);
        String password = getText(passwordEdit);
        String confirmPassword = getText(confirmPasswordEdit);

        if (adminName.isEmpty() || className.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "נא להזין אימייל תקין", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "הסיסמאות אינן תואמות", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("fullName", adminName);
                        userData.put("email", email);
                        userData.put("className", className);
                        userData.put("isAdmin", true);

                        db.collection("users").document(uid).set(userData)
                                .addOnCompleteListener(dbTask -> {
                                    progressBar.setVisibility(View.GONE);
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(this, "הכיתה נוצרה בהצלחה!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(this, HomeActivity.class);
                                        intent.putExtra("EMAIL", email);
                                        intent.putExtra("IS_ADMIN", true);
                                        intent.putExtra("CLASS_NAME", className);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(this, "שגיאה בשמירת הנתונים", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        String err = task.getException() != null ? task.getException().getMessage() : "שגיאה";
                        Toast.makeText(this, "שגיאה ביצירת החשבון: " + err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getText(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }
}
