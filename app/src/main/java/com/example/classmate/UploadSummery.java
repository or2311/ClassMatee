package com.example.classmate;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UploadSummery extends AppCompatActivity {

    private TextInputEditText titleEdit, courseEdit;
    private ImageView previewImage;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String email = "";
    private String className = "";
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    previewImage.setVisibility(View.VISIBLE);
                    previewImage.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_summary);

        email = getIntent().getStringExtra("EMAIL");
        className = getIntent().getStringExtra("CLASS_NAME");
        db = FirebaseFirestore.getInstance();

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

        // שמירת התמונה לאחסון מקומי
        String savedPath = saveImageLocally(selectedImageUri, title);

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("course", course);
        data.put("filePath", savedPath != null ? savedPath : "");
        data.put("uploaderEmail", email != null ? email : "");
        data.put("className", className != null ? className : "");
        data.put("timestamp", System.currentTimeMillis());

        db.collection("summaries").add(data)
                .addOnSuccessListener(ref -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "הסיכום הועלה בהצלחה", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "שגיאה בהעלאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String saveImageLocally(Uri uri, String name) {
        try {
            File dir = new File(getFilesDir(), "summaries");
            if (!dir.exists()) dir.mkdirs();
            File outFile = new File(dir, name.replaceAll("[^a-zA-Z0-9א-ת_]", "_") + "_" + System.currentTimeMillis() + ".jpg");
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(in);
            in.close();
            FileOutputStream out = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            out.close();
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private String getText(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }
}
