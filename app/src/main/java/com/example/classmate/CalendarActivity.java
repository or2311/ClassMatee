package com.example.classmate;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * מסך לוח המבחנים (CalendarActivity).
 * מסך זה מציג לוח שנה המאפשר למשתמש לבחור יום ולראות את המבחנים או האירועים שנקבעו לאותו יום.
 * ניתן גם להוסיף אירועים חדשים, למחוק אותם או להוסיף אותם ליומן האישי של הטלפון.
 */
public class CalendarActivity extends AppCompatActivity {

    private ExamsAdapter adapter; // מתאם להצגת רשימת המבחנים מתחת ללוח השנה
    private final List<Event> eventList = new ArrayList<>(); // רשימת האירועים של היום הנבחר
    private FirebaseFirestore db; // חיבור לבסיס הנתונים בענן
    private String className = ""; // שם הכיתה של המשתמש
    private boolean isAdmin = false; // האם המשתמש הוא מנהל (מורה)
    private TextView noEventsText; // טקסט שמופיע כשאין אירועים ביום הנבחר
    private long selectedDateStart, selectedDateEnd; // משתנים לשמירת טווח הזמן של היום הנבחר

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        db = FirebaseFirestore.getInstance();
        className = getIntent().getStringExtra("CLASS_NAME");
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // הגדרת סרגל עליון עם כפתור חזרה
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // הגדרת הרשימה (RecyclerView) שתציג את המבחנים
        RecyclerView recyclerView = findViewById(R.id.exams_list_recycler_view);
        noEventsText = findViewById(R.id.no_events_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // אתחול המתאם עם הגדרת פעולות לכל מבחן (פרטים, מחיקה, הוספה ליומן)
        adapter = new ExamsAdapter(eventList, new ExamsAdapter.OnExamActionListener() {
            @Override
            public void onViewDetails(Event event) {
                showEventDetailsDialog(event); // הצגת חלונית פרטים
            }

            @Override
            public void onDeleteExam(Event event) {
                // בדיקת הרשאות מחיקה (רק מנהל או מי שיצר את האירוע)
                if (isAdmin || (FirebaseAuth.getInstance().getCurrentUser() != null && 
                    event.getCreatedBy().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))) {
                    deleteEvent(event);
                } else {
                    Toast.makeText(CalendarActivity.this, "אין לך הרשאה למחוק אירוע זה", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAddToCalendar(Event event) {
                addEventToDeviceCalendar(event); // הוספה ליומן של הטלפון (Google Calendar וכו')
            }
        });
        recyclerView.setAdapter(adapter);

        // הגדרת לוח השנה
        android.widget.CalendarView calendarView = findViewById(R.id.full_calendar_view);
        
        // קביעת התאריך הנוכחי כברירת מחדל
        updateSelectedDate(System.currentTimeMillis());
        
        // האזנה לשינוי תאריך בלוח השנה
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            updateSelectedDate(cal.getTimeInMillis());
            loadEventsForSelectedDate(); // טעינת האירועים של היום החדש שנבחר
        });

        // כפתור הוספת אירוע (Floating Action Button)
        FloatingActionButton fab = findViewById(R.id.fab_add_event);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            intent.putExtra("CLASS_NAME", className);
            intent.putExtra("IS_ADMIN", isAdmin);
            startActivity(intent);
        });

        loadEventsForSelectedDate(); // טעינה ראשונית של אירועי היום
    }

    /**
     * פונקציה שפותחת את אפליקציית היומן של הטלפון עם פרטי המבחן.
     */
    private void addEventToDeviceCalendar(Event event) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, event.getSubject() + " - " + event.getType())
                .putExtra(CalendarContract.Events.DESCRIPTION, event.getMaterial() + "\n" + event.getNotes())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getTimestamp())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getTimestamp() + (60 * 60 * 1000)) // נמשך שעה
                .putExtra(CalendarContract.Events.EVENT_LOCATION, "ClassMate App")
                .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
        
        startActivity(intent);
    }

    /**
     * מציג חלונית (Dialog) עם החומר למבחן והערות נוספות.
     */
    private void showEventDetailsDialog(Event event) {
        new AlertDialog.Builder(this)
                .setTitle(event.getSubject() + " - " + event.getType())
                .setMessage("חומר: " + (event.getMaterial().isEmpty() ? "לא צוין" : event.getMaterial()) + 
                           "\n\nהערות: " + (event.getNotes().isEmpty() ? "אין" : event.getNotes()))
                .setPositiveButton("סגור", null)
                .show();
    }

    /**
     * מוחק את האירוע מ-Firebase.
     */
    private void deleteEvent(Event event) {
        db.collection("events").document(event.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "האירוע נמחק", Toast.LENGTH_SHORT).show();
                    loadEventsForSelectedDate(); // רענון הרשימה
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
    }

    /**
     * מחשבת את תחילת וסוף היום שנבחר (במילישניות) כדי לסנן את האירועים.
     */
    private void updateSelectedDate(long timeInMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedDateStart = cal.getTimeInMillis();
        
        cal.add(Calendar.DAY_OF_MONTH, 1);
        selectedDateEnd = cal.getTimeInMillis();
    }

    /**
     * טוענת את כל המבחנים והאירועים מהענן ומסננת רק את אלו ששייכים ליום שנבחר.
     */
    private void loadEventsForSelectedDate() {
        if (className == null) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("events")
                .whereEqualTo("className", className)
                .get()
                .addOnSuccessListener(queryDocs -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : queryDocs) {
                        Long timestamp = doc.getLong("timestamp");
                        if (timestamp == null) continue;
                        
                        // בדיקה האם האירוע מתקיים ביום שנבחר
                        if (timestamp >= selectedDateStart && timestamp < selectedDateEnd) {
                            Event e = new Event(
                                    doc.getId(),
                                    doc.getString("subject"),
                                    doc.getString("type"),
                                    timestamp,
                                    doc.getString("material"),
                                    doc.getString("notes"),
                                    doc.getString("className"),
                                    doc.getString("createdBy"),
                                    Boolean.TRUE.equals(doc.getBoolean("isGlobal"))
                            );
                            
                            // בדיקת הרשאות צפייה: מציג אירועים כיתתיים (גלובליים) או אירועים אישיים של המשתמש
                            if (e.isGlobal() || (e.getCreatedBy() != null && e.getCreatedBy().equals(currentUid))) {
                                eventList.add(e);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged(); // עדכון הרשימה על המסך
                    // הצגת הודעה אם אין אירועים
                    noEventsText.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת אירועים", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsForSelectedDate(); // רענון הנתונים בכל פעם שחוזרים למסך
    }
}
