package com.example.skillswap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class CreateSkillActivity extends AppCompatActivity {

    private static final String TAG = "CreateSkillActivity";
    private EditText etTitle;
    private EditText etCategory;
    private EditText etDescription;
    private EditText etLocation;
    private EditText etPhone;
    private Button btnSubmit;
    private ImageView imgPreview;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;
    private String selectedImageUriStr = "";

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
        etPhone = findViewById(R.id.etSkillPhone);
        btnSubmit = findViewById(R.id.btnSubmitSkill);
        Button btnAttachPhoto = findViewById(R.id.btnAttachPhoto);
        imgPreview = findViewById(R.id.imgSkillPreview);

        // Register System Visual Photo Picker (ActivityResultContracts.PickVisualMedia)
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}
                        selectedImageUriStr = uri.toString();
                        imgPreview.setImageURI(uri);
                        imgPreview.setVisibility(View.VISIBLE);
                        Toast.makeText(CreateSkillActivity.this, "Portfolio photo attached!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnAttachPhoto.setOnClickListener(v -> photoPickerLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));

        btnSubmit.setOnClickListener(v -> submitSkillPost());
    }

    private void submitSkillPost() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

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
        if (TextUtils.isEmpty(phone)) {
            phone = "01700000000";
        }

        btnSubmit.setEnabled(false);

        // Determine Author Name from logged in user email or intent extra
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null || userEmail.isEmpty()) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    userEmail = user.getEmail();
                }
            } catch (Exception ignored) {}
        }

        String authorName = "Student";
        if (userEmail != null && !userEmail.isEmpty()) {
            if (userEmail.contains("@")) {
                String handle = userEmail.split("@")[0];
                if (!handle.isEmpty()) {
                    authorName = handle.substring(0, 1).toUpperCase() + handle.substring(1);
                } else {
                    authorName = handle;
                }
            } else {
                authorName = userEmail;
            }
        }

        String id = UUID.randomUUID().toString().substring(0, 8);
        SkillPost newPost = new SkillPost(id, title, category, description, authorName, location, "Available", selectedImageUriStr, phone);

        // Upload to Firebase Storage if an image is selected
        if (!selectedImageUriStr.isEmpty()) {
            try {
                Uri fileUri = Uri.parse(selectedImageUriStr);
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("skill_photos/" + id + ".jpg");
                Toast.makeText(this, "Uploading photo to Cloud Storage...", Toast.LENGTH_SHORT).show();

                storageRef.putFile(fileUri)
                        .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            Log.d(TAG, "Photo uploaded to Cloud Storage successfully: " + downloadUri.toString());
                            newPost.setImageUrl(downloadUri.toString());
                            savePostToRepository(newPost);
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to retrieve download URL, using local URI", e);
                            savePostToRepository(newPost);
                        }))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firebase Storage upload failed, using local URI", e);
                            savePostToRepository(newPost);
                        });
            } catch (Exception e) {
                Log.e(TAG, "Firebase Storage Exception, using local URI", e);
                savePostToRepository(newPost);
            }
        } else {
            savePostToRepository(newPost);
        }
    }

    private void savePostToRepository(SkillPost newPost) {
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
