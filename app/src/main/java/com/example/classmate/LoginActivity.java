package com.example.classmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private RadioGroup roleRadioGroup;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        roleRadioGroup = findViewById(R.id.role_radio_group);
        progressBar = findViewById(R.id.login_progress);

        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> handleLogin());

        TextView createAccountButton = findViewById(R.id.create_account_button);
        createAccountButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        int selectedId = roleRadioGroup.getCheckedRadioButtonId();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "נא להזין אימייל תקין", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // אם הצלחנו להתחבר, נבדוק את התפקיד ב-Firestore
                        checkUserInFirestore(email, selectedId == R.id.radio_admin);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "התחברות נכשלה: " + (task.getException() != null ? task.getException().getMessage() : "שגיאה לא ידועה"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserInFirestore(String email, boolean wantsToBeAdmin) {
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Boolean isAdminInDb = document.getBoolean("isAdmin");
                            String className = document.getString("className");
                            
                            if (isAdminInDb == null) isAdminInDb = false;

                            // בדיקה אם המשתמש מנסה להיכנס בתפקיד שלא שייך לו
                            if (isAdminInDb != wantsToBeAdmin) {
                                String msg = isAdminInDb ? "חשבון זה שייך למנהל. נא לבחור התחברות כמנהל." : "חשבון זה שייך לתלמיד. נא לבחור התחברות כתלמיד.";
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                                return;
                            }

                            // הכל תקין - מעבר למסך הבית
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.putExtra("EMAIL", email);
                            intent.putExtra("IS_ADMIN", isAdminInDb);
                            intent.putExtra("CLASS_NAME", className);
                            startActivity(intent);
                            finish();
                        } else {
                            mAuth.signOut();
                            Toast.makeText(LoginActivity.this, "לא נמצאו נתוני משתמש ב-Database", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this, "שגיאה בשליפת נתונים: " + (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
