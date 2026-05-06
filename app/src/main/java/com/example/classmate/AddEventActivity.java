package com.example.classmate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * מסך הוספת אירוע (AddEventActivity).
 * מסך זה מאפשר למנהלים (מורים) להוסיף מבחנים כיתתיים, ולתלמידים להוסיף תזכורות אישיות ללוח השנה.
 * הוא כולל בחירת מקצוע מתוך רשימה, בחירת סוג אירוע, קביעת תאריך ושעה, והזנת חומר לימודי.
 */
public class AddEventActivity extends AppCompatActivity {

    // --- רכיבי ממשק המשתמש (UI) ---
    private AutoCompleteTextView subjectSpinner; // תיבת בחירה למקצוע (מתמטיקה, אנגלית וכו')
    private AutoCompleteTextView typeSpinner;    // תיבת בחירה לסוג האירוע (מבחן, בוחן, מטלה)
    private TextInputLayout newSubjectLayout;     // האזור שמופיע רק אם המשתמש רוצה להקליד מקצוע חדש
    private TextInputEditText newSubjectEdit;    // השדה להקלדת שם מקצוע חדש
    private TextInputEditText materialEdit;      // שדה להזנת החומר למבחן
    private TextInputEditText notesEdit;         // שדה להערות נוספות
    private TextView selectedDateText;           // טקסט המציג את התאריך והשעה שנבחרו
    private ProgressBar progressBar;              // עיגול טעינה שמופיע בזמן השמירה בענן
    
    // --- משתני נתונים ולוגיקה ---
    private final Calendar selectedCalendar = Calendar.getInstance(); // אובייקט לניהול התאריך והשעה שנבחרו
    private boolean dateSelected = false;         // משתנה שעוזר לוודא שהמשתמש אכן בחר תאריך
    private String className = "";                // שם הכיתה אליה משויך האירוע
    private boolean isAdmin = false;              // האם המשתמש הוא מנהל (קובע אם כולם יראו את האירוע)
    
