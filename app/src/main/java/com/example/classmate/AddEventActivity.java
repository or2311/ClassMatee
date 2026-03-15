package com.example.classmate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private TextInputEditText subjectEdit, typeEdit, materialEdit, notesEdit;
    private TextView selectedDateText;
    private ProgressBar progressBar;
    private final Calendar selectedCalendar = Calendar.getInstance();
    private boolean dateSelected = false;
    private String className = "";
    private static final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        className = getIntent().getStringExtra("CLASS_NAME");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("הוספת אירוע");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        subjectEdit = findViewById(R.id.event_subject_edit_text);
        typeEdit = findViewById(R.id.event_type_edit_text);
        materialEdit = findViewById(R.id.event_material_edit_text);
        notesEdit = findViewById(R.id.event_notes_edit_text);
        selectedDateText = findViewById(R.id.selected_date_text);
        progressBar = findViewById(R.id.event_progress);

        Button pickDateButton = findViewById(R.id.pick_date_button);
        pickDateButton.setOnClickListener(v -> showDatePicker());

        Button saveButton = findViewById(R.id.save_event_button);
        saveButton.setOnClickListener(v -> saveEvent());
    }

    private void showDatePicker() {
        int year = selectedCalendar.get(Calendar.YEAR);
        int month = selectedCalendar.get(Calendar.MONTH);
        int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, y, m, d) -> {
            selectedCalendar.set(Calendar.YEAR, y);
            selectedCalendar.set(Calendar.MONTH, m);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, d);
            showTimePicker();
        }, year, month, day).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            selectedCalendar.set(Calendar.HOUR_OF_DAY, hour);
            selectedCalendar.set(Calendar.MINUTE, minute);
            dateSelected = true;
            selectedDateText.setText(displayFormat.format(selectedCalendar.getTime()));
        }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show();
    }

    private void saveEvent() {
        String subject = getText(subjectEdit);
        String type = getText(typeEdit);

        if (subject.isEmpty()) {
            Toast.makeText(this, "נא להזין מקצוע", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!dateSelected) {
            Toast.makeText(this, "נא לבחור תאריך ושעה", Toast.LENGTH_SHORT).show();
            return;
        }
        if (type.isEmpty()) type = "מבחן";

        Event event = new Event(
                null,
                subject,
                type,
                selectedCalendar.getTimeInMillis(),
                getText(materialEdit),
                getText(notesEdit),
                className
        );

        progressBar.setVisibility(View.VISIBLE);
        EventManager.saveEvent(event, new EventManager.OnEventSavedListener() {
            @Override
            public void onSaved() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddEventActivity.this, "האירוע נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddEventActivity.this, "שגיאה בשמירה: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getText(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }
}
