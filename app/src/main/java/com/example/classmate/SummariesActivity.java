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

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

public class SummariesActivity extends AppCompatActivity {

    private SummariesAdapter adapter;
    private final List<Summary> summaryList = new ArrayList<>();
    private FirebaseFirestore db;
    private String className = "";
    private boolean isAdmin = false;
    private String email = "";
    private TextView emptyStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summaries);

        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);
        email = getIntent().getStringExtra("EMAIL");
        className = getIntent().getStringExtra("CLASS_NAME");
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("סיכומים");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        emptyStateText = findViewById(R.id.empty_state_text);

        RecyclerView recyclerView = findViewById(R.id.summaries_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SummariesAdapter(summaryList);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_upload_summary);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadSummery.class);
            intent.putExtra("EMAIL", email);
            intent.putExtra("IS_ADMIN", isAdmin);
            intent.putExtra("CLASS_NAME", className);
            startActivity(intent);
        });

        loadSummaries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSummaries();
    }

    private void loadSummaries() {
        if (className == null) return;
        db.collection("summaries")
                .whereEqualTo("className", className)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        summaryList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Long ts = doc.getLong("timestamp");
                            summaryList.add(new Summary(
                                    doc.getId(),
                                    doc.getString("title"),
                                    doc.getString("course"),
                                    doc.getString("imageUrl"),   // שינוי: imageUrl במקום filePath
                                    doc.getString("uploaderEmail"),
                                    doc.getString("className"),
                                    ts != null ? ts : 0L
                            ));
                        }
                        adapter.notifyDataSetChanged();
                        emptyStateText.setVisibility(summaryList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(this, "שגיאה בטעינת הסיכומים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteSummary(Summary summary) {
        // מחיקה מ-Firestore
        db.collection("summaries").document(summary.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // מחיקה מ-Firebase Storage אם יש URL (לא בלוק כי לא קריטי)
                    String imageUrl = summary.getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        try {
                            FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl).delete();
                        } catch (Exception ignored) { /* URL format issue - ignore */ }
                    }
                    Toast.makeText(this, "הסיכום נמחק", Toast.LENGTH_SHORT).show();
                    loadSummaries();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
    }

    // ---- Adapter פנימי ----
    private class SummariesAdapter extends RecyclerView.Adapter<SummariesAdapter.SummaryHolder> {
        private final List<Summary> items;

        SummariesAdapter(List<Summary> items) { this.items = items; }

        @Override
        public SummaryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_summary, parent, false);
            return new SummaryHolder(v);
        }

        @Override
        public void onBindViewHolder(SummaryHolder holder, int position) {
            Summary s = items.get(position);
            holder.titleView.setText(s.getTitle());
            holder.courseView.setText(s.getCourse());

            // הצגת כפתור מחיקה למנהלים בלבד
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

            // פתיחת תמונה בלחיצה
            holder.itemView.setOnClickListener(v -> {
                String imageUrl = s.getFilePath(); // השדה נקרא filePath במודל אבל מכיל עכשיו את ה-URL
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
