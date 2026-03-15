package com.example.classmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SummariesActivity extends AppCompatActivity {

    private SummariesAdapter adapter;
    private final List<Summary> summaryList = new ArrayList<>();
    private FirebaseFirestore db;
    private String className = "";
    private boolean isAdmin = false;
    private String email = "";

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

        RecyclerView recyclerView = findViewById(R.id.summaries_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SummariesAdapter(summaryList);
        recyclerView.setAdapter(adapter);

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
                                    doc.getString("filePath"),
                                    doc.getString("uploaderEmail"),
                                    doc.getString("className"),
                                    ts != null ? ts : 0L
                            ));
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "שגיאה בטעינת הסיכומים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- Adapter מוגדר כמחלקה פנימית ---
    private class SummariesAdapter extends Adapter<SummariesAdapter.SummaryHolder> {
        private final List<Summary> items;

        SummariesAdapter(List<Summary> items) { this.items = items; }

        @Override
        public SummaryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_summary, parent, false);
            return new SummaryHolder(v);
        }

        @Override
        public void onBindViewHolder(SummaryHolder holder, int position) {
            Summary s = items.get(position);
            holder.titleView.setText(s.getTitle());
            holder.courseView.setText(s.getCourse());
            holder.itemView.setOnClickListener(v -> {
                if (!s.getFilePath().isEmpty()) {
                    Intent intent = new Intent(SummariesActivity.this, ViewImageActivity.class);
                    intent.putExtra("TITLE", s.getTitle());
                    intent.putExtra("FILE_PATH", s.getFilePath());
                    startActivity(intent);
                } else {
                    Toast.makeText(SummariesActivity.this, "קובץ לא זמין", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class SummaryHolder extends ViewHolder {
            TextView titleView, courseView;
            SummaryHolder(View v) {
                super(v);
                titleView = v.findViewById(R.id.summary_title);
                courseView = v.findViewById(R.id.summary_course);
            }
        }
    }
}
