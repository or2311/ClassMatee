package com.example.classmate;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

import java.io.File;

public class ViewImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        String title = getIntent().getStringExtra("TITLE");
        // תמיכה בשני מפתחות: URL (Firebase Storage) ו-FILE_PATH (מקומי - ישן)
        String imageUrl = getIntent().getStringExtra("IMAGE_URL");
        String filePath = getIntent().getStringExtra("FILE_PATH");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title != null ? title : "סיכום");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView imageView = findViewById(R.id.full_image_view);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // תמונה מ-Firebase Storage (URL)
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(imageView);
        } else if (filePath != null && !filePath.isEmpty()) {
            // תמונה מקומית (גיבוי לגרסאות ישנות)
            File file = new File(filePath);
            if (file.exists()) {
                Glide.with(this).load(file).into(imageView);
            } else {
                Toast.makeText(this, "התמונה לא נמצאה", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "לא סופקה תמונה", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
