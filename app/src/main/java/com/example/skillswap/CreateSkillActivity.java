package com.example.skillswap;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.UUID;

public class CreateSkillActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etCategory;
    private EditText etDescription;
    private EditText etLocation;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_skill);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Post New Skill");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etTitle = findViewById(R.id.etSkillTitle);
        etCategory = findViewById(R.id.etSkillCategory);
        etDescription = findViewById(R.id.etSkillDescription);
        etLocation = findViewById(R.id.etSkillLocation);
        btnSubmit = findViewById(R.id.btnSubmitSkill);

        btnSubmit.setOnClickListener(v -> submitSkillPost());
    }

    private void submitSkillPost() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            return;
        }
        if (TextUtils.isEmpty(category)) {
            etCategory.setError("Category is required");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            return;
        }
        if (TextUtils.isEmpty(location)) {
            etLocation.setError("Location is required");
            return;
        }

        btnSubmit.setEnabled(false);

        // Determine Author Name from logged in Firebase user or fallback
        String authorName = "Student";
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                authorName = user.getEmail().split("@")[0];
            }
        } catch (Exception ignored) {}

        String id = UUID.randomUUID().toString().substring(0, 8);
        SkillPost newPost = new SkillPost(id, title, category, description, authorName, location, "Available");

        SkillRepository.getInstance().addSkill(newPost, new SkillRepository.SaveCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(CreateSkillActivity.this, "Skill post published successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(CreateSkillActivity.this, "Failed to publish skill post", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
