package com.example.classmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * מסך ההתחברות (Login).
 * זהו המסך הראשון שהמשתמש פוגש. הוא מאפשר למשתמשים קיימים להיכנס למערכת
 * ומפנה משתמשים חדשים להרשמה או להגדרת פרופיל ראשונית.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private RadioGroup roleRadioGroup;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            ThemeHelper.applyTheme(this, currentUser.getUid());
        }
        
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
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "נא להזין אימייל תקין", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = roleRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "נא לבחור סוג התחברות (תלמיד / מנהל)", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean wantsToBeAdmin = (selectedId == R.id.radio_admin);

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkUserInFirestore(email, wantsToBeAdmin);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "שגיאה לא ידועה";
                        Toast.makeText(LoginActivity.this, "התחברות נכשלה: " + errMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserInFirestore(String email, boolean wantsToBeAdmin) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        // תיקון: הצגת שגיאה מפורטת במידה והשליפה נכשלת
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            processUserDocument(document, email, wantsToBeAdmin);
                        } else {
                            // אם המשתמש לא קיים ב-Firestore, ננתק אותו כדי שלא יישאר במצב מוזר
                            mAuth.signOut();
                            Toast.makeText(LoginActivity.this, "משתמש לא נמצא במערכת הנתונים. נא להירשם מחדש.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // הצגת השגיאה האמיתית ב-Log וב-Toast לדיבג
                        String error = task.getException() != null ? task.getException().getMessage() : "שגיאה לא ידועה";
                        Log.e("LOGIN_ERROR", "Firestore error: " + error);
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this, "שגיאה בשליפת נתונים: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void processUserDocument(DocumentSnapshot document, String email, boolean wantsToBeAdmin) {
        Boolean isAdminInDb = document.getBoolean("isAdmin");
        String className = document.getString("className");
        String fullName = document.getString("fullName");

        if (isAdminInDb == null) isAdminInDb = false;

        if (!isAdminInDb.equals(wantsToBeAdmin)) {
            String msg = isAdminInDb
                    ? "חשבון זה שייך למנהל. נא לבחור התחברות כמנהל."
                    : "חשבון זה שייך לתלמיד. נא לבחור התחברות כתלמיד.";
            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            mAuth.signOut();
            return;
        }

        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra("EMAIL", email);
        intent.putExtra("FULL_NAME", fullName != null ? fullName : email);
        intent.putExtra("IS_ADMIN", isAdminInDb);
        intent.putExtra("CLASS_NAME", className);
        startActivity(intent);
        finish();
    }
}
