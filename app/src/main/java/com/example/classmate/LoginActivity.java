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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
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

        // בדיקה שנבחר תפקיד
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

        // קודם כל מנסים לחפש לפי UID (הדרך המועדפת)
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // נמצא לפי UID
                            processUserDocument(document, email, wantsToBeAdmin);
                        } else {
                            // *** תיקון הבאג העיקרי ***
                            // המסמך לא נמצא לפי UID – מנסים לחפש לפי אימייל
                            // (קורה כשהמסמך נוצר עם ID שאינו ה-UID)
                            searchUserByEmail(email, wantsToBeAdmin);
                        }
                    } else {
                        mAuth.signOut();
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "";
                        Toast.makeText(LoginActivity.this, "שגיאה בשליפת נתונים: " + errMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // חיפוש גיבוי לפי שדה האימייל
    private void searchUserByEmail(String email, boolean wantsToBeAdmin) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        processUserDocument(document, email, wantsToBeAdmin);
                    } else {
                        // לא נמצאה רשומה – בדיקה אם החשבון ממתין למחיקה
                        checkPendingDeletion(email, wantsToBeAdmin);
                    }
                });
    }

    // בדיקה אם המשתמש נמחק על ידי המנהל, אחרת הצגת הגדרת פרופיל
    private void checkPendingDeletion(String email, boolean wantsToBeAdmin) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("pending_deletions").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        // החשבון מסומן למחיקה – מוחקים את חשבון ה-Auth
                        db.collection("pending_deletions").document(uid).delete();
                        com.google.firebase.auth.FirebaseUser userToDelete = mAuth.getCurrentUser();
                        if (userToDelete != null) {
                            userToDelete.delete().addOnCompleteListener(deleteTask ->
                                    Toast.makeText(LoginActivity.this,
                                            "החשבון שלך הוסר מהמערכת על ידי המנהל",
                                            Toast.LENGTH_LONG).show());
                        }
                    } else {
                        // לא נמצא מסמך ב-Firestore – הצגת הגדרת פרופיל ראשונית
                        showFirstTimeSetupDialog(uid, email, wantsToBeAdmin);
                    }
                });
    }

    // דיאלוג להגדרת פרופיל ראשונית כשהמשתמש קיים ב-Auth אך לא ב-Firestore
    private void showFirstTimeSetupDialog(String uid, String email, boolean wantsToBeAdmin) {
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        android.view.View dialogView = inflater.inflate(R.layout.dialog_first_setup, null);

        com.google.android.material.textfield.TextInputEditText fullNameInput =
                dialogView.findViewById(R.id.setup_fullname_input);
        com.google.android.material.textfield.TextInputEditText classNameInput =
                dialogView.findViewById(R.id.setup_classname_input);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("הגדרת פרופיל")
                .setMessage("החשבון שלך אומת, אך לא נמצאו פרטים ב-Database.
מלא את הפרטים:")
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, "שמור", (d, w) -> {});
        dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE, "ביטול", (d, w) -> {
            mAuth.signOut();
        });

        dialog.setOnShowListener(dlg -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String fullName = fullNameInput.getText() != null ? fullNameInput.getText().toString().trim() : "";
                String className = classNameInput.getText() != null ? classNameInput.getText().toString().trim() : "";

                if (fullName.isEmpty() || className.isEmpty()) {
                    Toast.makeText(this, "נא למלא שם ושם כיתה", Toast.LENGTH_SHORT).show();
                    return;
                }

                java.util.Map<String, Object> userData = new java.util.HashMap<>();
                userData.put("fullName", fullName);
                userData.put("email", email);
                userData.put("className", className);
                userData.put("isAdmin", wantsToBeAdmin);

                db.collection("users").document(uid).set(userData)
                        .addOnSuccessListener(aVoid -> {
                            dialog.dismiss();
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.putExtra("EMAIL", email);
                            intent.putExtra("IS_ADMIN", wantsToBeAdmin);
                            intent.putExtra("CLASS_NAME", className);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        });

        dialog.show();
    }

    private void processUserDocument(DocumentSnapshot document, String email, boolean wantsToBeAdmin) {
        Boolean isAdminInDb = document.getBoolean("isAdmin");
        String className = document.getString("className");

        if (isAdminInDb == null) isAdminInDb = false;

        // בדיקה אם התפקיד הנבחר תואם לתפקיד בבסיס הנתונים
        if (!isAdminInDb.equals(wantsToBeAdmin)) {
            String msg = isAdminInDb
                    ? "חשבון זה שייך למנהל. נא לבחור התחברות כמנהל."
                    : "חשבון זה שייך לתלמיד. נא לבחור התחברות כתלמיד.";
            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            mAuth.signOut();
            return;
        }

        // הכל תקין – מעבר למסך הבית
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra("EMAIL", email);
        intent.putExtra("IS_ADMIN", isAdminInDb);
        intent.putExtra("CLASS_NAME", className);
        startActivity(intent);
        finish();
    }
}
