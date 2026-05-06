package com.example.classmate;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * מסך העלאת סיכומים (UploadSummery).
 * מסך זה מאפשר למשתמש לבחור תמונה מהגלריה של הטלפון, להוסיף לה כותרת ושם קורס,
 * ולהעלות אותה לענן (גם לקבצים וגם לבסיס הנתונים).
 */
public class UploadSummery extends AppCompatActivity {

    // רכיבי ממשק המשתמש
    private TextInputEditText titleEdit, courseEdit; // תיבות טקסט לכותרת וקורס
    private ImageView previewImage;                  // תצוגה מקדימה של התמונה שנבחרה
    private ProgressBar progressBar;                 // פס התקדמות להעלאה
    
    // חיבור לשירותי Firebase
    private FirebaseFirestore db;        // בסיס הנתונים (לשמירת פרטי הסיכום)
    private FirebaseStorage storage;    // אחסון הקבצים (לשמירת התמונה עצמה)
    
    private String email = "";          // המייל של המעלה
    private String className = "";      // הכיתה אליה משויך הסיכום
    private Uri selectedImageUri = null; // הכתובת הפנימית של התמונה בטלפון

    /**
     * כלי לבחירת תוכן מהטלפון.
     * במקרה זה, הוא פותח את הגלריה ומחזיר את התמונה שהמשתמש בחר.
     */
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri; // שומר את כתובת התמונה
                    previewImage.setVisibility(View.VISIBLE);
                    // שימוש בספריית Glide כדי להציג את התמונה בצורה יעילה
                    Glide.with(this).load(uri).into(previewImage);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_summary);

        // קבלת נתונים מהמסך הקודם
        email = getIntent().getStringExtra("EMAIL");
        className = getIntent().getStringExtra("CLASS_NAME");
        
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // הגדרת סרגל כלים עליון עם כפתור חזור
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("העלאת סיכום");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // קישור רכיבי העיצוב לקוד
        titleEdit = findViewById(R.id.summary_title_edit_text);
        courseEdit = findViewById(R.id.summary_course_edit_text);
        previewImage = findViewById(R.id.preview_image);
        progressBar = findViewById(R.id.upload_progress);

        // כפתור לבחירת תמונה מהגלריה
        Button pickImageButton = findViewById(R.id.pick_image_button);
        pickImageButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // כפתור לביצוע ההעלאה הסופית
        Button uploadButton = findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(v -> uploadSummary());
    }

    /**
     * פונקציה המנהלת את תהליך העלאת הסיכום.
     * היא קודם מעלה את הקובץ לאחסון (Storage) ואז שומרת את הפרטים בבסיס הנתונים.
     */
    private void uploadSummary() {
        String title = getText(titleEdit);
        String course = getText(courseEdit);

        // בדיקה שכל הפרטים הוזנו
        if (title.isEmpty() || course.isEmpty()) {
            Toast.makeText(this, "נא למלא כותרת ושם קורס", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "נא לבחור תמונה", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE); // הצגת טעינה
        findViewById(R.id.upload_button).setEnabled(false); // נטרול הכפתור למניעת לחיצות כפולות

        // יצירת שם ייחודי לקובץ בענן כדי שלא ידרסו קבצים אחרים
        String fileName = "summaries/" + className + "/" + UUID.randomUUID() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        // שלב 1: העלאת הקובץ ל-Firebase Storage
        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        // שלב 2: אם ההעלאה הצליחה, מקבלים קישור (URL) לתמונה
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            // שלב 3: שמירת כל הפרטים ב-Firestore (כולל הקישור לתמונה)
                            saveToFirestore(title, course, downloadUri.toString());
                        }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.upload_button).setEnabled(true);
                    Toast.makeText(this, "שגיאה בהעלאת התמונה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                })
                .addOnProgressListener(snapshot -> {
                    // עדכון פס ההתקדמות בזמן אמת
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressBar.setProgress((int) progress);
                });
    }

    /**
     * שמירת נתוני הסיכום בטבלת הנתונים (Firestore).
     */
    private void saveToFirestore(String title, String course, String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("course", course);
        data.put("imageUrl", imageUrl);
        data.put("uploaderEmail", email != null ? email : "");
        data.put("className", className != null ? className : "");
        data.put("timestamp", System.currentTimeMillis());

        db.collection("summaries").add(data)
                .addOnSuccessListener(ref -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "הסיכום הועלה בהצלחה!", Toast.LENGTH_SHORT).show();
                    finish(); // חזרה למסך הקודם
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.upload_button).setEnabled(true);
                    Toast.makeText(this, "שגיאה בשמירת הנתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * פונקציית עזר לקבלת טקסט נקי משדות קלט.
     */
    private String getText(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }
}
