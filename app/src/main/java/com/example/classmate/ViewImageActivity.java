package com.example.classmate;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

import java.io.File;

/**
 * מסך תצוגת תמונה (ViewImageActivity).
 * מסך זה אחראי להציג תמונת סיכום במסך מלא. 
 * הוא תומך בטעינת תמונות ישירות מהאינטרנט (Firebase Storage) או מקבצים מקומיים בטלפון.
 */
public class ViewImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        // קבלת הכותרת וכתובת התמונה מהמסך ששלח אותנו לכאן
        String title = getIntent().getStringExtra("TITLE");
        // המערכת תומכת בשני סוגי כתובות: קישור מהאינטרנט או נתיב לקובץ בטלפון
        String imageUrl = getIntent().getStringExtra("IMAGE_URL");
        String filePath = getIntent().getStringExtra("FILE_PATH");

        // הגדרת סרגל הכלים העליון (Toolbar)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // הצגת חץ חזור
            getSupportActionBar().setTitle(title != null ? title : "סיכום"); // הצגת שם הסיכום ככותרת
        }
        // פקודה לחזור למסך הקודם כשלוחצים על החץ
        toolbar.setNavigationOnClickListener(v -> finish());

        // קישור לרכיב שמציג את התמונה ב-XML
        ImageView imageView = findViewById(R.id.full_image_view);

        // בדיקה: מאיפה עלינו לטעון את התמונה?
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // מקרה 1: התמונה נמצאת בענן (Firebase Storage)
            // אנחנו משתמשים בספריית Glide שהיא "מומחית" בטעינת תמונות מהאינטרנט בצורה מהירה
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery) // תמונה זמנית שמופיעה בזמן שהתמונה נטענת
                    .error(android.R.drawable.ic_delete)           // תמונה שמופיעה אם יש שגיאה בטעינה
                    .into(imageView);
        } else if (filePath != null && !filePath.isEmpty()) {
            // מקרה 2: התמונה שמורה פיזית על הטלפון (תמיכה בגרסאות קודמות של האפליקציה)
            File file = new File(filePath);
            if (file.exists()) {
                Glide.with(this).load(file).into(imageView);
            } else {
                Toast.makeText(this, "התמונה לא נמצאה", Toast.LENGTH_SHORT).show();
                finish(); // סגירת המסך אם הקובץ לא קיים
            }
        } else {
            // אם לא קיבלנו שום כתובת, נציג הודעה ונסגור את המסך
            Toast.makeText(this, "לא סופקה תמונה", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
