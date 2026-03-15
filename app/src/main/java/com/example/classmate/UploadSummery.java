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

public class UploadSummery extends AppCompatActivity {

    private TextInputEditText titleEdit, courseEdit;
    private ImageView previewImage;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String email = "";
    private String className = "";
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    previewImage.setVisibility(View.VISIBLE);
                    Glide.with(this).load(uri).into(previewImage);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_summary);

        email = getIntent().getStringExtra("EMAIL");
        className = getIntent().getStringExtra("CLASS_NAME");
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("העלאת סיכום");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        titleEdit = findViewById(R.id.summary_title_edit_text);
        courseEdit = findViewById(R.id.summary_course_edit_text);
        previewImage = findViewById(R.id.preview_image);
        progressBar = findViewById(R.id.upload_progress);

        Button pickImageButton = findViewById(R.id.pick_image_button);
        pickImageButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        Button uploadButton = findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(v -> uploadSummary());
    }

    private void uploadSummary() {
        String title = getText(titleEdit);
        String course = getText(courseEdit);

        if (title.isEmpty() || course.isEmpty()) {
            Toast.makeText(this, "נא למלא כותרת ושם קורס", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "נא לבחור תמונה", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.upload_button).setEnabled(false);

        // העלאה ל-Firebase Storage
        String fileName = "summaries/" + className + "/" + UUID.randomUUID() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            saveToFirestore(title, course, downloadUri.toString());
                        }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.upload_button).setEnabled(true);
                    Toast.makeText(this, "שגיאה בהעלאת התמונה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressBar.setProgress((int) progress);
                });
    }

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
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.upload_button).setEnabled(true);
                    Toast.makeText(this, "שגיאה בשמירת הנתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getText(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }
}
