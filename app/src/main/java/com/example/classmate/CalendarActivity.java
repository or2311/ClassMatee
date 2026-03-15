package com.example.classmate;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CalendarView;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView examsRecyclerView;
    private ExamsAdapter adapter;
    private List<Event> allEvents;
    private CalendarView calendarView;
    private DrawerLayout drawerLayout;
    private boolean isAdmin = false;
    private String email = "";
    private String className = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        email = getIntent().getStringExtra("EMAIL");
        className = getIntent().getStringExtra("CLASS_NAME");

        // עדכון שם המשתמש בתפריט הצד
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderSubtitle = headerView.findViewById(R.id.nav_header_subtitle);
        if (email != null && !email.isEmpty()) {
            navHeaderSubtitle.setText("ברוכים הבאים - " + email);
        }

        Menu menu = navigationView.getMenu();
        menu.setGroupVisible(R.id.admin_menu_group, isAdmin);

        calendarView = findViewById(R.id.full_calendar_view);
        examsRecyclerView = findViewById(R.id.exams_list_recycler_view);
        examsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        allEvents = new ArrayList<>();
        
        adapter = new ExamsAdapter(allEvents, new ExamsAdapter.OnExamActionListener() {
            @Override
            public void onViewDetails(Event event) {
                showEventDetailsDialog(event);
            }

            @Override
            public void onDeleteExam(Event event) {
                showDeleteConfirmationDialog(event);
            }
        });
        examsRecyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_event);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CalendarActivity.this, AddEventActivity.class);
                intent.putExtra("IS_ADMIN", isAdmin);
                intent.putExtra("EMAIL", email);
                intent.putExtra("CLASS_NAME", className);
                startActivity(intent);
            }
        });

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                filterEventsByDate(year, month, dayOfMonth);
            }
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        EventManager.loadEvents(className, new EventManager.OnEventsLoadedListener() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                allEvents = events;
                adapter.updateEvents(allEvents);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CalendarActivity.this, "שגיאה בטעינת אירועים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterEventsByDate(int year, int month, int dayOfMonth) {
        List<Event> filteredEvents = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        
        for (Event event : allEvents) {
            cal.setTimeInMillis(event.getTimestamp());
            if (cal.get(Calendar.YEAR) == year && 
                cal.get(Calendar.MONTH) == month && 
                cal.get(Calendar.DAY_OF_MONTH) == dayOfMonth) {
                filteredEvents.add(event);
            }
        }
        adapter.updateEvents(filteredEvents);
    }

    private void showEventDetailsDialog(Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("פרטי " + event.getType());

        StringBuilder message = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        
        message.append("מקצוע: ").append(event.getSubject()).append("\n");
        message.append("מועד: ").append(sdf.format(event.getTimestamp())).append("\n\n");
        
        if (event.getMaterial() != null && !event.getMaterial().isEmpty()) {
            message.append("חומר למבחן:\n").append(event.getMaterial()).append("\n\n");
        }
        
        if (event.getNotes() != null && !event.getNotes().isEmpty()) {
            message.append("הערות:\n").append(event.getNotes()).append("\n");
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton("סגור", null);
        builder.show();
    }

    private void showDeleteConfirmationDialog(final Event event) {
        if (!isAdmin) return;
        
        new AlertDialog.Builder(this)
                .setTitle("מחיקת אירוע")
                .setMessage("האם אתה בטוח שברצונך למחוק את האירוע?")
                .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteEvent(event);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteEvent(Event event) {
        EventManager.deleteEvent(this, event, className);
        Toast.makeText(this, "האירוע נמחק בהצלחה", Toast.LENGTH_SHORT).show();
        loadEvents();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_summaries) {
            Intent intent = new Intent(this, SummariesActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("EMAIL", email);
            intent.putExtra("CLASS_NAME", className);
            startActivity(intent);
        } else if (id == R.id.nav_upload_summary) {
            Intent intent = new Intent(this, UploadSummery.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("EMAIL", email);
            intent.putExtra("CLASS_NAME", className);
            startActivity(intent);
        } else if (id == R.id.nav_exams) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_students) {
            Intent intent = new Intent(this, StudentsActivity.class);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("EMAIL", email);
            intent.putExtra("CLASS_NAME", className);
            startActivity(intent);
        } else if (id == R.id.nav_add_student) {
            showAddStudentDialog();
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_student, null);
        builder.setView(dialogView);
        builder.setTitle("הוספת תלמיד חדש");

        final TextInputEditText fullNameInput = dialogView.findViewById(R.id.student_fullname_input);
        final TextInputEditText emailInput = dialogView.findViewById(R.id.student_username_input);
        final TextInputEditText passwordInput = dialogView.findViewById(R.id.student_password_input);

        AlertDialog dialog = builder.create();
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "הוסף", (DialogInterface.OnClickListener) null);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "ביטול", (DialogInterface.OnClickListener) null);
        dialog.setOnShowListener(dlg -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String fullName = fullNameInput.getText() != null ? fullNameInput.getText().toString().trim() : "";
                String emailStr = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
                String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

                if (fullName.isEmpty() || emailStr.isEmpty() || password.isEmpty()) {
                    Toast.makeText(CalendarActivity.this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(CalendarActivity.this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
                    return;
                }

                addStudentToClass(fullName, emailStr, password, dialog);
            });
        });
        dialog.show();
    }

    private void addStudentToClass(String fullName, String studentEmail, String password, AlertDialog dialog) {
        // יצירת משתמש חדש ב-Firebase Auth דרך instance משני (כדי לא לנתק את המנהל)
        com.google.firebase.FirebaseOptions options = com.google.firebase.FirebaseApp.getInstance().getOptions();
        com.google.firebase.FirebaseApp secondaryApp;
        try {
            secondaryApp = com.google.firebase.FirebaseApp.getInstance("studentCreation");
        } catch (IllegalStateException e) {
            secondaryApp = com.google.firebase.FirebaseApp.initializeApp(this, options, "studentCreation");
        }

        final com.google.firebase.FirebaseApp finalSecondaryApp = secondaryApp;
        com.google.firebase.auth.FirebaseAuth secondaryAuth = com.google.firebase.auth.FirebaseAuth.getInstance(finalSecondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(studentEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("fullName", fullName);
                        userData.put("email", studentEmail);
                        userData.put("className", className);
                        userData.put("isAdmin", false);

                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(uid).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    secondaryAuth.signOut();
                                    dialog.dismiss();
                                    Toast.makeText(CalendarActivity.this, "התלמיד נוסף בהצלחה", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    secondaryAuth.signOut();
                                    Toast.makeText(CalendarActivity.this, "שגיאה בשמירת פרטי התלמיד: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "שגיאה";
                        Toast.makeText(CalendarActivity.this, "שגיאה ביצירת חשבון: " + err, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
