package com.example.classmate;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך צפייה בציונים (ViewGradesActivity).
 * מסך זה מאפשר לתלמידים לראות את הציונים האישיים שלהם, 
 * ולמנהלים (מורים) לראות ציונים של כל תלמיד בכיתה על ידי בחירה מתוך רשימה.
 */
public class ViewGradesActivity extends AppCompatActivity {

    // --- רכיבי ממשק המשתמש (UI) ---
    private RecyclerView recyclerView;         // רשימה נגוללת להצגת הציונים
    private GradesAdapter adapter;             // המתאם שמחבר את נתוני הציונים לרשימה
    private final List<Grade> gradeList = new ArrayList<>(); // רשימה המכילה את אובייקטי הציונים
    private final List<Student> studentList = new ArrayList<>(); // רשימת התלמידים (עבור תצוגת מנהל)
    private FirebaseFirestore db;              // חיבור לבסיס הנתונים בענן
    private TextView emptyText;                // טקסט שמוצג כשאין ציונים להצגה
    private AutoCompleteTextView studentSpinner; // תיבת בחירה לבחירת תלמיד (למורים)
    private MaterialCardView selectorCard;     // כרטיס המכיל את תיבת בחירת התלמיד
    
    // --- משתני לוגיקה ---
    private boolean isAdminMode = false;       // האם המסך פתוח במצב מנהל
    private String className = "";             // שם הכיתה
    private String currentUserId = "";         // המזהה של המשתמש המחובר כרגע

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_grades);

        db = FirebaseFirestore.getInstance();
        // קבלת הגדרות מהמסך הקודם: האם זה מורה ומה שם הכיתה
        isAdminMode = getIntent().getBooleanExtra("IS_ADMIN_VIEW", false);
        className = getIntent().getStringExtra("CLASS_NAME");
        
        // קבלת ה-ID של המשתמש המחובר כדי לדעת איזה ציונים לשלוף
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // הגדרת סרגל עליון עם כפתור חזור
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isAdminMode ? "ציוני כיתה" : "הציונים שלי");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // קישור רכיבי הממשק לקוד
        recyclerView = findViewById(R.id.grades_recycler_view);
        emptyText = findViewById(R.id.empty_grades_text);
        selectorCard = findViewById(R.id.student_selector_card);
        studentSpinner = findViewById(R.id.view_student_auto_complete);

        // הגדרת רשימת הציונים
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GradesAdapter(gradeList);
        recyclerView.setAdapter(adapter);

        // בדיקה: האם המשתמש הוא מורה?
        if (isAdminMode) {
            selectorCard.setVisibility(View.VISIBLE); // הצגת תיבת בחירת תלמיד
            loadStudents();      // טעינת רשימת התלמידים לבחירה
            setupStudentSelector(); // הגדרת מה קורה כשבוחרים תלמיד
        } else if (!currentUserId.isEmpty()) {
            // אם זה תלמיד, נטען מיד את הציונים שלו
            loadGrades(currentUserId);
        }
    }

    /**
     * מגדיר את הפעולה שתתבצע כאשר המורה בוחר תלמיד מהרשימה.
     */
    private void setupStudentSelector() {
        studentSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String studentName = (String) parent.getItemAtPosition(position);
            // מציאת התלמיד הנבחר ברשימה כדי לקבל את ה-ID שלו
            for (Student s : studentList) {
                if (s.getFullName().equals(studentName)) {
                    loadGrades(s.getUserId()); // טעינת הציונים של התלמיד הנבחר
                    break;
                }
            }
        });
    }

    /**
     * טוענת את רשימת התלמידים השייכים לכיתה מתוך בסיס הנתונים.
     */
    private void loadStudents() {
        if (className == null || className.isEmpty()) return;
        
        db.collection("users")
                .whereEqualTo("className", className)
                .whereEqualTo("isAdmin", false) // טוענים רק תלמידים
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        studentList.clear();
                        List<String> names = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Student s = new Student(
                                    doc.getString("fullName"),
                                    doc.getString("email"),
                                    doc.getId(),
                                    doc.getString("className"),
                                    false
                            );
                            studentList.add(s);
                            names.add(s.getFullName()); // הוספת השם לרשימת התצוגה בתיבת הבחירה
                        }
                        // עדכון תיבת הבחירה בשמות התלמידים
                        ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
                        studentSpinner.setAdapter(studentAdapter);
                    }
                });
    }

    /**
     * טוענת את כל הציונים של תלמיד ספציפי מתוך בסיס הנתונים בענן.
     * @param studentId המזהה הייחודי של התלמיד שאת ציוניו רוצים לראות.
     */
    private void loadGrades(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;

        db.collection("grades")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        gradeList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            // המרת הנתונים שהתקבלו מהענן לאובייקט מסוג Grade
                            Grade g = doc.toObject(Grade.class);
                            g.setId(doc.getId());
                            gradeList.add(g);
                        }
                        // מיון הציונים מהחדש ביותר לישן ביותר לפי זמן ההזנה
                        gradeList.sort((g1, g2) -> Long.compare(g2.getTimestamp(), g1.getTimestamp()));
                        
                        // עדכון הרשימה שמוצגת על המסך
                        adapter.updateGrades(gradeList);
                        // הצגת הודעה אם לא נמצאו ציונים לתלמיד זה
                        emptyText.setVisibility(gradeList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Log.e("ViewGrades", "Error getting grades", task.getException());
                        Toast.makeText(this, "שגיאה בטעינת ציונים", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
