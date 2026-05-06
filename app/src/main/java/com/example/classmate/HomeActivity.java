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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * מסך הבית המרכזי של האפליקציה.
 * מכאן המשתמש (תלמיד או מנהל) יכול לנווט לכל שאר חלקי האפליקציה.
 */
public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private boolean isAdmin = false;
    private String email = "";
    private String fullName = "";
    private String className = "";
    
    private StudentsAdapter studentsAdapter;
    private final List<Student> studentList = new ArrayList<>();
    private FirebaseFirestore db;
    
    private TextView examsCountHome, avgGradeHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            ThemeHelper.applyTheme(this, user.getUid());
        }
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // קבלת נתונים מהאינטנט
        email = getIntent().getStringExtra("EMAIL");
        fullName = getIntent().getStringExtra("FULL_NAME");
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        className = getIntent().getStringExtra("CLASS_NAME");

        // תיקון קריטי: אם שם הכיתה חסר (קורה לעיתים במנהל חדש), נשלוף אותו מהדאטהבייס
        if (className == null || className.isEmpty()) {
            fetchUserDataAndRefresh();
        } else {
            setupUI(navigationView);
            refreshStudentsData();
            loadHomeStats();
        }

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
    }

    /**
     * שליפת נתוני המשתמש מה-Firestore במידה והם לא הגיעו מהמסך הקודם.
     */
    private void fetchUserDataAndRefresh() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        className = document.getString("className");
                        fullName = document.getString("fullName");
                        isAdmin = Boolean.TRUE.equals(document.getBoolean("isAdmin"));
                        email = document.getString("email");

                        setupUI((NavigationView) findViewById(R.id.nav_view));
                        refreshStudentsData();
                        loadHomeStats();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת נתוני כיתה", Toast.LENGTH_SHORT).show());
    }

    private void setupUI(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderSubtitle = headerView.findViewById(R.id.nav_header_subtitle);
        navHeaderSubtitle.setText(fullName != null ? fullName : email);

        TextView welcomeTitle = findViewById(R.id.welcome_title);
        welcomeTitle.setText("שלום, " + (fullName != null ? fullName.split(" ")[0] : "אורח") + "!");

        examsCountHome = findViewById(R.id.exams_count_home);
        avgGradeHome = findViewById(R.id.avg_grade_home);

        Menu menu = navigationView.getMenu();
        menu.setGroupVisible(R.id.admin_menu_group, isAdmin);
        MenuItem myGradesItem = menu.findItem(R.id.nav_my_grades);
        if (myGradesItem != null) myGradesItem.setVisible(!isAdmin);

        RecyclerView studentsRecyclerView = findViewById(R.id.students_recycler_view);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        studentsAdapter = new StudentsAdapter(studentList, isAdmin, new StudentsAdapter.OnStudentActionListener() {
            @Override
            public void onEditStudent(Student student) { openActivity(StudentsActivity.class); }
            @Override
            public void onDeleteStudent(Student student) { openActivity(StudentsActivity.class); }
        });
        studentsRecyclerView.setAdapter(studentsAdapter);

        findViewById(R.id.exams_card).setOnClickListener(v -> openActivity(CalendarActivity.class));
        findViewById(R.id.students_card).setOnClickListener(v -> openActivity(StudentsActivity.class));
    }

    private void loadHomeStats() {
        if (className == null || className.isEmpty()) return;
        long currentTime = System.currentTimeMillis();
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        db.collection("events")
                .whereEqualTo("className", className)
                .whereGreaterThanOrEqualTo("timestamp", currentTime)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Boolean isGlobal = doc.getBoolean("isGlobal");
                            String createdBy = doc.getString("createdBy");
                            if (Boolean.TRUE.equals(isGlobal) || (createdBy != null && createdBy.equals(uid))) {
                                count++;
                            }
                        }
                    }
                    if (examsCountHome != null) examsCountHome.setText(String.valueOf(count));
                });

        db.collection("grades")
                .whereEqualTo(isAdmin ? "className" : "studentId", isAdmin ? className : uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        if (avgGradeHome != null) avgGradeHome.setText("--");
                        return;
                    }
                    double sum = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long score = doc.getLong("score");
                        if (score != null) sum += score;
                    }
                    double avg = sum / queryDocumentSnapshots.size();
                    if (avgGradeHome != null) avgGradeHome.setText(String.format(Locale.getDefault(), "%.1f", avg));
                });
    }

    private void openActivity(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.putExtra("IS_ADMIN", isAdmin);
        intent.putExtra("EMAIL", email);
        intent.putExtra("FULL_NAME", fullName);
        intent.putExtra("CLASS_NAME", className);
        startActivity(intent);
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
        } else if (id == R.id.nav_my_grades) {
            intent = new Intent(this, ViewGradesActivity.class);
            intent.putExtra("IS_ADMIN_VIEW", false);
        } else if (id == R.id.nav_enter_grades) {
            intent = new Intent(this, EnterGradeActivity.class);
        } else if (id == R.id.nav_class_grades) {
            intent = new Intent(this, ViewGradesActivity.class);
            intent.putExtra("IS_ADMIN_VIEW", true);
        } else if (id == R.id.nav_add_student) {
            drawerLayout.closeDrawer(GravityCompat.START);
            showAddStudentDialog();
            return true;
        } else if (id == R.id.nav_dark_mode) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String uid = user.getUid();
                boolean isCurrentlyDark = ThemeHelper.isDarkMode(this, uid);
                ThemeHelper.saveThemeMode(this, uid, !isCurrentlyDark);
                ThemeHelper.applyTheme(this, uid);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        if (intent != null) {
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("EMAIL", email);
            intent.putExtra("FULL_NAME", fullName);
            intent.putExtra("CLASS_NAME", className);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHomeStats();
        refreshStudentsData();
    }

    private void refreshStudentsData() {
        if (className == null || className.isEmpty()) return;
        db.collection("users")
                .whereEqualTo("className", className)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        studentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Boolean isAdminDoc = document.getBoolean("isAdmin");
                            if (isAdminDoc != null && isAdminDoc) continue;

                            studentList.add(new Student(
                                    document.getString("fullName"),
                                    document.getString("email"),
                                    document.getId(),
                                    document.getString("className"),
                                    false
                            ));
                        }
                        if (studentsAdapter != null) {
                            studentsAdapter.updateStudents(studentList);
                        }
                    }
                });
    }

    private void showAddStudentDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_student, null);
        TextInputEditText fullNameInput = dialogView.findViewById(R.id.student_fullname_input);
        TextInputEditText emailInput = dialogView.findViewById(R.id.student_username_input);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.student_password_input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("הוספת תלמיד חדש")
                .setView(dialogView)
                .create();

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "הוסף", (DialogInterface.OnClickListener) null);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "ביטול", (DialogInterface.OnClickListener) null);

        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String studentName = fullNameInput.getText() != null ? fullNameInput.getText().toString().trim() : "";
            String emailStr = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

            if (studentName.isEmpty() || emailStr.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
                return;
            }
            addStudentToClass(studentName, emailStr, password, dialog);
        }));

        dialog.show();
    }

    private void addStudentToClass(String fullName, String studentEmail, String password, AlertDialog dialog) {
        com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseApp.getInstance().getOptions();
        com.google.firebase.FirebaseApp secondaryApp;
        try {
            secondaryApp = com.google.firebase.FirebaseApp.getInstance("studentCreationHome");
        } catch (IllegalStateException e) {
            secondaryApp = com.google.firebase.FirebaseApp.initializeApp(this, options, "studentCreationHome");
        }

        final com.google.firebase.FirebaseApp finalSecondaryApp = secondaryApp;
        com.google.firebase.auth.FirebaseAuth secondaryAuth =
                com.google.firebase.auth.FirebaseAuth.getInstance(finalSecondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(studentEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("fullName", fullName);
                        userData.put("email", studentEmail);
                        userData.put("className", className);
                        userData.put("isAdmin", false);

                        db.collection("users").document(uid).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    secondaryAuth.signOut();
                                    dialog.dismiss();
                                    Toast.makeText(this, "התלמיד נוסף בהצלחה", Toast.LENGTH_SHORT).show();
                                    refreshStudentsData();
                                })
                                .addOnFailureListener(e -> {
                                    secondaryAuth.signOut();
                                    Toast.makeText(this, "שגיאה בשמירת פרטי התלמיד: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "שגיאה";
                        Toast.makeText(this, "שגיאה ביצירת חשבון: " + err, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
