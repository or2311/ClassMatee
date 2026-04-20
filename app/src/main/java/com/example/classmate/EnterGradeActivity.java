package com.example.classmate;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * מסך זה מיועד למורים/מנהלים בלבד.
 * הוא מאפשר להזין ציון לתלמיד מסוים במקצוע נבחר, לשמור אותו בענן,
 * ולבחור האם לשלוח לתלמיד התראה או מייל אוטומטי.
 */
public class EnterGradeActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש (תיבות בחירה, תיבות טקסט וכו')
    private AutoCompleteTextView studentSpinner, subjectSpinner;
    private TextInputLayout newSubjectLayout;
    private TextInputEditText newSubjectEdit, scoreEdit;
    private SwitchMaterial sendEmailSwitch;
    private ProgressBar progressBar;
    
    private String className = ""; // שם הכיתה עבורה מזינים את הציון
    private final List<Student> studentList = new ArrayList<>(); // רשימת התלמידים בכיתה
    private final List<String> subjects = new ArrayList<>();    // רשימת המקצועות הקיימים
    private final String ADD_NEW_SUBJECT = "הוסף מקצוע חדש..."; // אפשרות להוספת מקצוע שלא ברשימה
    private FirebaseFirestore db; // חיבור לבסיס הנתונים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_grade);

        db = FirebaseFirestore.getInstance();
        className = getIntent().getStringExtra("CLASS_NAME"); // קבלת שם הכיתה מהמסך הקודם

        // הגדרת סרגל עליון עם כפתור חזור
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // קישור משתני הקוד לרכיבים הגרפיים שבמסך (XML)
        studentSpinner = findViewById(R.id.student_auto_complete);
        subjectSpinner = findViewById(R.id.grade_subject_auto_complete);
        newSubjectLayout = findViewById(R.id.new_subject_layout_grade);
        newSubjectEdit = findViewById(R.id.new_subject_edit_text_grade);
        scoreEdit = findViewById(R.id.score_edit_text);
        sendEmailSwitch = findViewById(R.id.send_email_switch);
        progressBar = findViewById(R.id.grade_progress);

        setupSpinners(); // הגדרת תיבות הבחירה
        loadStudents();  // טעינת רשימת התלמידים מהענן
        loadSubjects();  // טעינת רשימת המקצועות מהענן

        // הגדרת כפתור השמירה
        MaterialButton saveButton = findViewById(R.id.save_grade_button);
        saveButton.setOnClickListener(v -> saveGrade());
    }

    /**
     * הגדרת פעולות בעת בחירה מתוך רשימה.
     */
    private void setupSpinners() {
        subjectSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            // אם המשתמש בחר "הוסף מקצוע חדש", נציג לו תיבת טקסט להקלדה ידנית
            newSubjectLayout.setVisibility(ADD_NEW_SUBJECT.equals(selected) ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * טעינת כל התלמידים השייכים לכיתה הנוכחית מתוך בסיס הנתונים.
     */
    private void loadStudents() {
        db.collection("users")
                .whereEqualTo("className", className)
                .whereEqualTo("isAdmin", false) // טוענים רק תלמידים, לא מנהלים
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        studentList.clear();
                        List<String> studentNames = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Student s = doc.toObject(Student.class);
                            s.setUserId(doc.getId());
                            studentList.add(s);
                            studentNames.add(s.getFullName());
                        }
                        // עדכון רשימת הבחירה במסך
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentNames);
                        studentSpinner.setAdapter(adapter);
                    }
                });
    }

    /**
     * טעינת רשימת המקצועות. אנחנו לוקחים רשימה בסיסית ומוסיפים לה מקצועות שכבר הוזנו בעבר.
     */
    private void loadSubjects() {
        subjects.clear();
        subjects.addAll(Arrays.asList("מתמטיקה", "אנגלית", "לשון", "תנ\"ך", "היסטוריה", "פיזיקה", "ביולוגיה"));
        
        db.collection("grades")
                .whereEqualTo("className", className)
                .get()
                .addOnCompleteListener(task -> {
                    Set<String> uniqueSubjects = new HashSet<>(subjects);
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String sub = doc.getString("subject");
                            if (sub != null) uniqueSubjects.add(sub);
                        }
                    }
                    subjects.clear();
                    subjects.addAll(uniqueSubjects);
                    java.util.Collections.sort(subjects); // מיון לפי א"ב
                    subjects.add(ADD_NEW_SUBJECT);
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjects);
                    subjectSpinner.setAdapter(adapter);
                });
    }

    /**
     * פונקציה המבצעת את שמירת הציון בענן.
     */
    private void saveGrade() {
        String studentName = studentSpinner.getText().toString();
        String subject = subjectSpinner.getText().toString();
        String scoreStr = scoreEdit.getText().toString().trim();

        // בדיקה שכל השדות מולאו
        if (studentName.isEmpty() || subject.isEmpty() || scoreStr.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // טיפול במקרה של מקצוע חדש
        if (ADD_NEW_SUBJECT.equals(subject)) {
            subject = newSubjectEdit.getText().toString().trim();
            if (subject.isEmpty()) {
                Toast.makeText(this, "נא להזין מקצוע חדש", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // בדיקה שהציון הוא מספר תקין בין 0 ל-100
        int score;
        try {
            score = Integer.parseInt(scoreStr);
            if (score < 0 || score > 100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "ציון חייב להיות בין 0 ל-100", Toast.LENGTH_SHORT).show();
            return;
        }

        // מציאת התלמיד הנבחר מתוך הרשימה כדי לקבל את המזהה שלו (ID)
        Student selectedStudent = null;
        for (Student s : studentList) {
            if (s.getFullName().equals(studentName)) {
                selectedStudent = s;
                break;
            }
        }

        if (selectedStudent == null) return;

        final Student finalSelectedStudent = selectedStudent;
        final String finalSubject = subject;
        final int finalScore = score;
        final boolean shouldSendEmail = sendEmailSwitch.isChecked();

        progressBar.setVisibility(View.VISIBLE); // הצגת פס טעינה
        
        // יצירת אובייקט הציון לשמירה
        Grade grade = new Grade(
                null,
                selectedStudent.getUserId(),
                selectedStudent.getFullName(),
                subject,
                score,
                System.currentTimeMillis(),
                className
        );

        // שליחת הנתונים ל-Firebase
        db.collection("grades").add(grade)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "הציון נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                    
                    // 1. שליחת התראה פנימית לתלמיד (מופיע בתוך האפליקציה)
                    NotificationHelper.notifyStudentOfNewGrade(
                            finalSelectedStudent.getUserId(),
                            finalSubject,
                            finalScore
                    );
                    
                    // 2. שליחת מייל אמיתי לתלמיד אם המורה בחר בכך
                    if (shouldSendEmail) {
                        NotificationHelper.sendEmailToStudentBackground(
                                finalSelectedStudent.getEmail(),
                                finalSubject,
                                finalScore
                        );
                    }
                    
                    finish(); // סגירת המסך וחזרה אחורה
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "שגיאה בשמירת הציון", Toast.LENGTH_SHORT).show();
                });
    }
}
