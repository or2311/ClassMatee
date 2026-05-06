package com.example.classmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך הסיכומים (SummariesActivity).
 * מסך זה מציג רשימה של כל הסיכומים הלימודיים שהועלו על ידי תלמידים או מורים בכיתה.
 * המשתמש יכול לצפות בסיכום (לחיצה על השורה תפתח את התמונה) ומנהלים יכולים למחוק סיכומים.
 */
public class SummariesActivity extends AppCompatActivity {

    private SummariesAdapter adapter; // המתאם שמחבר את נתוני הסיכומים לרשימה במסך
    private final List<Summary> summaryList = new ArrayList<>(); // רשימת הסיכומים שנטענו
    private FirebaseFirestore db; // חיבור לבסיס הנתונים לצורך שליפת פרטי הסיכומים
    private String className = ""; // שם הכיתה אליה שייכים הסיכומים
    private boolean isAdmin = false; // האם המשתמש הוא מנהל (לצורך הצגת כפתור מחיקה)
    private String email = ""; // כתובת המייל של המשתמש הנוכחי
    private TextView emptyStateText; // הודעה שמוצגת כשאין סיכומים בכיתה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summaries);

        // קבלת נתוני המשתמש מהמסך הקודם
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        email = getIntent().getStringExtra("EMAIL");
        className = getIntent().getStringExtra("CLASS_NAME");
        db = FirebaseFirestore.getInstance();

        // הגדרת סרגל הכלים העליון עם כותרת וכפתור חזור
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("סיכומים");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish()); // סגירת המסך בלחיצה על החץ

        emptyStateText = findViewById(R.id.empty_state_text);

        // הגדרת הרשימה (RecyclerView)
        RecyclerView recyclerView = findViewById(R.id.summaries_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SummariesAdapter(summaryList);
        recyclerView.setAdapter(adapter);

        // כפתור הוספת סיכום (Floating Action Button) - פותח את מסך ההעלאה
        FloatingActionButton fab = findViewById(R.id.fab_upload_summary);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadSummery.class);
            intent.putExtra("EMAIL", email);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("CLASS_NAME", className);
            startActivity(intent);
        });

        loadSummaries(); // טעינת הסיכומים מהענן
    }

    /**
     * פונקציה זו נקראת בכל פעם שהמשתמש חוזר למסך (למשל לאחר שהעלה סיכום חדש).
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadSummaries();
    }

    /**
     * שואבת את כל הסיכומים השייכים לכיתה הנוכחית מ-Firebase Firestore.
     */
    private void loadSummaries() {
        if (className == null) return;
        
        db.collection("summaries")
                .whereEqualTo("className", className) // סינון לפי שם הכיתה
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        summaryList.clear(); // ניקוי הרשימה לפני טעינה מחדש
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Long ts = doc.getLong("timestamp");
                            // יצירת אובייקט סיכום חדש מהנתונים שהגיעו מהענן
                            summaryList.add(new Summary(
                                    doc.getId(),
                                    doc.getString("title"),
                                    doc.getString("course"),
                                    doc.getString("imageUrl"),
                                    doc.getString("uploaderEmail"),
                                    doc.getString("className"),
                                    ts != null ? ts : 0L
                            ));
                        }
                        adapter.notifyDataSetChanged(); // רענון הרשימה על המסך
                        // אם הרשימה ריקה, נציג טקסט שאומר שאין סיכומים
                        emptyStateText.setVisibility(summaryList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(this, "שגיאה בטעינת הסיכומים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * מוחקת סיכום גם מבסיס הנתונים וגם מקובץ התמונה השמור בענן.
     */
    private void deleteSummary(Summary summary) {
        db.collection("summaries").document(summary.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // לאחר מחיקת הפרטים, ננסה למחוק גם את קובץ התמונה המקורי מהאחסון
                    String imageUrl = summary.getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        try {
                            FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl).delete();
                        } catch (Exception ignored) { /* תקלה במחיקת הקובץ לא תעצור את התהליך */ }
                    }
                    Toast.makeText(this, "הסיכום נמחק", Toast.LENGTH_SHORT).show();
                    loadSummaries(); // רענון הרשימה
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
    }

    /**
     * מתאם פנימי (Adapter) לניהול רשימת הסיכומים בתוך המסך.
     */
    private class SummariesAdapter extends RecyclerView.Adapter<SummariesAdapter.SummaryHolder> {
        private final List<Summary> items;

        SummariesAdapter(List<Summary> items) { this.items = items; }

        @Override
        public SummaryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // "מנפח" את קובץ ה-XML של שורת סיכום
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_summary, parent, false);
            return new SummaryHolder(v);
        }

        @Override
        public void onBindViewHolder(SummaryHolder holder, int position) {
            Summary s = items.get(position);
            holder.titleView.setText(s.getTitle());   // כותרת הסיכום
            holder.courseView.setText(s.getCourse()); // שם המקצוע

            // הצגת כפתור מחיקה רק אם המשתמש הוא מנהל
            if (holder.deleteButton != null) {
                holder.deleteButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                holder.deleteButton.setOnClickListener(v ->
                        new AlertDialog.Builder(SummariesActivity.this)
                                .setTitle("מחיקת סיכום")
                                .setMessage("למחוק את \"" + s.getTitle() + "\"?")
                                .setPositiveButton("מחק", (d, w) -> deleteSummary(s))
                                .setNegativeButton("ביטול", null)
                                .show()
                );
            }

            // פתיחת התמונה במסך מלא בעת לחיצה על השורה
            holder.itemView.setOnClickListener(v -> {
                String imageUrl = s.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Intent intent = new Intent(SummariesActivity.this, ViewImageActivity.class);
                    intent.putExtra("TITLE", s.getTitle());
                    intent.putExtra("IMAGE_URL", imageUrl);
                    startActivity(intent);
                } else {
                    Toast.makeText(SummariesActivity.this, "אין תמונה לסיכום זה", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        /**
         * מחזיק את רכיבי התצוגה של שורת סיכום בודדת.
         */
        class SummaryHolder extends RecyclerView.ViewHolder {
            TextView titleView, courseView;
            ImageButton deleteButton;

            SummaryHolder(View v) {
                super(v);
                titleView = v.findViewById(R.id.summary_title);
                courseView = v.findViewById(R.id.summary_course);
                deleteButton = v.findViewById(R.id.btn_delete_summary);
            }
        }
    }
}
