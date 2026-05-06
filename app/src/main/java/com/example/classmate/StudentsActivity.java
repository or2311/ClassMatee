package com.example.classmate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך ניהול תלמידים (StudentsActivity).
 * מסך זה מאפשר למנהלים (מורים) לצפות ברשימת כל התלמידים בכיתה שלהם.
 * המנהל יכול לערוך שמות תלמידים או להוציא אותם מהכיתה.
 * תלמידים יכולים רק לראות מי נמצא איתם בכיתה.
 */
public class StudentsActivity extends AppCompatActivity {

    private StudentsAdapter adapter; // המתאם לניהול הצגת רשימת התלמידים
    private final List<Student> studentList = new ArrayList<>(); // רשימת התלמידים שנטענו מהענן
    private FirebaseFirestore db; // חיבור לבסיס הנתונים (Firestore)
    private boolean isAdmin = false; // האם המשתמש הצופה הוא מנהל הכיתה
    private String className = ""; // שם הכיתה שאת תלמידיה מציגים
    private TextView emptyText; // הודעה שמוצגת אם אין תלמידים בכיתה
    private TextView studentCountText; // טקסט המציג את סך כל התלמידים בכיתה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);

        // קבלת נתוני המשתמש והכיתה שנשלחו מהמסך הקודם
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        className = getIntent().getStringExtra("CLASS_NAME");

        // הגדרת סרגל כלים עליון עם כפתור חזור
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("תלמידי הכיתה");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish()); // סגירת המסך בלחיצה על החץ

        // קישור רכיבי הממשק
        emptyText = findViewById(R.id.empty_students_text);
        studentCountText = findViewById(R.id.student_count_text);
        db = FirebaseFirestore.getInstance();

        // הגדרת הרשימה הנגוללת (RecyclerView)
        RecyclerView recyclerView = findViewById(R.id.students_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // אתחול המתאם עם הגדרת פעולות של עריכה ומחיקה (שיהיו פעילות רק למנהל)
        adapter = new StudentsAdapter(studentList, isAdmin, new StudentsAdapter.OnStudentActionListener() {
            @Override
            public void onEditStudent(Student student) {
                showEditStudentDialog(student); // פתיחת חלונית עריכת שם
            }

            @Override
            public void onDeleteStudent(Student student) {
                showDeleteConfirmation(student); // פתיחת חלונית אישור הוצאה מהכיתה
            }
        });
        recyclerView.setAdapter(adapter);

        loadStudents(); // טעינת נתוני התלמידים מהענן
    }

    /**
     * שואבת את כל המשתמשים ששייכים לכיתה הנוכחית (שאינם מנהלים).
     */
    private void loadStudents() {
        if (className == null) return;

        db.collection("users")
                .whereEqualTo("className", className)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        studentList.clear(); // ניקוי הרשימה לפני טעינה מחדש
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                Boolean isAdminDoc = doc.getBoolean("isAdmin");
                                // אנחנו רוצים להציג רק תלמידים, לא את המנהלים של הכיתה
                                if (isAdminDoc != null && isAdminDoc) continue;

                                studentList.add(new Student(
                                        doc.getString("fullName"),
                                        doc.getString("email"),
                                        doc.getId(),
                                        doc.getString("className"),
                                        false
                                ));
                            }
                        }
                        adapter.updateStudents(studentList); // עדכון הרשימה על המסך
                        updateUI(); // עדכון מונה התלמידים
                    } else {
                        Toast.makeText(this, "שגיאה בטעינת התלמידים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * מעדכנת את מונה התלמידים בראש המסך ואת הודעת "אין תלמידים".
     */
    private void updateUI() {
        int count = studentList.size();
        String countText = "מספר תלמידים בכיתה: " + count;
        studentCountText.setText(countText);
        
        if (emptyText != null) {
            emptyText.setVisibility(studentList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * פותחת חלונית (Dialog) המאפשרת למנהל לשנות את שמו של התלמיד.
     */
    private void showEditStudentDialog(Student student) {
        // טעינת העיצוב של החלונית מקובץ XML
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.student_fullname_input);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.student_email_layout);
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.student_password_layout);
        
        // בעריכה אנחנו מסתירים שדות שלא רלוונטיים (מייל וסיסמה)
        if (emailLayout != null) emailLayout.setVisibility(View.GONE);
        if (passwordLayout != null) passwordLayout.setVisibility(View.GONE);
        
        if (title != null) title.setText("עריכת פרטי תלמיד");
        if (nameInput != null) {
            nameInput.setText(student.getFullName()); // מציג את השם הנוכחי
        }

        // בנייה והצגת החלונית
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("שמור", (dialog, which) -> {
                    if (nameInput != null && nameInput.getText() != null) {
                        String newName = nameInput.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            updateStudentName(student, newName); // שמירה בענן
                        }
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * מעדכנת את שם התלמיד בבסיס הנתונים.
     */
    private void updateStudentName(Student student, String newName) {
        db.collection("users").document(student.getUserId())
                .update("fullName", newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "השם עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                    loadStudents(); // רענון הרשימה
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בעדכון השם", Toast.LENGTH_SHORT).show());
    }

    /**
     * מציגה חלונית אישור לפני הוצאת תלמיד מהכיתה.
     */
    private void showDeleteConfirmation(Student student) {
        if (!isAdmin) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("הוצאת תלמיד מהכיתה")
                .setMessage("האם להוציא את " + student.getFullName() + " מהכיתה?\nהתלמיד לא יוכל להיכנס למערכת עד שישובץ לכיתה.")
                .setPositiveButton("הוצא מהכיתה", (dialog, which) -> removeStudentFromClass(student))
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * "מוחקת" את התלמיד מהכיתה על ידי ניתוק שם הכיתה מהפרופיל שלו.
     */
    private void removeStudentFromClass(Student student) {
        String uid = student.getUserId();
        
        db.collection("users").document(uid)
                .update("className", null) // הסרת שם הכיתה מהמשתמש
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(StudentsActivity.this,
                            "התלמיד " + student.getFullName() + " הוצא מהכיתה",
                            Toast.LENGTH_SHORT).show();
                    loadStudents(); // רענון הרשימה
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
