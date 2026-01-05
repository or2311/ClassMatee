package com.example.classmate;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private boolean isAdmin = false;
    private StudentsAdapter studentsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        String username = getIntent().getStringExtra("USERNAME");
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        TextView welcomeTitle = findViewById(R.id.welcome_title);
        if (username != null && !username.isEmpty()) {
            welcomeTitle.setText(getString(R.string.welcome_user, username));
        } else {
            welcomeTitle.setText(getString(R.string.welcome_user, "אורח"));
        }

        Menu menu = navigationView.getMenu();
        MenuItem adminGroup = menu.findItem(R.id.admin_menu_group);
        if (adminGroup != null) {
            adminGroup.setVisible(isAdmin);
        }

        RecyclerView studentsRecyclerView = findViewById(R.id.students_recycler_view);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<Student> students = StudentManager.loadStudents(this);
        
        studentsAdapter = new StudentsAdapter(students, isAdmin, new StudentsAdapter.OnStudentActionListener() {
            @Override
            public void onEditStudent(Student student) {
                showEditStudentDialog(student);
            }

            @Override
            public void onDeleteStudent(Student student) {
                showDeleteStudentDialog(student);
            }
        });
        studentsRecyclerView.setAdapter(studentsAdapter);

        MaterialCardView examsCard = findViewById(R.id.exams_card);
        examsCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CalendarActivity.class);
            startActivity(intent);
        });
        
        MaterialCardView studentsCard = findViewById(R.id.students_card);
        studentsCard.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, StudentsActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            startActivity(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    if (isEnabled()) {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (studentsAdapter != null) {
            studentsAdapter.updateStudents(StudentManager.loadStudents(this));
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_summaries) {
            Toast.makeText(this, "מעבר לצפייה בסיכומים", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_upload_summary) {
            Intent intent = new Intent(this, UploadSummery.class);
            startActivity(intent);
        } else if (id == R.id.nav_exams) {
            Intent intent = new Intent(this, CalendarActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_students) {
            Intent intent = new Intent(this, StudentsActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            startActivity(intent);
        } else if (id == R.id.nav_add_student) {
            showAddStudentDialog();
        } else if (id == R.id.nav_logout) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showEditStudentDialog(final Student student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_student, null);
        builder.setView(dialogView);
        builder.setTitle("עריכת פרטי תלמיד");

        final TextInputEditText fullNameInput = dialogView.findViewById(R.id.student_fullname_input);
        final TextInputEditText usernameInput = dialogView.findViewById(R.id.student_username_input);
        final TextInputEditText passwordInput = dialogView.findViewById(R.id.student_password_input);

        fullNameInput.setText(student.getFullName());
        usernameInput.setText(student.getUsername());
        passwordInput.setText(student.getPassword());
        
        final String oldUsername = student.getUsername();

        builder.setPositiveButton("שמור שינויים", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fullName = fullNameInput.getText().toString();
                String username = usernameInput.getText().toString();
                String password = passwordInput.getText().toString();

                if (!fullName.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
                    Student updatedStudent = new Student(fullName, username, password);
                    StudentManager.updateStudent(HomeActivity.this, oldUsername, updatedStudent);
                    Toast.makeText(HomeActivity.this, "הפרטים עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                    
                    studentsAdapter.updateStudents(StudentManager.loadStudents(HomeActivity.this));
                } else {
                    Toast.makeText(HomeActivity.this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("ביטול", null);
        builder.create().show();
    }

    private void showDeleteStudentDialog(final Student student) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת תלמיד")
                .setMessage("האם אתה בטוח שברצונך למחוק את התלמיד " + student.getFullName() + "?")
                .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StudentManager.deleteStudent(HomeActivity.this, student.getUsername());
                        Toast.makeText(HomeActivity.this, "התלמיד נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                        studentsAdapter.updateStudents(StudentManager.loadStudents(HomeActivity.this));
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_student, null);
        builder.setView(dialogView);

        final TextInputEditText fullNameInput = dialogView.findViewById(R.id.student_fullname_input);
        final TextInputEditText usernameInput = dialogView.findViewById(R.id.student_username_input);
        final TextInputEditText passwordInput = dialogView.findViewById(R.id.student_password_input);

        builder.setPositiveButton("הוסף", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fullName = fullNameInput.getText().toString();
                String username = usernameInput.getText().toString();
                String password = passwordInput.getText().toString();

                if (!fullName.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
                    Student newStudent = new Student(fullName, username, password);
                    StudentManager.addStudent(HomeActivity.this, newStudent);
                    Toast.makeText(HomeActivity.this, "התלמיד נוסף בהצלחה", Toast.LENGTH_SHORT).show();
                    
                    studentsAdapter.updateStudents(StudentManager.loadStudents(HomeActivity.this));
                } else {
                    Toast.makeText(HomeActivity.this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("ביטול", null);
        builder.create().show();
    }
}
