package com.example.classmate;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentsActivity extends AppCompatActivity {

    private StudentsAdapter adapter;
    private List<Student> studentList = new ArrayList<>();
    private FirebaseFirestore db;
    private boolean isAdmin = false;
    private String className = "";

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

        db = FirebaseFirestore.getInstance();

        RecyclerView recyclerView = findViewById(R.id.students_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StudentsAdapter(studentList, isAdmin, new StudentsAdapter.OnStudentActionListener() {
            @Override
            public void onEditStudent(Student student) {
                // עריכת תלמיד – ניתן להרחיב בעתיד
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
                            studentList.add(new Student(
                                    doc.getString("fullName"),
                                    doc.getString("email"),
                                    doc.getId(),
                                    doc.getString("className"),
                                    doc.getBoolean("isAdmin") != null && doc.getBoolean("isAdmin")
                            ));
                        }
                        Collections.sort(studentList, (s1, s2) -> Boolean.compare(s2.isAdmin(), s1.isAdmin()));
                        adapter.updateStudents(studentList);
                    }
                });
    }

    private void showDeleteConfirmation(Student student) {
        if (!isAdmin) return;
        new AlertDialog.Builder(this)
                .setTitle("מחיקת תלמיד")
                .setMessage("האם למחוק את " + student.getFullName() + " מהכיתה?")
                .setPositiveButton("מחק", (dialog, which) -> deleteStudent(student))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteStudent(Student student) {
        db.collection("users").document(student.getUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "התלמיד הוסר בהצלחה", Toast.LENGTH_SHORT).show();
                    loadStudents();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
