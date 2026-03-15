package com.example.classmate;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentsActivity extends AppCompatActivity {

    private StudentsAdapter adapter;
    private List<Student> studentList = new ArrayList<>();
    private FirebaseFirestore db;
    private boolean isAdmin = false;
    private String className = "";
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);

        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        className = getIntent().getStringExtra("CLASS_NAME");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("תלמידי הכיתה");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        emptyText = findViewById(R.id.empty_students_text);
        db = FirebaseFirestore.getInstance();

        RecyclerView recyclerView = findViewById(R.id.students_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StudentsAdapter(studentList, isAdmin, new StudentsAdapter.OnStudentActionListener() {
            @Override
            public void onEditStudent(Student student) {
                Toast.makeText(StudentsActivity.this, "עריכה: " + student.getFullName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteStudent(Student student) {
                showDeleteConfirmation(student);
            }
        });
        recyclerView.setAdapter(adapter);

        loadStudents();
    }

    private void loadStudents() {
        if (className == null) return;

        db.collection("users")
                .whereEqualTo("className", className)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        studentList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            // הצגת תלמידים בלבד – מסנן מנהלים
                            Boolean isAdminDoc = doc.getBoolean("isAdmin");
                            if (isAdminDoc != null && isAdminDoc) continue;

                            studentList.add(new Student(
                                    doc.getString("fullName"),
                                    doc.getString("email"),
                                    doc.getId(),   // UID = document ID
                                    doc.getString("className"),
                                    false
                            ));
                        }
                        adapter.updateStudents(studentList);
                        if (emptyText != null) {
                            emptyText.setVisibility(studentList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    } else {
                        Toast.makeText(this, "שגיאה בטעינת התלמידים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDeleteConfirmation(Student student) {
        if (!isAdmin) return;
        new AlertDialog.Builder(this)
                .setTitle("מחיקת תלמיד")
                .setMessage("האם למחוק את " + student.getFullName() + "?\n\nהתלמיד לא יוכל יותר להתחבר לאפליקציה.")
                .setPositiveButton("מחק", (dialog, which) -> deleteStudent(student))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteStudent(final Student student) {
        String uid = student.getUserId();

        // שלב 1: מחיקה מ-Firestore (מונעת התחברות מיידית)
        db.collection("users").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // שלב 2: כתיבת רשומת "ממתין למחיקה" ב-Firebase
                    // בפעם הבאה שהתלמיד ינסה להתחבר, LoginActivity ימחק את חשבון ה-Auth שלו
                    db.collection("pending_deletions").document(uid)
                            .set(new java.util.HashMap<String, Object>() {{
                                put("email", student.getEmail());
                                put("deletedAt", System.currentTimeMillis());
                            }})
                            .addOnCompleteListener(t -> {
                                Toast.makeText(StudentsActivity.this,
                                        "התלמיד " + student.getFullName() + " נמחק בהצלחה",
                                        Toast.LENGTH_SHORT).show();
                                loadStudents();
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
