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
        
        Menu menu = navigationView.getMenu();
        MenuItem adminGroup = menu.findItem(R.id.admin_menu_group);
        if (adminGroup != null) {
            adminGroup.setVisible(isAdmin);
        }

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
                // הסרתי את בדיקת האדמין כדי לאפשר מחיקה לכולם
                showDeleteConfirmationDialog(event);
            }
        });
        examsRecyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_event);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CalendarActivity.this, AddEventActivity.class);
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
        loadEvents();
    }

    private void loadEvents() {
        allEvents = EventManager.loadEvents(this);
        adapter.updateEvents(allEvents);
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
        EventManager.deleteEvent(this, event);
        Toast.makeText(this, "האירוע נמחק בהצלחה", Toast.LENGTH_SHORT).show();
        loadEvents();
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
            drawerLayout.closeDrawer(GravityCompat.START);
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

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_student, null);
        builder.setView(dialogView);
        builder.setTitle("הוספת תלמיד חדש");

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
                    StudentManager.addStudent(CalendarActivity.this, newStudent);
                    Toast.makeText(CalendarActivity.this, "התלמיד נוסף בהצלחה", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CalendarActivity.this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("ביטול", null);
        builder.create().show();
    }
}
