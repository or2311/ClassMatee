package com.example.classmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private boolean isAdmin = false;
    private String email = "";
    private String className = "";
    private StudentsAdapter studentsAdapter;
    private List<Student> studentList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        email = getIntent().getStringExtra("EMAIL");
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        className = getIntent().getStringExtra("CLASS_NAME");

        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderSubtitle = headerView.findViewById(R.id.nav_header_subtitle);
        if (email != null) {
            navHeaderSubtitle.setText(email);
        }

        TextView welcomeTitle = findViewById(R.id.welcome_title);
        if (email != null) {
            welcomeTitle.setText(getString(R.string.welcome_user, email));
        }

        Menu menu = navigationView.getMenu();
        menu.setGroupVisible(R.id.admin_menu_group, isAdmin);

        RecyclerView studentsRecyclerView = findViewById(R.id.students_recycler_view);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        studentsAdapter = new StudentsAdapter(studentList, isAdmin, new StudentsAdapter.OnStudentActionListener() {
            @Override
            public void onEditStudent(Student student) {
                // Implement if needed or redirect to StudentsActivity
            }

            @Override
            public void onDeleteStudent(Student student) {
                // Implement if needed
            }
        });
        studentsRecyclerView.setAdapter(studentsAdapter);

        MaterialCardView examsCard = findViewById(R.id.exams_card);
        examsCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CalendarActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("EMAIL", email);
            intent.putExtra("CLASS_NAME", className);
            startActivity(intent);
        });
        
        MaterialCardView studentsCard = findViewById(R.id.students_card);
        studentsCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, StudentsActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("EMAIL", email);
            intent.putExtra("CLASS_NAME", className);
            startActivity(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });

        refreshStudentsData();
    }

    private void refreshStudentsData() {
        if (className == null) return;
        db.collection("users")
                .whereEqualTo("className", className)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        studentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            studentList.add(new Student(
                                    document.getString("fullName"),
                                    document.getString("email"),
                                    "",
                                    document.getString("className"),
                                    document.getBoolean("isAdmin") != null && document.getBoolean("isAdmin")
                            ));
                        }
                        Collections.sort(studentList, (s1, s2) -> Boolean.compare(s2.isAdmin(), s1.isAdmin()));
                        studentsAdapter.updateStudents(studentList);
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;
        if (id == R.id.nav_summaries) {
            intent = new Intent(this, SummariesActivity.class);
        } else if (id == R.id.nav_upload_summary) {
            intent = new Intent(this, UploadSummery.class);
        } else if (id == R.id.nav_exams) {
            intent = new Intent(this, CalendarActivity.class);
        } else if (id == R.id.nav_students) {
            intent = new Intent(this, StudentsActivity.class);
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        if (intent != null) {
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("EMAIL", email);
            intent.putExtra("CLASS_NAME", className);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