    // פורמט להצגת התאריך למשתמש (יום/חודש/שנה שעה:דקות)
    private static final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private List<String> subjects = new ArrayList<>(); // רשימת המקצועות שתוצג בתיבת הבחירה
    private final String ADD_NEW_SUBJECT = "הוסף מקצוע חדש..."; // אפשרות מיוחדת להוספה ידנית
    private final String[] defaultTypes = {"מבחן", "בוחן", "מטלה", "הגשה", "אחר"}; // סוגי אירועים קבועים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        // קבלת נתונים שנשלחו מהמסך הקודם
        className = getIntent().getStringExtra("CLASS_NAME");
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // הגדרת סרגל כלים עליון עם כפתור חזור
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish()); // סגירת המסך בלחיצה על החץ

        // קישור המשתנים לרכיבים הגרפיים שבקובץ ה-XML
        subjectSpinner = findViewById(R.id.subject_auto_complete);
        newSubjectLayout = findViewById(R.id.new_subject_layout);
        newSubjectEdit = findViewById(R.id.new_subject_edit_text);
        typeSpinner = findViewById(R.id.type_auto_complete);
        materialEdit = findViewById(R.id.event_material_edit_text);
        notesEdit = findViewById(R.id.event_notes_edit_text);
        selectedDateText = findViewById(R.id.selected_date_text);
        progressBar = findViewById(R.id.event_progress);

        setupSpinners();              // הגדרת תיבות הבחירה (Dropdowns)
        loadSubjectsFromFirestore();   // טעינת רשימת מקצועות קיימת מ-Firebase

        // הגדרת לחיצה על כפתור בחירת תאריך
        MaterialButton pickDateButton = findViewById(R.id.pick_date_button);
        pickDateButton.setOnClickListener(v -> showDatePicker());

        // הגדרת לחיצה על כפתור השמירה
        MaterialButton saveButton = findViewById(R.id.save_event_button);
        saveButton.setOnClickListener(v -> saveEvent());
    }

    /**
     * מגדירה את תיבות הבחירה ומאזינה לשינויים בהן.
     */
    private void setupSpinners() {
        // הגדרת רשימת סוגי האירועים
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, defaultTypes);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setText(defaultTypes[0], false); // קביעת "מבחן" כברירת מחדל

        // מאזין לבחירת מקצוע - אם נבחר "הוסף מקצוע חדש", נציג את תיבת ההקלדה הידנית
        subjectSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            newSubjectLayout.setVisibility(ADD_NEW_SUBJECT.equals(selected) ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * טוענת את רשימת המקצועות מהאינטרנט ומוסיפה אותם לרשימת הבחירה.
     */
    private void loadSubjectsFromFirestore() {
        subjects.clear();
        // רשימת מקצועות בסיסית שתמיד תופיע
        subjects.addAll(Arrays.asList("מתמטיקה", "אנגלית", "לשון", "תנ\"ך", "היסטוריה", "פיזיקה", "ביולוגיה"));
        
        // פנייה ל-Firebase כדי להביא מקצועות נוספים שכבר קיימים בכיתה הזו
        FirebaseFirestore.getInstance().collection("events")
                .whereEqualTo("className", className)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Set<String> uniqueSubjects = new HashSet<>(subjects);
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String sub = doc.getString("subject");
                            if (sub != null) uniqueSubjects.add(sub);
                        }
                        subjects.clear();
                        subjects.addAll(uniqueSubjects);
                        java.util.Collections.sort(subjects); // מיון לפי א-ב
                        subjects.add(ADD_NEW_SUBJECT); // הוספת האופציה הידנית בסוף
                        
                        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjects);
                        subjectSpinner.setAdapter(subjectAdapter);
                    }
                });
    }

    /**
     * פותחת חלונית בחירת תאריך (יום, חודש, שנה).
     */
    private void showDatePicker() {
        int year = selectedCalendar.get(Calendar.YEAR);
        int month = selectedCalendar.get(Calendar.MONTH);
        int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, y, m, d) -> {
            selectedCalendar.set(Calendar.YEAR, y);
            selectedCalendar.set(Calendar.MONTH, m);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, d);
            showTimePicker(); // מיד לאחר בחירת תאריך, נעבור לבחירת שעה
        }, year, month, day).show();
    }

    /**
     * פותחת חלונית בחירת שעה (שעות ודקות).
     */
    private void showTimePicker() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            selectedCalendar.set(Calendar.HOUR_OF_DAY, hour);
            selectedCalendar.set(Calendar.MINUTE, minute);
            dateSelected = true;
            // הצגת התאריך והשעה הנבחרים בתיבת הטקסט על המסך
            selectedDateText.setText(displayFormat.format(selectedCalendar.getTime()));
        }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show();
    }

    /**
     * פונקציה האוספת את כל הנתונים מהמסך ושומרת אותם כאירוע חדש בענן.
     */
    private void saveEvent() {
        String subject = subjectSpinner.getText().toString();
        // אם המשתמש הקליד מקצוע חדש, נקח אותו משדה הטקסט הידני
        if (ADD_NEW_SUBJECT.equals(subject)) {
            subject = newSubjectEdit.getText() != null ? newSubjectEdit.getText().toString().trim() : "";
        }
        
        String type = typeSpinner.getText().toString();
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // בדיקות תקינות אחרונות
        if (subject.isEmpty()) {
            Toast.makeText(this, "נא לבחור או להזין מקצוע", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!dateSelected) {
            Toast.makeText(this, "נא לבחור תאריך ושעה", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת אובייקט האירוע עם כל המידע
        Event event = new Event(
                null,
                subject,
                type,
                selectedCalendar.getTimeInMillis(), // המרה למילישניות עבור מסד הנתונים
                getText(materialEdit),
                getText(notesEdit),
                className,
                currentUid,
                isAdmin // אם מנהל יצר, זה יהיה אירוע "גלובלי" שכולם רואים
        );

        progressBar.setVisibility(View.VISIBLE); // הצגת טעינה
        
        // שימוש במחלקת העזר EventManager כדי לבצע את השמירה בפועל ב-Firebase
        EventManager.saveEvent(event, new EventManager.OnEventSavedListener() {
            @Override
            public void onSaved() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddEventActivity.this, "האירוע נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                finish(); // סגירת המסך וחזרה ללוח השנה
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddEventActivity.this, "שגיאה בשמירה: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * פונקציית עזר לקבלת טקסט נקי (ללא רווחים מיותרים) משדה קלט.
     */
    private String getText(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }
}
